-- Data API extension tables for H2 (standalone shared metadata DB)

CREATE TABLE IF NOT EXISTS da_api_asset_bind (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_path        VARCHAR(255) NOT NULL,
    datasource_id   INT,
    asset_urn       VARCHAR(512),
    status          TINYINT NOT NULL DEFAULT 1,
    owner_user_id   INT,
    version         INT NOT NULL DEFAULT 1,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_da_api_path ON da_api_asset_bind(api_path);

CREATE TABLE IF NOT EXISTS da_api_invoke_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_path        VARCHAR(255) NOT NULL,
    user_id         VARCHAR(64),
    app_id          VARCHAR(64),
    latency_ms      BIGINT,
    row_count       INT,
    code            INT,
    trace_id        VARCHAR(64),
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS da_app_credential (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    app_id          VARCHAR(64) NOT NULL,
    app_key_hash    VARCHAR(128) NOT NULL,
    user_id         INT NOT NULL,
    scopes          VARCHAR(512),
    expire_time     TIMESTAMP,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_da_app_id ON da_app_credential(app_id);

CREATE TABLE IF NOT EXISTS da_api_acl (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_path        VARCHAR(255) NOT NULL,
    subject_type    TINYINT NOT NULL,
    subject_id      VARCHAR(128) NOT NULL,
    effect          TINYINT NOT NULL DEFAULT 1,
    remark          VARCHAR(255),
    create_by       INT,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_da_acl ON da_api_acl(api_path, subject_type, subject_id);
CREATE INDEX IF NOT EXISTS idx_da_acl_subject ON da_api_acl(subject_type, subject_id);

-- Shared magic-api resource store (cluster profile, M14)
CREATE TABLE IF NOT EXISTS magic_api_file (
    file_path       VARCHAR(255) NOT NULL PRIMARY KEY,
    file_content    CLOB         NULL
);

-- Publish history (M13)
CREATE TABLE IF NOT EXISTS da_api_release (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_path        VARCHAR(255) NOT NULL,
    version         INT          NOT NULL,
    method          VARCHAR(16)  NULL,
    script          CLOB         NULL,
    entity_json     CLOB         NULL,
    state           VARCHAR(16)  NOT NULL DEFAULT 'PUBLISHED',
    remark          VARCHAR(512) NULL,
    create_by       INT          NULL,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_da_release ON da_api_release(api_path, version);
CREATE INDEX IF NOT EXISTS idx_da_release_path ON da_api_release(api_path, create_time);

-- API → table references (M22)
CREATE TABLE IF NOT EXISTS da_api_table_ref (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_path        VARCHAR(255) NOT NULL,
    datasource_key  VARCHAR(128) NULL,
    table_name      VARCHAR(512) NOT NULL,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_da_table_ref ON da_api_table_ref(api_path, datasource_key, table_name);
CREATE INDEX IF NOT EXISTS idx_da_table_ref_table ON da_api_table_ref(table_name);
