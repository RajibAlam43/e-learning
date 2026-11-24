-- Run using psql as a database administrator authorized to manage migration_user.
-- Users must already exist, with distinct passwords supplied outside this file.
-- Example (no passwords in command history):
-- psql "$ADMIN_DATABASE_URL" -v ON_ERROR_STOP=1 -v db_name=gii_stage_db \
--   -v migration_user=gii_stage_migration -v api_user=gii_stage_api \
--   -v worker_user=gii_stage_worker -f deploy/db/permissions.sql
-- Change identifiers for prod. Never use application users as administrators.

BEGIN;
GRANT CONNECT ON DATABASE :"db_name"
    TO :"migration_user", :"api_user", :"worker_user";
GRANT CREATE ON DATABASE :"db_name" TO :"migration_user";
GRANT USAGE, CREATE ON SCHEMA public TO :"migration_user";
GRANT USAGE ON SCHEMA public TO :"api_user", :"worker_user";

-- Install extensions administratively before the first application migration.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public
    TO :"api_user", :"worker_user";
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public
    TO :"api_user", :"worker_user";
ALTER DEFAULT PRIVILEGES FOR ROLE :"migration_user" IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO :"api_user", :"worker_user";
ALTER DEFAULT PRIVILEGES FOR ROLE :"migration_user" IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO :"api_user", :"worker_user";
COMMIT;
