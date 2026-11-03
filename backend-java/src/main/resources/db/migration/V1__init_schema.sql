-- V1__init_schema.sql
-- Schema initialization for AI-Driven Smart Ambulance Navigation

CREATE TABLE users (
    id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    full_name VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ambulances (
    id VARCHAR(64) PRIMARY KEY,
    plate_number VARCHAR(32) NOT NULL UNIQUE,
    model VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL, -- AVAILABLE, ON_TRIP, MAINTENANCE
    current_lat DOUBLE PRECISION NOT NULL,
    current_lon DOUBLE PRECISION NOT NULL,
    speed_kmph DOUBLE PRECISION DEFAULT 0.0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE hospitals (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    emergency_contact VARCHAR(64),
    total_beds INT DEFAULT 50,
    available_beds INT DEFAULT 12,
    notes VARCHAR(255)
);

CREATE TABLE roads (
    id VARCHAR(32) PRIMARY KEY, -- e.g. A1, A2, B1, B2, C1, C2
    source_junction VARCHAR(32) NOT NULL,
    target_junction VARCHAR(32) NOT NULL,
    distance_km DOUBLE PRECISION NOT NULL,
    typical_speed_kmph DOUBLE PRECISION NOT NULL,
    current_smoothed_speed_kmph DOUBLE PRECISION NOT NULL,
    travel_time_minutes DOUBLE PRECISION NOT NULL,
    is_closed BOOLEAN DEFAULT FALSE,
    congestion_level VARCHAR(32) DEFAULT 'LOW',
    last_observation_time TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE traffic_observations (
    id BIGSERIAL PRIMARY KEY,
    segment_id VARCHAR(32) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    vehicle_count INT NOT NULL,
    density_veh_per_km DOUBLE PRECISION NOT NULL,
    speed_kmph DOUBLE PRECISION NOT NULL,
    queue_m DOUBLE PRECISION NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    data_source VARCHAR(32) NOT NULL, -- SIMULATION, CAMERA_LIVE, CAMERA_REPLAY, CSV
    is_masked BOOLEAN DEFAULT FALSE,
    mask_reason VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_obs_segment_time ON traffic_observations(segment_id, timestamp DESC);

CREATE TABLE cameras (
    id VARCHAR(64) PRIMARY KEY,
    junction_id VARCHAR(32) NOT NULL,
    segment_id VARCHAR(32) NOT NULL,
    ip_or_stream_url VARCHAR(255),
    status VARCHAR(32) DEFAULT 'ACTIVE', -- ACTIVE, OFFLINE, REPLAY
    resolution VARCHAR(32) DEFAULT '1920x1080',
    pixels_to_meters_ratio DOUBLE PRECISION DEFAULT 25.0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE emergency_trips (
    id VARCHAR(64) PRIMARY KEY,
    ambulance_id VARCHAR(64) NOT NULL REFERENCES ambulances(id),
    hospital_id VARCHAR(64) NOT NULL REFERENCES hospitals(id),
    origin VARCHAR(64) NOT NULL,
    destination VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL, -- CREATED, ACTIVE, PAUSED, COMPLETED, CANCELLED
    selected_route VARCHAR(32) NOT NULL, -- Route A, Route B, Route C
    initial_eta_minutes DOUBLE PRECISION NOT NULL,
    current_eta_minutes DOUBLE PRECISION NOT NULL,
    remaining_distance_km DOUBLE PRECISION NOT NULL,
    route_change_count INT DEFAULT 0,
    start_time TIMESTAMP WITH TIME ZONE,
    arrival_time TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE routes (
    id VARCHAR(64) PRIMARY KEY,
    trip_id VARCHAR(64),
    name VARCHAR(32) NOT NULL, -- Route A, Route B, Route C
    segments_json TEXT NOT NULL,
    total_distance_km DOUBLE PRECISION NOT NULL,
    estimated_time_minutes DOUBLE PRECISION NOT NULL,
    algorithm VARCHAR(32) DEFAULT 'DIJKSTRA',
    is_recommended BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE route_updates (
    id BIGSERIAL PRIMARY KEY,
    trip_id VARCHAR(64) NOT NULL REFERENCES emergency_trips(id),
    previous_route VARCHAR(32) NOT NULL,
    new_route VARCHAR(32) NOT NULL,
    previous_eta_minutes DOUBLE PRECISION NOT NULL,
    new_eta_minutes DOUBLE PRECISION NOT NULL,
    time_difference_minutes DOUBLE PRECISION NOT NULL,
    reason VARCHAR(255) NOT NULL,
    data_source VARCHAR(32) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE recommendations (
    id VARCHAR(64) PRIMARY KEY,
    trip_id VARCHAR(64) NOT NULL REFERENCES emergency_trips(id),
    recommended_route VARCHAR(32) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    eta_minutes DOUBLE PRECISION NOT NULL,
    status VARCHAR(32) DEFAULT 'PENDING', -- PENDING, ACCEPTED, REJECTED
    driver_decision VARCHAR(32) DEFAULT 'NONE',
    decision_timestamp TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE signal_events (
    id BIGSERIAL PRIMARY KEY,
    junction_id VARCHAR(32) NOT NULL,
    direction VARCHAR(32) NOT NULL,
    ambulance_id VARCHAR(64) NOT NULL,
    trip_id VARCHAR(64),
    state VARCHAR(32) NOT NULL, -- REQUESTED, YELLOW_CLEARANCE, ALL_RED, GREEN_PRIORITY, NORMAL_CYCLE, OVERRIDDEN
    acknowledged BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE vehicle_detection_summaries (
    id BIGSERIAL PRIMARY KEY,
    camera_id VARCHAR(64) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    total_vehicles INT NOT NULL,
    car_count INT DEFAULT 0,
    bus_count INT DEFAULT 0,
    truck_count INT DEFAULT 0,
    motorcycle_count INT DEFAULT 0,
    avg_speed_kmph DOUBLE PRECISION DEFAULT 0.0,
    queue_length_m DOUBLE PRECISION DEFAULT 0.0,
    confidence DOUBLE PRECISION DEFAULT 0.85
);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_role VARCHAR(32) NOT NULL,
    title VARCHAR(128) NOT NULL,
    message VARCHAR(512) NOT NULL,
    severity VARCHAR(32) DEFAULT 'INFO',
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN DEFAULT FALSE
);

CREATE TABLE dataset_uploads (
    id VARCHAR(64) PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    row_count INT NOT NULL,
    column_mapping_json TEXT,
    status VARCHAR(32) DEFAULT 'PROCESSED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE system_settings (
    setting_key VARCHAR(64) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
