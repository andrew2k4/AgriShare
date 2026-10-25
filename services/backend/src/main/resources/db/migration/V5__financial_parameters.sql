CREATE TABLE financial_parameter_configs (
    id                BIGSERIAL PRIMARY KEY,
    param_key         VARCHAR(100) NOT NULL,
    project_id        BIGINT REFERENCES projects(id) ON DELETE CASCADE,
    coefficient_value NUMERIC(10,4) NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    updated_by        BIGINT
);

CREATE UNIQUE INDEX uq_financial_parameter_key_project
    ON financial_parameter_configs (param_key, COALESCE(project_id, -1));

CREATE TABLE financial_parameter_history (
    id         BIGSERIAL PRIMARY KEY,
    param_key  VARCHAR(100) NOT NULL,
    project_id BIGINT,
    old_value  NUMERIC(10,4),
    new_value  NUMERIC(10,4) NOT NULL,
    changed_by BIGINT,
    changed_at TIMESTAMP NOT NULL
);
