CREATE TABLE investors (
    id         BIGSERIAL PRIMARY KEY,
    full_name  VARCHAR(255),
    contribution DOUBLE PRECISION NOT NULL DEFAULT 0
);

CREATE TABLE projects (
    id                  BIGSERIAL PRIMARY KEY,
    owner_uid           VARCHAR(255) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    project_type        VARCHAR(50)  NOT NULL,
    livestock_type      VARCHAR(50),
    -- ProjectResources (embedded)
    budget              DOUBLE PRECISION NOT NULL DEFAULT 0,
    land_size           DOUBLE PRECISION NOT NULL DEFAULT 0,
    existing_structures BOOLEAN NOT NULL DEFAULT FALSE,
    notes               VARCHAR(255),
    -- ProjectCapital (embedded)
    initial_capital     DOUBLE PRECISION,
    current_capital     DOUBLE PRECISION,
    total_investment    DOUBLE PRECISION,
    total_expenses      DOUBLE PRECISION,
    -- ProjectStatistics (embedded)
    total_production    DOUBLE PRECISION,
    total_revenue       DOUBLE PRECISION,
    total_profit        DOUBLE PRECISION,
    productivity_rate   DOUBLE PRECISION
);

CREATE TABLE production_days (
    id         BIGSERIAL PRIMARY KEY,
    project_id BIGINT REFERENCES projects(id) ON DELETE CASCADE,
    date       DATE,
    quantity   INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE operations (
    id             BIGSERIAL PRIMARY KEY,
    project_id     BIGINT REFERENCES projects(id) ON DELETE CASCADE,
    label          VARCHAR(255),
    date           DATE,
    amount         DOUBLE PRECISION NOT NULL DEFAULT 0,
    operation_type VARCHAR(50) NOT NULL
);

CREATE TABLE capital_distributions (
    id BIGSERIAL PRIMARY KEY
);

CREATE TABLE capital_shares (
    distribution_id BIGINT REFERENCES capital_distributions(id) ON DELETE CASCADE,
    investor_name   VARCHAR(255),
    percentage      DOUBLE PRECISION NOT NULL DEFAULT 0
);

CREATE TABLE stock_inventories (
    id BIGSERIAL PRIMARY KEY
);

CREATE TABLE stock_items (
    inventory_id BIGINT REFERENCES stock_inventories(id) ON DELETE CASCADE,
    name         VARCHAR(255),
    quantity     INTEGER NOT NULL DEFAULT 0,
    unit         VARCHAR(50)
);
