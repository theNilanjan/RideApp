package com.ridebooking.dto;

import com.ridebooking.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Pattern(regexp = "^[0-9+ -]{8,30}$") String phone,
            @NotBlank @Size(min = 8, max = 80) String password
    ) {}

    public record DriverRegisterRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Pattern(regexp = "^[0-9+ -]{8,30}$") String phone,
            @NotBlank @Size(min = 8, max = 80) String password,
            @NotBlank @Size(max = 80) String licenseNumber,
            @NotBlank @Size(max = 40) String vehicleNumber,
            @NotBlank @Size(max = 120) String vehicleModel
    ) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record AuthResponse(String token, UUID userId, String name, String email, Role role) {}
}
