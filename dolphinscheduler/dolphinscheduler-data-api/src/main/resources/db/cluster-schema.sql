-- Shared magic-api resource store used by the "cluster" profile.
CREATE TABLE IF NOT EXISTS magic_api_file (
    file_path    VARCHAR(255) PRIMARY KEY,
    file_content TEXT
);
