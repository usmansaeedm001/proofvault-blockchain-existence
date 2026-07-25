CREATE TABLE wallet_auth_challenges (
  id VARCHAR(36) NOT NULL,
  wallet_address VARCHAR(42) NOT NULL,
  nonce_hash VARCHAR(64) NOT NULL,
  chain_id BIGINT NOT NULL,
  domain VARCHAR(255) NOT NULL,
  issued_at TIMESTAMP NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  consumed_at TIMESTAMP DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE INDEX ix_wallet_auth_challenges_wallet ON wallet_auth_challenges(wallet_address);
CREATE INDEX ix_wallet_auth_challenges_nonce_hash ON wallet_auth_challenges(nonce_hash);
