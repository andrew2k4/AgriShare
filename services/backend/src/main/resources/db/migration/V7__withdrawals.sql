CREATE TABLE withdrawals (
    id               BIGSERIAL PRIMARY KEY,
    contribution_id  BIGINT NOT NULL REFERENCES contributions(id) ON DELETE CASCADE,
    project_id       BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    investor_id      BIGINT NOT NULL REFERENCES investors(id),
    requested_amount NUMERIC(19,4) NOT NULL,
    penalty_rate     NUMERIC(10,4),
    resulting_loss   NUMERIC(19,4),
    net_amount_paid  NUMERIC(19,4),
    status           VARCHAR(50) NOT NULL,
    block_reason     VARCHAR(1000),
    requested_by     BIGINT NOT NULL,
    requested_at     TIMESTAMP NOT NULL,
    validated_by     BIGINT,
    validated_at     TIMESTAMP
);
