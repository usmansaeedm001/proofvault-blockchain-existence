CREATE DATABASE IF NOT EXISTS proofvault;
CREATE DATABASE IF NOT EXISTS proofvault_auth;

GRANT ALL PRIVILEGES ON proofvault.* TO 'proofvault'@'%';
GRANT ALL PRIVILEGES ON proofvault_auth.* TO 'proofvault'@'%';
FLUSH PRIVILEGES;
