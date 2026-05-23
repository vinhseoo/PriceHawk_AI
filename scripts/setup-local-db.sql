-- =============================================================================
-- PriceHawk AI — Local PostgreSQL Setup
-- Run ONCE before starting services for the first time.
--
-- Usage (Windows — run as postgres user):
--   psql -U postgres -h localhost -f scripts\setup-local-db.sql
--
-- What this does:
--   1. Creates catalog_db and scraper_db (user_db uses the existing 'postgres' DB)
--   2. Creates the 'pricehawk' schema in the postgres DB (for user-service)
--   3. Enables required extensions in each DB
-- =============================================================================

-- ─── catalog_db ───────────────────────────────────────────────────────────────
SELECT 'Creating catalog_db...' AS step;
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'catalog_db') THEN
    PERFORM dblink_exec('dbname=postgres', 'CREATE DATABASE catalog_db');
  END IF;
END $$;

-- Fallback: if dblink not available, use psql meta-command (run separately if needed)
-- \! createdb -U postgres catalog_db

\c catalog_db

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
SELECT 'catalog_db ready' AS status;

-- ─── scraper_db ───────────────────────────────────────────────────────────────
\c postgres

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'scraper_db') THEN
    PERFORM dblink_exec('dbname=postgres', 'CREATE DATABASE scraper_db');
  END IF;
END $$;

\c scraper_db

CREATE EXTENSION IF NOT EXISTS pg_trgm;
SELECT 'scraper_db ready' AS status;

-- ─── postgres DB — pricehawk schema (for user-service) ───────────────────────
\c postgres

CREATE SCHEMA IF NOT EXISTS pricehawk;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
SELECT 'pricehawk schema ready in postgres DB' AS status;
