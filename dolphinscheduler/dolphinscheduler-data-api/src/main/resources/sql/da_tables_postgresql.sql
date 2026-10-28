-- Data API extension tables (PostgreSQL metadata DB)

CREATE TABLE IF NOT EXISTS da_api_asset_bind (
    id              BIGSERIAL PRIMARY KEY,
    api_path        VARCHAR(255) NOT NULL,
    datasource_id   INT          NULL,
    asset_urn       VARCHAR(512) NULL,
    status          SMALLINT     NOT NULL DEFAULT 1,
    owner_user_id   INT          NULL,
    version         INT          NOT NULL DEFAULT 1,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_da_api_path UNIQUE (api_path)
);

CREATE TABLE IF NOT EXISTS da_api_invoke_log (
    id              BIGSERIAL PRIMARY KEY,
    api_path        VARCHAR(255) NOT NULL,
    user_id         VARCHAR(64)  NULL,
    app_id          VARCHAR(64)  NULL,
    latency_ms      BIGINT       NULL,
    row_count       INT          NULL,
    code            INT          NULL,
    trace_id        VARCHAR(64)  NULL,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_da_api_invoke_api_time ON da_api_invoke_log (api_path, create_time);

CREATE TABLE IF NOT EXISTS da_app_credential (
    id              BIGSERIAL PRIMARY KEY,
    app_id          VARCHAR(64)  NOT NULL,
    app_key_hash    VARCHAR(128) NOT NULL,
    user_id         INT          NOT NULL,
    scopes          VARCHAR(512) NULL,
    expire_time     TIMESTAMP    NULL,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_da_app_id UNIQUE (app_id)
);

CREATE TABLE IF NOT EXISTS da_api_acl (
    id              BIGSERIAL PRIMARY KEY,
    api_path        VARCHAR(255) NOT NULL,
    subject_type    SMALLINT     NOT NULL,
    subject_id      VARCHAR(128) NOT NULL,
    effect          SMALLINT     NOT NULL DEFAULT 1,
    remark          VARCHAR(255) NULL,
    create_by       INT          NULL,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_da_acl UNIQUE (api_path, subject_type, subject_id)
);

CREATE INDEX IF NOT EXISTS idx_da_acl_subject ON da_api_acl (subject_type, subject_id);

-- Shared magic-api resource store (cluster profile, M14)
CREATE TABLE IF NOT EXISTS magic_api_file (
    file_path       VARCHAR(255) PRIMARY KEY,
    file_content    TEXT         NULL
);

-- Publish history (M13)
CREATE TABLE IF NOT EXISTS da_api_release (
    id              BIGSERIAL PRIMARY KEY,
    api_path        VARCHAR(255) NOT NULL,
    version         INT          NOT NULL,
    method          VARCHAR(16)  NULL,
    script          TEXT         NULL,
    entity_json     TEXT         NULL,
    state           VARCHAR(16)  NOT NULL DEFAULT 'PUBLISHED',
    remark          VARCHAR(512) NULL,
    create_by       INT          NULL,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_da_release UNIQUE (api_path, version)
);

CREATE INDEX IF NOT EXISTS idx_da_release_path ON da_api_release (api_path, create_time);

-- API → table references (M22)
CREATE TABLE IF NOT EXISTS da_api_table_ref (
    id              BIGSERIAL PRIMARY KEY,
    api_path        VARCHAR(255) NOT NULL,
    datasource_key  VARCHAR(128) NULL,
    table_name      VARCHAR(512) NOT NULL,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_da_table_ref UNIQUE (api_path, datasource_key, table_name)
);

CREATE INDEX IF NOT EXISTS idx_da_table_ref_table ON da_api_table_ref (table_name);
