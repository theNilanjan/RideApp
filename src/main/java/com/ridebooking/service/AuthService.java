package com.ridebooking.service;

import com.ridebooking.domain.DriverProfile;
import com.ridebooking.domain.Role;
import com.ridebooking.domain.User;
import com.ridebooking.dto.AuthDtos;
import com.ridebooking.exception.ApiException;
import com.ridebooking.repository.DriverProfileRepository;
import com.ridebooking.repository.UserRepository;
import com.ridebooking.security.JwtService;
import com.ridebooking.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final DriverProfileRepository drivers;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository users, DriverProfileRepository drivers, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.users = users;
        this.drivers = drivers;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthDtos.AuthResponse registerRider(AuthDtos.RegisterRequest request) {
        User user = createUser(request.name(), request.email(), request.phone(), request.password(), Role.ROLE_RIDER);
        return authResponse(user);
    }

    @Transactional
    public AuthDtos.AuthResponse registerDriver(AuthDtos.DriverRegisterRequest request) {
        if (drivers.existsByLicenseNumber(request.licenseNumber())) {
            throw new ApiException(HttpStatus.CONFLICT, "License number is already registered");
        }
        User user = createUser(request.name(), request.email(), request.phone(), request.password(), Role.ROLE_DRIVER);

        DriverProfile profile = new DriverProfile();
        profile.setUser(user);
        profile.setLicenseNumber(request.licenseNumber());
        profile.setVehicleNumber(request.vehicleNumber());
        profile.setVehicleModel(request.vehicleModel());
        drivers.save(profile);

        return authResponse(user);
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = users.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        return authResponse(user);
    }

    private User createUser(String name, String email, String phone, String password, Role role) {
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        }
        if (users.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "Phone is already registered");
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        return users.save(user);
    }

    private AuthDtos.AuthResponse authResponse(User user) {
        String token = jwtService.generate(new UserPrincipal(user));
        return new AuthDtos.AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
