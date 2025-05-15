CREATE SCHEMA motadata;

CREATE TABLE public.clients (
   id TEXT PRIMARY KEY NOT NULL,
   description TEXT
   )

INSERT INTO public.clients (id, description) VALUES ("motadata", "sample client");

CREATE TABLE motadata.device_catalog (                   -- (snmp, linux)
    id SERIAL PRIMARY KEY,
    type TEXT,
    metadata JSONB
);

CREATE TABLE motadata.metric_group (
    id SERIAL PRIMARY KEY,
    name TEXT
    metrics TEXT[]
);

CREATE TABLE motadata.credential_profile (
    id SERIAL PRIMARY KEY,
    name TEXT,
    device_type INT REFERENCES device_catalog(id),
    credentials JSONB                                    -- {"version": "v2c", "community":"public"}
);

CREATE TABLE motadata.discovery_profile (
    id SERIAL PRIMARY KEY,
    target_ips JSONB                                     -- {"ip":"localhost", "low_limit":"192.168.10.100", "high_limit":"192.168.10.500"}
    port INT,
    credentials_profile_id INT credential_profile(id),
)

// --status for each discovery -> table

CREATE TABLE motadata.provisioned_devices (
    id SERIAL PRIMARY KEY,

    ip VARCHAR(255) NOT NULL,
    port INT,

    discovery_profile_id INT discovery_profile(id), -- think abt removing
    credentials_profile_id INT credential_profile(id),

    hostname VARCHAR(255),
    os VARCHAR(100),
    device_type VARCHAR(50),

    status TEXT,                        -- PROVISIONED, NOT-PROVISIONED
    discovered_at TIMESTAMP DEFAULT now()
);

CREATE TABLE motadata.metric_configs (
    id SERIAL PRIMARY KEY,
    discovery_profile_id INT discovery_profile(id),
    metric_name VARCHAR(100),
    polling_interval_seconds INT DEFAULT 10,
    last_polled at TIMESTAMP,
    status TEXT                         -- ONGOING, STOPPED
);

CREATE TABLE motadata.metrics (
    id SERIAL PRIMARY KEY,
    device_id INT REFERENCES provisioned_devices(id),
    metric_id INT REFERENCES metric_configs(id),

    metric_name VARCHAR(100),
    metric_value DOUBLE PRECISION,

    collected_at TIMESTAMP DEFAULT now(),
);

CREATE INDEX metric_by_device on motadata.metrics (device_id, metric_id);


