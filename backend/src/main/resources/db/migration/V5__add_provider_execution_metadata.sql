ALTER TABLE generation_record ADD COLUMN requested_provider VARCHAR(64);
ALTER TABLE generation_record ADD COLUMN requested_model VARCHAR(128);
ALTER TABLE generation_record ADD COLUMN fallback_used BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE generation_record ADD COLUMN fallback_reason VARCHAR(64);

UPDATE generation_record
SET requested_provider = provider_name,
    requested_model = model_name
WHERE requested_provider IS NULL
  AND requested_model IS NULL;
