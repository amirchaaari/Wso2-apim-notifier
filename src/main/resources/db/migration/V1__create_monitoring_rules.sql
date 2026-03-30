CREATE TYPE rule_type AS ENUM (
    'HIGH_LATENCY',
    'FAULTY_EVENT',
    'THRESHOLD',
    'DELETE_EVENT'
);

CREATE TABLE monitoring_rules (
                                  id          BIGSERIAL                   PRIMARY KEY,
                                  name        VARCHAR(255)                NOT NULL,
                                  type        rule_type                   NOT NULL,
                                  parameters  JSONB                       NOT NULL DEFAULT '{}',
                                  enabled     BOOLEAN                     NOT NULL DEFAULT TRUE,
                                  created_at  TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW(),
                                  updated_at  TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rules_enabled    ON monitoring_rules(enabled);
CREATE INDEX idx_rules_type       ON monitoring_rules(type);
CREATE INDEX idx_rules_parameters ON monitoring_rules USING GIN(parameters);