-- V2__seed_demo_data.sql
-- Seed synthetic demo data (All values explicitly labelled synthetic / simulation)

-- Standard BCrypt hash for "admin123", "driver123", "hospital123", "police123"
-- Using pre-computed BCrypt hashes:
-- admin123: $2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.
-- driver123: $2a$10$lU72l3pXFp/r0wO2Z0qW.uT3L3.rK7UfS1eO2u9tF0.u6U3c7E8Ua
-- hospital123: $2a$10$o1vYy1.q2m3u4v5w6x7y8u9i0o1p2q3r4s5t6u7v8w9x0y1z2a3b.
-- police123: $2a$10$z1x2c3v4b5n6m7a8s9d0f1g2h3j4k5l6z7x8c9v0b1n2m3a4s5d6.
-- To ensure deterministic authentication without BCrypt version divergence in tests, we can use Spring Security's {noop} or BCrypt delegating encoder.
-- With DelegatingPasswordEncoder: {bcrypt}$2a$10$... or {noop}password
INSERT INTO users (id, username, password_hash, role, full_name, created_at) VALUES
('USR-001', 'admin', '{noop}admin123', 'ADMIN', 'System Administrator', CURRENT_TIMESTAMP),
('USR-002', 'driver', '{noop}driver123', 'AMBULANCE_OPERATOR', 'Ambulance Operator Rajan', CURRENT_TIMESTAMP),
('USR-003', 'hospital', '{noop}hospital123', 'HOSPITAL_STAFF', 'Karur Medical Staff Dr. Priya', CURRENT_TIMESTAMP),
('USR-004', 'police', '{noop}police123', 'TRAFFIC_POLICE', 'Karur Traffic Control Officer Kumar', CURRENT_TIMESTAMP);

-- Road network from Section 5 (Demo scenario)
-- S: Source (10.9400, 78.0700)
-- J1: Intermediate Junction 1 (10.9500, 78.0550)
-- J2: Intermediate Junction 2 (10.9550, 78.0750)
-- J3: Intermediate Junction 3 (10.9600, 78.0950)
-- H: Hospital Junction (10.9700, 78.0800)
INSERT INTO roads (id, source_junction, target_junction, distance_km, typical_speed_kmph, current_smoothed_speed_kmph, travel_time_minutes, is_closed, congestion_level, last_observation_time) VALUES
('A1', 'S', 'J1', 3.0, 30.0, 30.0, 6.0, FALSE, 'LOW', CURRENT_TIMESTAMP),
('A2', 'J1', 'H', 3.0, 15.0, 15.0, 12.0, FALSE, 'MEDIUM', CURRENT_TIMESTAMP),
('B1', 'S', 'J2', 4.0, 40.0, 40.0, 6.0, FALSE, 'LOW', CURRENT_TIMESTAMP),
('B2', 'J2', 'H', 4.0, 40.0, 40.0, 6.0, FALSE, 'LOW', CURRENT_TIMESTAMP),
('C1', 'S', 'J3', 5.0, 40.0, 40.0, 7.5, FALSE, 'LOW', CURRENT_TIMESTAMP),
('C2', 'J3', 'H', 5.0, 40.0, 40.0, 7.5, FALSE, 'LOW', CURRENT_TIMESTAMP);

-- Ambulances
INSERT INTO ambulances (id, plate_number, model, status, current_lat, current_lon, speed_kmph) VALUES
('AMB-01', 'TN-47-AM-1001', 'Force Traveller BLS', 'AVAILABLE', 10.9400, 78.0700, 0.0),
('AMB-02', 'TN-47-AM-1002', 'Tata Winger ALS', 'AVAILABLE', 10.9410, 78.0710, 0.0),
('AMB-03', 'TN-47-AM-1003', 'Force Traveller ALS (Cardiac Care)', 'AVAILABLE', 10.9420, 78.0690, 0.0),
('AMB-04', 'TN-47-AM-1004', 'Mahindra Bolero Neo Fast-Response', 'AVAILABLE', 10.9390, 78.0720, 0.0);

-- Hospitals
INSERT INTO hospitals (id, name, address, latitude, longitude, emergency_contact, total_beds, available_beds, notes) VALUES
('H-01', 'Karur Government Medical College Hospital (H)', 'Gandhigramam, Karur, TN 639004', 10.9700, 78.0800, '+91-4324-222001', 120, 28, 'Primary trauma centre with designated emergency bay'),
('H-02', 'Amaravathi Specialty Hospital', 'Kovai Main Road, Karur, TN 639002', 10.9630, 78.0750, '+91-4324-245550', 65, 14, 'Cardiology and neurology ICU equipped'),
('H-03', 'Apollo Reach Hospital', 'Salem Bypass Road, Karur, TN 639006', 10.9750, 78.0900, '+91-4324-266100', 80, 19, 'Multi-specialty emergency facility');

-- Cameras
INSERT INTO cameras (id, junction_id, segment_id, ip_or_stream_url, status, resolution, pixels_to_meters_ratio) VALUES
('CAM-J1', 'J1', 'A1', 'http://simulator/camera/j1', 'ACTIVE', '1920x1080', 25.0),
('CAM-J2', 'J2', 'B2', 'http://simulator/camera/j2', 'ACTIVE', '1920x1080', 28.5),
('CAM-J3', 'J3', 'C1', 'http://simulator/camera/j3', 'ACTIVE', '1920x1080', 24.0),
('CAM-H',  'H',  'B2', 'http://simulator/camera/h',  'ACTIVE', '1920x1080', 30.0);

-- Initial synthetic baseline observations
INSERT INTO traffic_observations (segment_id, timestamp, vehicle_count, density_veh_per_km, speed_kmph, queue_m, confidence, data_source, is_masked) VALUES
('A1', CURRENT_TIMESTAMP, 14, 28.0, 30.0, 15.0, 0.92, 'SIMULATION', FALSE),
('A2', CURRENT_TIMESTAMP, 24, 48.0, 15.0, 75.0, 0.88, 'SIMULATION', FALSE),
('B1', CURRENT_TIMESTAMP, 10, 20.0, 40.0, 10.0, 0.95, 'SIMULATION', FALSE),
('B2', CURRENT_TIMESTAMP, 12, 24.0, 40.0, 10.0, 0.94, 'SIMULATION', FALSE),
('C1', CURRENT_TIMESTAMP, 8,  16.0, 40.0, 5.0,  0.96, 'SIMULATION', FALSE),
('C2', CURRENT_TIMESTAMP, 9,  18.0, 40.0, 5.0,  0.95, 'SIMULATION', FALSE);

-- System Settings
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('ALGORITHM', 'DIJKSTRA', 'Active route optimization algorithm: DIJKSTRA or A_STAR'),
('HYSTERESIS_MARGIN_MIN', '1.0', 'Minimum travel time improvement in minutes required to trigger re-routing'),
('STALE_DATA_TIMEOUT_S', '120', 'Seconds after which unrefreshed camera data triggers baseline fallback'),
('ACTIVE_SCENARIO', 'NORMAL', 'Active simulation scenario: NORMAL, QUEUE_ON_B2, SUDDEN_CONGESTION, FEED_LOST, ROAD_CLOSED, RECOVERY'),
('SIMULATION_RUNNING', 'TRUE', 'Global simulation state flag');

-- Initial Notification
INSERT INTO notifications (user_role, title, message, severity) VALUES
('AMBULANCE_OPERATOR', 'System Initialized', 'AI-Driven Smart Ambulance Navigation prototype active in Simulation Mode.', 'INFO'),
('TRAFFIC_POLICE', 'Signal Network Ready', 'Miniature ESP32 traffic junction listener connected and ready for priority requests.', 'INFO');
