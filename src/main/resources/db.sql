CREATE TABLE discovered_devices (
    id SERIAL PRIMARY KEY,
    ip_address VARCHAR(255) NOT NULL,
    hostname VARCHAR(255),
    os VARCHAR(255),
    device_type VARCHAR(50),
    discovery_id INT,
    discovered_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE device_metrics (
    id SERIAL PRIMARY KEY,
    device_id INT,
    metric_name VARCHAR(255),
    metric_value DOUBLE PRECISION,
    collected_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
