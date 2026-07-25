CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  public_id VARCHAR(36) NOT NULL,
  issuer VARCHAR(512) NOT NULL,
  subject VARCHAR(255) NOT NULL,
  email VARCHAR(320) NOT NULL,
  display_name VARCHAR(255) NOT NULL,
  subscription_tier VARCHAR(32) NOT NULL,
  usage_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_users_public_id UNIQUE (public_id),
  CONSTRAINT uk_users_issuer_subject UNIQUE (issuer, subject)
);

CREATE INDEX idx_users_email ON users (email);

CREATE TABLE proofs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  owner_id BIGINT NOT NULL,
  public_id VARCHAR(36) NOT NULL,
  file_name VARCHAR(512) NOT NULL,
  file_hash VARCHAR(64) NOT NULL,
  file_size BIGINT NOT NULL,
  content_type VARCHAR(255) NOT NULL,
  blockchain_timestamp TIMESTAMP(6) NOT NULL,
  transaction_hash VARCHAR(128) NOT NULL,
  network VARCHAR(64) NOT NULL,
  tier_at_creation VARCHAR(32) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_proofs_public_id UNIQUE (public_id),
  CONSTRAINT fk_proofs_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE INDEX idx_proofs_file_hash ON proofs (file_hash);
CREATE INDEX idx_proofs_owner_created_at ON proofs (owner_id, created_at);
