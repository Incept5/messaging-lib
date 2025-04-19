CREATE TABLE IF NOT EXISTS messages (
  topic VARCHAR(255) NOT NULL,
  payload_json LONGVARCHAR NOT NULL,
  type VARCHAR(255) NOT NULL,
  message_id UUID NOT NULL,
  created_at TIMESTAMP NOT NULL,
  correlation_id VARCHAR(255) NOT NULL,
  trace_id VARCHAR(255),
  reply_to VARCHAR(255),
  PRIMARY KEY (message_id)
);