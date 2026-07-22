# ProofVault Auth Server

Spring Authorization Server microservice for ProofVault. It provides OAuth2 and OpenID Connect issuer endpoints, persistent registered clients, persistent authorizations, local bootstrap users, JDBC-backed users, Prometheus metrics, and OpenTelemetry tracing.

## Local Run

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Local defaults:

- Issuer: `http://localhost:9000`
- Bootstrap user: `admin@proofvault.local`
- Bootstrap password: `ChangeMeLocal123!`
- Client ID: `proofvault-web`
- Client secret: `proofvault-local-secret`

## Docker Run

From the project root:

```bash
docker compose -f backend/docker-compose.yml up --build
```

Auth server URLs:

- Metadata: `http://localhost:9000/api/authserver/metadata`
- OIDC discovery: `http://localhost:9000/.well-known/openid-configuration`
- JWKS: `http://localhost:9000/oauth2/jwks`
- Token endpoint: `http://localhost:9000/oauth2/token`
- Authorization endpoint: `http://localhost:9000/oauth2/authorize`

Client credentials token check:

```bash
curl -u proofvault-web:proofvault-local-secret \
  -d grant_type=client_credentials \
  -d "scope=proof:read proof:write" \
  http://localhost:9000/oauth2/token
```

## Profiles

| Profile | Purpose | Database | Signing key |
| --- | --- | --- | --- |
| `local` | Fast local development | H2 | ephemeral allowed |
| `docker` | Local container stack | MySQL | ephemeral allowed |
| `dev` | Shared developer environment | MySQL | configured key required by default |
| `staging` | Pre-production | MySQL | configured key required |
| `prod` | Production | MySQL | configured key required |

## Production Settings

Set these outside local development:

```bash
AUTH_ISSUER=https://auth.your-domain.example
AUTH_AUDIENCE=proofvault-api
AUTH_BOOTSTRAP_ENABLED=false
AUTH_SIGNING_REQUIRE_CONFIGURED_KEY=true
AUTH_SIGNING_PRIVATE_KEY_PEM="-----BEGIN PRIVATE KEY-----..."
AUTH_SIGNING_PUBLIC_KEY_PEM="-----BEGIN PUBLIC KEY-----..."
DB_URL=jdbc:mysql://...
DB_USERNAME=...
DB_PASSWORD=...
OTEL_EXPORTER_OTLP_ENDPOINT=https://otel-collector.example/v1/traces
```

The server uses JDBC-backed Spring Authorization Server stores:

- `oauth2_registered_client`
- `oauth2_authorization`
- `oauth2_authorization_consent`
- `users`
- `authorities`
