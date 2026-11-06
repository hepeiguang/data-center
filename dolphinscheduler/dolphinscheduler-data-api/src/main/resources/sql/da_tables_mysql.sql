-- Data API extension tables (same metadata DB as DolphinScheduler)
-- Run once against the DS metadata database.

CREATE TABLE IF NOT EXISTS da_api_asset_bind (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    api_path        VARCHAR(255) NOT NULL,
    datasource_id   INT          NULL,
    asset_urn       VARCHAR(512) NULL COMMENT 'e.g. doris://cluster/db.table',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1=online 0=offline',
    owner_user_id   INT          NULL,
    version         INT          NOT NULL DEFAULT 1,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_api_path (api_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS da_api_invoke_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    api_path        VARCHAR(255) NOT NULL,
    user_id         VARCHAR(64)  NULL,
    app_id          VARCHAR(64)  NULL,
    latency_ms      BIGINT       NULL,
    row_count       INT          NULL,
    code            INT          NULL,
    trace_id        VARCHAR(64)  NULL,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_api_time (api_path, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS da_app_credential (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    app_id          VARCHAR(64)  NOT NULL,
    app_key_hash    VARCHAR(128) NOT NULL,
    user_id         INT          NOT NULL,
    scopes          VARCHAR(512) NULL,
    expire_time     DATETIME     NULL,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_app_id (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- API ACL: which subject may call which gateway path (relative to /data-api/gateway)
-- subject_type: 1=USER 2=TOKEN 3=APP
-- effect: 1=ALLOW 0=DENY
-- api_path: exact (/smoke/select1) or prefix wildcard (/smoke/**)
CREATE TABLE IF NOT EXISTS da_api_acl (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    api_path        VARCHAR(255) NOT NULL,
    subject_type    TINYINT      NOT NULL COMMENT '1=USER 2=TOKEN 3=APP',
    subject_id      VARCHAR(128) NOT NULL COMMENT 'userId / accessToken / appId',
    effect          TINYINT      NOT NULL DEFAULT 1 COMMENT '1=ALLOW 0=DENY',
    remark          VARCHAR(255) NULL,
    create_by       INT          NULL,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_da_acl (api_path, subject_type, subject_id),
    KEY idx_da_acl_subject (subject_type, subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Shared magic-api resource store (cluster profile, M14)
CREATE TABLE IF NOT EXISTS magic_api_file (
    file_path       VARCHAR(255) NOT NULL PRIMARY KEY,
    file_content    LONGTEXT     NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Publish history (M13)
CREATE TABLE IF NOT EXISTS da_api_release (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    api_path        VARCHAR(255) NOT NULL,
    version         INT          NOT NULL,
    method          VARCHAR(16)  NULL,
    script          LONGTEXT     NULL,
    entity_json     LONGTEXT     NULL,
    state           VARCHAR(16)  NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED/ROLLBACK/OFFLINE',
    remark          VARCHAR(512) NULL,
    create_by       INT          NULL,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_da_release (api_path, version),
    KEY idx_da_release_path (api_path, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- API → table references (M22)
CREATE TABLE IF NOT EXISTS da_api_table_ref (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    api_path        VARCHAR(255) NOT NULL,
    datasource_key  VARCHAR(128) NULL,
    table_name      VARCHAR(512) NOT NULL,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_da_table_ref (api_path, datasource_key, table_name),
    KEY idx_da_table_ref_table (table_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
