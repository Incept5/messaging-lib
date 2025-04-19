-- This is just an example of a migration file for the scheduler
-- This file needs to be copied to resources/db/migration/
-- and renamed to V1__init_schedules.sql or equivalent to be picked up by flyway etc
CREATE SCHEMA IF NOT EXISTS ${flyway:defaultSchema};

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.example_data (
  id uuid NOT NULL PRIMARY KEY
);
