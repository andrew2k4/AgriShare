ALTER TABLE projects ADD COLUMN reference_duration_days INTEGER;
ALTER TABLE projects ADD COLUMN current_phase VARCHAR(50);

ALTER TABLE contributions ADD COLUMN phase_at_entry VARCHAR(50);
