CREATE TABLE profit_distributions (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    period_start        DATE NOT NULL,
    period_end          DATE NOT NULL,
    revenue             NUMERIC(19,4) NOT NULL,
    operating_costs     NUMERIC(19,4) NOT NULL,
    taxes               NUMERIC(19,4) NOT NULL,
    reserve             NUMERIC(19,4) NOT NULL,
    reinvestment        NUMERIC(19,4) NOT NULL,
    distributable_profit NUMERIC(19,4) NOT NULL,
    status              VARCHAR(50) NOT NULL,
    proposed_by         BIGINT NOT NULL,
    proposed_at         TIMESTAMP NOT NULL,
    validated_by        BIGINT,
    validated_at        TIMESTAMP
);

CREATE TABLE profit_distribution_lines (
    distribution_id       BIGINT REFERENCES profit_distributions(id) ON DELETE CASCADE,
    contribution_id       BIGINT NOT NULL,
    investor_id           BIGINT NOT NULL,
    units_of_contribution NUMERIC(19,6) NOT NULL,
    share_percentage      NUMERIC(19,2) NOT NULL,
    amount                NUMERIC(19,4) NOT NULL
);
