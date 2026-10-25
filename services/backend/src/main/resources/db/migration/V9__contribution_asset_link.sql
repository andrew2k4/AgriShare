ALTER TABLE contributions ADD COLUMN asset_id BIGINT REFERENCES assets(id);
