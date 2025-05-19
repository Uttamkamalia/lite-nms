CREATE SCHEMA motadata;

CREATE TABLE public.clients (
   id TEXT PRIMARY KEY NOT NULL,
   description TEXT
   )

INSERT INTO public.clients (id, description) VALUES ("motadata", "sample client");

CREATE TABLE motadata.device_catalog (
    id SERIAL PRIMARY KEY,
    type TEXT NOT NULL,
    default_protocol TEXT NOT NULL,
    default_port INTEGER NOT NULL,
    metadata JSONB DEFAULT '{}'::jsonb
);

-- Insert some common device types
INSERT INTO motadata.device_catalog (type, default_protocol, default_port, metadata)
VALUES ('NETWORK_DEVICE', 'SNMP', 161, '{"description": "Generic network device"}'::jsonb);

INSERT INTO motadata.device_catalog (type, default_protocol, default_port, metadata)
VALUES ('LINUX', 'SSH', 22, '{"description": "Linux server or workstation"}'::jsonb);


CREATE TABLE motadata.metric_group (
    id SERIAL PRIMARY KEY,
    name TEXT
    metrics TEXT[]
);

CREATE TABLE motadata.credential_profile (
    id SERIAL PRIMARY KEY,
    name TEXT,
    device_type INT REFERENCES device_catalog(id),
    credentials JSONB
);

CREATE TABLE motadata.discovery_profile (
    id SERIAL PRIMARY KEY,
    target TEXT
    credentials_profile_id INT credential_profile(id),
    created_at TIMESTAMP DEFAULT now()
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


