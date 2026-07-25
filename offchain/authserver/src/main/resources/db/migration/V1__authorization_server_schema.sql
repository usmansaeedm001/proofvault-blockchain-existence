CREATE TABLE users (
  username VARCHAR(100) NOT NULL,
  password VARCHAR(500) NOT NULL,
  enabled BOOLEAN NOT NULL,
  PRIMARY KEY (username)
);

CREATE TABLE authorities (
  username VARCHAR(100) NOT NULL,
  authority VARCHAR(100) NOT NULL,
  CONSTRAINT fk_authorities_users FOREIGN KEY (username) REFERENCES users(username)
);

CREATE UNIQUE INDEX ix_auth_username_authority ON authorities(username, authority);

CREATE TABLE oauth2_registered_client (
  id VARCHAR(100) NOT NULL,
  client_id VARCHAR(100) NOT NULL,
  client_id_issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  client_secret VARCHAR(200) DEFAULT NULL,
  client_secret_expires_at TIMESTAMP DEFAULT NULL,
  client_name VARCHAR(200) NOT NULL,
  client_authentication_methods VARCHAR(1000) NOT NULL,
  authorization_grant_types VARCHAR(1000) NOT NULL,
  redirect_uris VARCHAR(1000) DEFAULT NULL,
  post_logout_redirect_uris VARCHAR(1000) DEFAULT NULL,
  scopes VARCHAR(1000) NOT NULL,
  client_settings VARCHAR(2000) NOT NULL,
  token_settings VARCHAR(2000) NOT NULL,
  PRIMARY KEY (id)
);

CREATE UNIQUE INDEX ix_oauth2_registered_client_client_id ON oauth2_registered_client(client_id);

CREATE TABLE oauth2_authorization (
  id VARCHAR(100) NOT NULL,
  registered_client_id VARCHAR(100) NOT NULL,
  principal_name VARCHAR(200) NOT NULL,
  authorization_grant_type VARCHAR(100) NOT NULL,
  authorized_scopes VARCHAR(1000) DEFAULT NULL,
  attributes BLOB DEFAULT NULL,
  state VARCHAR(500) DEFAULT NULL,
  authorization_code_value BLOB DEFAULT NULL,
  authorization_code_issued_at TIMESTAMP DEFAULT NULL,
  authorization_code_expires_at TIMESTAMP DEFAULT NULL,
  authorization_code_metadata BLOB DEFAULT NULL,
  access_token_value BLOB DEFAULT NULL,
  access_token_issued_at TIMESTAMP DEFAULT NULL,
  access_token_expires_at TIMESTAMP DEFAULT NULL,
  access_token_metadata BLOB DEFAULT NULL,
  access_token_type VARCHAR(100) DEFAULT NULL,
  access_token_scopes VARCHAR(1000) DEFAULT NULL,
  oidc_id_token_value BLOB DEFAULT NULL,
  oidc_id_token_issued_at TIMESTAMP DEFAULT NULL,
  oidc_id_token_expires_at TIMESTAMP DEFAULT NULL,
  oidc_id_token_metadata BLOB DEFAULT NULL,
  refresh_token_value BLOB DEFAULT NULL,
  refresh_token_issued_at TIMESTAMP DEFAULT NULL,
  refresh_token_expires_at TIMESTAMP DEFAULT NULL,
  refresh_token_metadata BLOB DEFAULT NULL,
  user_code_value BLOB DEFAULT NULL,
  user_code_issued_at TIMESTAMP DEFAULT NULL,
  user_code_expires_at TIMESTAMP DEFAULT NULL,
  user_code_metadata BLOB DEFAULT NULL,
  device_code_value BLOB DEFAULT NULL,
  device_code_issued_at TIMESTAMP DEFAULT NULL,
  device_code_expires_at TIMESTAMP DEFAULT NULL,
  device_code_metadata BLOB DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE INDEX ix_oauth2_authorization_state ON oauth2_authorization(state);

CREATE TABLE oauth2_authorization_consent (
  registered_client_id VARCHAR(100) NOT NULL,
  principal_name VARCHAR(200) NOT NULL,
  authorities VARCHAR(1000) NOT NULL,
  PRIMARY KEY (registered_client_id, principal_name)
);
