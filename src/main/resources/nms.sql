CREATE SCHEMA motadata;

CREATE TABLE public.clients (
   id TEXT PRIMARY KEY NOT NULL,
   description TEXT
   )

INSERT INTO public.clients (id, description) VALUES ("motadata", "sample client");

CREATE TABLE motadata.device_catalog (
    id SERIAL PRIMARY KEY,
    type TEXT NOT NULL, --LINUX, NETWORK_DEVICE
    default_protocol TEXT NOT NULL, -- SSH, SNMP
    default_port INTEGER NOT NULL, -- 22, 161
    metadata JSONB DEFAULT '{}'::jsonb
);

-- Insert some common device types
INSERT INTO motadata.device_catalog (type, default_protocol, default_port, metadata)
VALUES ('NETWORK_DEVICE', 'SNMP', 161, '{"description": "Generic network device"}'::jsonb);

INSERT INTO motadata.device_catalog (type, default_protocol, default_port, metadata)
VALUES ('LINUX', 'SSH', 22, '{"description": "Linux server or workstation"}'::jsonb);

CREATE TABLE motadata.credential_profile (
    id SERIAL PRIMARY KEY,
    name TEXT,
    device_type INT REFERENCES device_catalog(id),
    credentials JSONB
);

CREATE TABLE motadata.discovery_profile (
    id SERIAL PRIMARY KEY,
    target TEXT,
    credentials_profile_id INT REFERENCES motadata.credential_profile(id), // todo for multiple cred profiles
    discovery_info JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE motadata.provisioned_devices (
    id SERIAL PRIMARY KEY,

    ip VARCHAR(255) NOT NULL,
    port INT,
    protocol TEXT,

    discovery_profile_id INT NOT NULL REFERENCES discovery_profile(id), -- think abt removing
    credentials_profile_id INT NOT NULL REFERENCES credential_profile(id),

    device_type_id INT NOT NULL REFERENCES device_catalog(id),
    metadata JSONB DEFAULT '{}'::jsonb,

    status TEXT,                        -- PROVISIONED, NOT-PROVISIONED
    discovered_at TIMESTAMP DEFAULT now()
);

CREATE TABLE motadata.metric (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    metric_type TEXT NOT NULL, -- COUNTER, GAUGE, etc.
    metric_unit TEXT NOT NULL, -- bytes, seconds, etc.
    device_type_id INT NOT NULL REFERENCES device_catalog(id),
    protocol TEXT NOT NULL,
    plugin_id TEXT NOT NULL,
);

CREATE TABLE motadata.metric_group (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    device_type_id INT NOT NULL REFERENCES device_catalog(id),
    polling_interval_seconds INT DEFAULT 10,
    last_polled_at TIMESTAMP,
    status TEXT,                        -- ONGOING, STOPPED
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE motadata.metric_group_metrics (
    metric_group_id INT NOT NULL REFERENCES motadata.metric_group(id),
    metric_id INT NOT NULL REFERENCES motadata.metric(id),
    PRIMARY KEY (metric_group_id, metric_id)
);

CREATE TABLE motadata.metrics_record (
    id SERIAL PRIMARY KEY,
    device_id INT REFERENCES provisioned_devices(id),
    metric_id INT REFERENCES metric_configs(id),
    collected_at TIMESTAMP DEFAULT now(),

    metric_value TEXT,
    unit VARCHAR(64),
    metadata JSONB DEFAULT '{}'::jsonb,
);

CREATE INDEX metric_by_device on motadata.metrics (device_id, metric_id);


