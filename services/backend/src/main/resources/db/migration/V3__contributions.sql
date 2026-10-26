CREATE TABLE contributions (
    id                     BIGSERIAL PRIMARY KEY,
    project_id             BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    investor_id            BIGINT NOT NULL REFERENCES investors(id),
    type                   VARCHAR(50) NOT NULL,
    amount                 NUMERIC(19,4) NOT NULL,
    entry_date             DATE NOT NULL,
    valuation_before_entry NUMERIC(19,4) NOT NULL
);
