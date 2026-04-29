-- =========================================================
-- DATABASE PERMISSIONS SETUP FOR FLYWAY + APP USERS
-- Run this as ADMIN user (managed DB) in DBeaver
-- =========================================================

-- =========================================================
-- 1. Allow migration user to create extensions (e.g. pgcrypto)
-- Required for: CREATE EXTENSION pgcrypto;
-- =========================================================
GRANT CREATE ON DATABASE gii_stage_db TO gii_stage_migration;


-- =========================================================
-- 2. Allow migration user to create/manage schema objects
-- Required for: tables, indexes, constraints, etc.
-- =========================================================
GRANT USAGE, CREATE ON SCHEMA public TO gii_stage_migration;


-- =========================================================
-- 3. Allow API and worker users to access the schema
-- Without this, they cannot even see tables
-- =========================================================
GRANT USAGE ON SCHEMA public TO gii_stage_api, gii_stage_worker;


-- =========================================================
-- 4. Grant access to EXISTING tables
-- Important if you already ran migrations before this script
-- =========================================================
GRANT SELECT, INSERT, UPDATE, DELETE
ON ALL TABLES IN SCHEMA public
TO gii_stage_api, gii_stage_worker;


-- =========================================================
-- 5. Grant access to EXISTING sequences (auto-increment IDs)
-- Prevents "permission denied for sequence" errors
-- =========================================================
GRANT USAGE, SELECT
ON ALL SEQUENCES IN SCHEMA public
TO gii_stage_api, gii_stage_worker;


-- =========================================================
-- 6. Ensure FUTURE tables created by migration user
-- automatically grant access to API + worker
-- =========================================================
ALTER DEFAULT PRIVILEGES FOR USER gii_stage_migration IN SCHEMA public
GRANT SELECT, INSERT, UPDATE, DELETE
ON TABLES TO gii_stage_api, gii_stage_worker;


-- =========================================================
-- 7. Ensure FUTURE sequences also grant access
-- Required for SERIAL / IDENTITY columns
-- =========================================================
ALTER DEFAULT PRIVILEGES FOR USER gii_stage_migration IN SCHEMA public
GRANT USAGE, SELECT
ON SEQUENCES TO gii_stage_api, gii_stage_worker;