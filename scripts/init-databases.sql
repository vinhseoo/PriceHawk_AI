-- PriceHawk AI — Initialize all databases
CREATE DATABASE user_db;
CREATE DATABASE catalog_db;
CREATE DATABASE scraper_db;

-- Enable pgvector for catalog_db
\c catalog_db
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

\c user_db
CREATE EXTENSION IF NOT EXISTS pg_trgm;

\c scraper_db
CREATE EXTENSION IF NOT EXISTS pg_trgm;
