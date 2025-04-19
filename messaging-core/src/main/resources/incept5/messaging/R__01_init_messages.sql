-- This will run automatically only if flyway is told to look in incept5/messaging as well as db/migration
CREATE SCHEMA IF NOT EXISTS ${flyway:defaultSchema};
CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.messages (
  topic text NOT NULL,
  payload_json text NOT NULL,
  type text NOT NULL,
  message_id uuid NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
  correlation_id text NOT NULL,
  trace_id text NOT NULL,
  reply_to text,
  PRIMARY KEY (message_id)
);
