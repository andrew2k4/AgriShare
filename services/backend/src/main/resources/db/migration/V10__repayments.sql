CREATE TABLE repayments (
    id                BIGSERIAL PRIMARY KEY,
    contribution_id   BIGINT NOT NULL REFERENCES contributions(id) ON DELETE CASCADE,
    principal_amount  NUMERIC(19,4) NOT NULL,
    interest_amount   NUMERIC(19,4) NOT NULL,
    due_date          DATE NOT NULL,
    status            VARCHAR(50) NOT NULL,
    paid_at           TIMESTAMP
);
