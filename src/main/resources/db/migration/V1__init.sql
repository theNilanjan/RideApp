CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    phone VARCHAR(30) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE driver_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES app_users(id),
    license_number VARCHAR(80) NOT NULL UNIQUE,
    vehicle_number VARCHAR(40) NOT NULL,
    vehicle_model VARCHAR(120) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT FALSE,
    current_latitude DOUBLE PRECISION,
    current_longitude DOUBLE PRECISION,
    rating DOUBLE PRECISION NOT NULL DEFAULT 5.0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE rides (
    id UUID PRIMARY KEY,
    rider_id UUID NOT NULL REFERENCES app_users(id),
    driver_id UUID REFERENCES driver_profiles(id),
    pickup_latitude DOUBLE PRECISION NOT NULL,
    pickup_longitude DOUBLE PRECISION NOT NULL,
    dropoff_latitude DOUBLE PRECISION NOT NULL,
    dropoff_longitude DOUBLE PRECISION NOT NULL,
    status VARCHAR(40) NOT NULL,
    fare NUMERIC(10,2) NOT NULL,
    surge_multiplier DOUBLE PRECISION NOT NULL,
    otp VARCHAR(8) NOT NULL,
    cancellation_reason VARCHAR(255),
    requested_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    ride_id UUID NOT NULL UNIQUE REFERENCES rides(id),
    amount NUMERIC(10,2) NOT NULL,
    method VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    provider_reference VARCHAR(120),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE ratings (
    id UUID PRIMARY KEY,
    ride_id UUID NOT NULL REFERENCES rides(id),
    reviewer_id UUID NOT NULL REFERENCES app_users(id),
    reviewee_id UUID NOT NULL REFERENCES app_users(id),
    score INTEGER NOT NULL CHECK (score BETWEEN 1 AND 5),
    comment VARCHAR(500),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_rides_rider ON rides(rider_id);
CREATE INDEX idx_rides_driver ON rides(driver_id);
CREATE INDEX idx_rides_status ON rides(status);
