-- =====================================================================
-- V8 — Platform feature completion.
-- Adds: admin/super-admin permissions, API request logging, IP blocks,
-- offline trainings, video library, password-reset & email-verification
-- tokens. Idempotent (ON CONFLICT DO NOTHING / IF NOT EXISTS).
-- =====================================================================

-- ---------------------------------------------------------------------
-- New permissions (admin dashboard + super-admin consoles)
-- ---------------------------------------------------------------------
INSERT INTO permissions (id, code, resource, action, description) VALUES
    (gen_random_uuid(), 'analytics:read',  'analytics',   'read',   'Read platform analytics overview'),
    (gen_random_uuid(), 'apilog:read',     'apilog',      'read',   'Read API request logs (super admin)'),
    (gen_random_uuid(), 'security:manage', 'security',    'manage', 'Manage security: events, IP blocks, account unlock'),
    (gen_random_uuid(), 'system:read',     'system',      'read',   'Read system health / monitoring'),
    (gen_random_uuid(), 'video:manage',    'video',       'manage', 'Manage own video library (tutor)'),
    (gen_random_uuid(), 'training:manage', 'training',    'manage', 'Manage own offline trainings (tutor)')
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------
-- Grant the new permissions to the appropriate roles.
-- (SUPER_ADMIN must be granted explicitly here — the V2 "all permissions"
--  rule only covered the permissions that existed at V2 time.)
-- ---------------------------------------------------------------------
WITH r AS (SELECT id, code FROM roles),
     p AS (SELECT id, code FROM permissions)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM r, p
WHERE
    (r.code = 'TUTOR' AND p.code IN ('video:manage','training:manage'))
    OR
    (r.code = 'ADMIN' AND p.code IN (
        'analytics:read','system:read','video:manage','training:manage'
    ))
    OR
    (r.code = 'SUPER_ADMIN' AND p.code IN (
        'analytics:read','apilog:read','security:manage','system:read',
        'video:manage','training:manage'
    ))
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- API request logs — captured by a servlet filter for admin observability.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS api_request_logs (
    id            UUID PRIMARY KEY,
    actor_id      UUID REFERENCES users(id) ON DELETE SET NULL,
    method        VARCHAR(10)  NOT NULL,
    path          VARCHAR(512) NOT NULL,
    query_string  VARCHAR(1024),
    status_code   INTEGER      NOT NULL,
    duration_ms   INTEGER      NOT NULL,
    ip_address    INET,
    user_agent    VARCHAR(255),
    request_id    UUID,
    occurred_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_api_logs_occurred ON api_request_logs(occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_api_logs_actor    ON api_request_logs(actor_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_api_logs_status   ON api_request_logs(status_code, occurred_at DESC);

-- ---------------------------------------------------------------------
-- IP blocks — enforced by the same filter; super-admin managed.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ip_blocks (
    id          UUID PRIMARY KEY,
    ip_address  INET NOT NULL UNIQUE,
    reason      VARCHAR(255),
    created_by  UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_ip_blocks_ip ON ip_blocks(ip_address);

-- ---------------------------------------------------------------------
-- Offline trainings — short-form tutor-led offline programmes.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS trainings (
    id            UUID PRIMARY KEY,
    tutor_id      UUID NOT NULL REFERENCES tutor_profiles(id) ON DELETE CASCADE,
    title         VARCHAR(160) NOT NULL,
    description   TEXT,
    city          VARCHAR(80),
    address_line  VARCHAR(255),
    start_date    DATE,
    end_date      DATE,
    capacity      INTEGER NOT NULL DEFAULT 0,
    enrolled_count INTEGER NOT NULL DEFAULT 0,
    price         NUMERIC(12,2) NOT NULL DEFAULT 0,
    currency      CHAR(3) NOT NULL DEFAULT 'USD',
    status        VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    version       BIGINT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ,
    created_by    UUID,
    updated_by    UUID,
    CONSTRAINT chk_training_status CHECK (status IN ('DRAFT','PUBLISHED','CANCELLED','COMPLETED')),
    CONSTRAINT chk_training_dates  CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
);
CREATE INDEX IF NOT EXISTS idx_trainings_tutor  ON trainings(tutor_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_trainings_status ON trainings(status)   WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------
-- Video library — tutor-owned uploaded videos (backed by StorageService).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS videos (
    id            UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title         VARCHAR(200) NOT NULL,
    storage       VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    bucket        VARCHAR(120),
    object_key    VARCHAR(512),
    mime_type     VARCHAR(120),
    byte_size     BIGINT NOT NULL DEFAULT 0,
    duration_sec  INTEGER,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    version       BIGINT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ,
    created_by    UUID,
    updated_by    UUID,
    CONSTRAINT chk_video_storage CHECK (storage IN ('S3','LOCAL','CDN')),
    CONSTRAINT chk_video_status  CHECK (status IN ('PENDING','UPLOADED','PROCESSING','READY','FAILED'))
);
CREATE INDEX IF NOT EXISTS idx_videos_owner ON videos(owner_user_id) WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------
-- Password reset + email verification tokens (single-use, hashed).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  CHAR(64) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_pwd_reset_user    ON password_reset_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_pwd_reset_expires ON password_reset_tokens(expires_at);

CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  CHAR(64) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_email_verify_user    ON email_verification_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_email_verify_expires ON email_verification_tokens(expires_at);

-- ---------------------------------------------------------------------
-- updated_at triggers for the new tables that carry updated_at.
-- (The V1 trigger-installer DO-block only ran against tables existing then.)
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_trainings_updated_at ON trainings;
CREATE TRIGGER trg_trainings_updated_at BEFORE UPDATE ON trainings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_videos_updated_at ON videos;
CREATE TRIGGER trg_videos_updated_at BEFORE UPDATE ON videos
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
