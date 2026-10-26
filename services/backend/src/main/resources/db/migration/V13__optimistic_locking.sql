-- Optimistic locking (review finding): prevents a concurrent validate/reject race from
-- silently overwriting the other decision on a profit distribution or withdrawal.
ALTER TABLE profit_distributions ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE withdrawals ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE leases ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE assets ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Belt-and-braces DB-level guarantee: an asset can never have two ACTIVE leases at once,
-- even if two concurrent requests both pass the application-level check.
CREATE UNIQUE INDEX uq_one_active_lease_per_asset
    ON leases (asset_id)
    WHERE status = 'ACTIVE';
