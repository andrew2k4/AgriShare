CREATE TABLE assets (
    id                BIGSERIAL PRIMARY KEY,
    project_id        BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    owner_investor_id BIGINT NOT NULL REFERENCES investors(id),
    type              VARCHAR(50) NOT NULL,
    description       VARCHAR(1000),
    initial_value     NUMERIC(19,4) NOT NULL,
    current_value     NUMERIC(19,4),
    ownership_status  VARCHAR(50) NOT NULL
);

CREATE TABLE leases (
    id           BIGSERIAL PRIMARY KEY,
    asset_id     BIGINT NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    monthly_rent NUMERIC(19,4) NOT NULL,
    start_date   DATE NOT NULL,
    end_date     DATE,
    status       VARCHAR(50) NOT NULL
);
