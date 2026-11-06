-- Read-only account used by data-api business pools (M16).
-- Replace password and database list before running. Never grant write/DDL privileges.

CREATE USER IF NOT EXISTS 'ds_api_ro'@'%' IDENTIFIED BY 'CHANGE_ME_STRONG_PASSWORD';

-- one GRANT per published database
GRANT SELECT ON `osint`.* TO 'ds_api_ro'@'%';
-- GRANT SELECT ON `demo`.* TO 'ds_api_ro'@'%';

FLUSH PRIVILEGES;

-- Verify from the data-api host:
--   mysql -h <host> -u ds_api_ro -p -e "select 1"
--   mysql -h <host> -u ds_api_ro -p osint -e "update <table> set <col>=<col> limit 0"  -- must fail
