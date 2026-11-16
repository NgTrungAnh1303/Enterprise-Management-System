
ALTER TABLE app_user DROP CONSTRAINT IF EXISTS chk_app_user_status;
ALTER TABLE app_user ADD CONSTRAINT chk_app_user_status 
    CHECK (status IN ('ACTIVE', 'LOCKED', 'PENDING_APPROVAL', 'REJECTED'));

ALTER TABLE app_user ADD COLUMN IF NOT EXISTS email VARCHAR(100);

CREATE UNIQUE INDEX IF NOT EXISTS uq_app_user_email_lower ON app_user (lower(email)) WHERE email IS NOT NULL;

ALTER TABLE app_user ALTER COLUMN username TYPE VARCHAR(100);
