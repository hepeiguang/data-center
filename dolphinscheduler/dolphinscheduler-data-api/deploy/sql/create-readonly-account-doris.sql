-- Read-only account for Apache Doris (M16).
-- Replace password and database list before running. SELECT_PRIV only; no LOAD/ALTER/CREATE.

CREATE USER 'ds_api_ro' IDENTIFIED BY 'CHANGE_ME_STRONG_PASSWORD';

GRANT SELECT_PRIV ON demo.* TO 'ds_api_ro'@'%';
-- GRANT SELECT_PRIV ON osint.* TO 'ds_api_ro'@'%';

-- Verify from the data-api host:
--   mysql -h <fe-host> -P 9030 -u ds_api_ro -p -e "select 1"
--   mysql -h <fe-host> -P 9030 -u ds_api_ro -p demo -e "insert into <t> values(...)"  -- must fail
