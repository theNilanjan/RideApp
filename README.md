# Ride Booking System

Spring Boot backend for a simple ride booking workflow with JWT authentication, PostgreSQL persistence, Flyway migrations, and WebSocket ride updates.

## Features

- Rider and driver registration/login
- JWT-protected REST API
- Fare estimation
- Ride booking with nearest available driver assignment
- Driver location, availability, accept/reject, and ride status updates
- OTP verification before starting a ride
- Demo payment capture
- Post-ride ratings
- Admin stats endpoint
- STOMP WebSocket endpoint for ride updates

## Requirements

- Java 21
- Maven
- PostgreSQL
- Redis

## Configuration

Defaults are in `src/main/resources/application.yml`.

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/ridedb"
$env:SPRING_DATASOURCE_USERNAME="ride"
$env:SPRING_DATASOURCE_PASSWORD="ride"
$env:SPRING_REDIS_HOST="localhost"
$env:SPRING_REDIS_PORT="6379"
$env:APP_JWT_SECRET="change-me-change-me-change-me-change-me-change-me"
```

## Run

```powershell
mvn spring-boot:run
```

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

## Main Endpoints

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/drivers/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/rides/estimate`
- `POST /api/v1/rides`
- `GET /api/v1/rides/me`
- `PATCH /api/v1/rides/{rideId}/cancel`
- `GET /api/v1/drivers/me`
- `PATCH /api/v1/drivers/me/location`
- `GET /api/v1/drivers/me/rides`
- `POST /api/v1/drivers/rides/{rideId}/decision`
- `PATCH /api/v1/drivers/rides/{rideId}/status`
- `GET /api/v1/drivers/me/earnings`
- `POST /api/v1/rides/{rideId}/payments`
- `POST /api/v1/rides/{rideId}/ratings`
- `GET /api/v1/admin/stats`

Use the JWT from login as:

```text
Authorization: Bearer <token>
```

Ride updates are published to:

```text
/topic/rides/{rideId}


## Copyright

© 2026 Nilanjan Ghosh. All rights reserved.

This project and its source code are the intellectual property of Nilanjan Ghosh. Unauthorized copying, modification, distribution, or use of this software, in whole or in part, without prior written permission is prohibited.

For permissions or inquiries, please contact the author.
```
