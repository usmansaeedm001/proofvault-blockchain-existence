# ProofVault Backend

Spring Boot API for ProofVault with OAuth2 resource-server security, user-scoped proof storage, Flyway migrations, MySQL-ready persistence, and OpenTelemetry/Micrometer observability.

## Profiles

| Profile | Purpose | Auth | Database |
| --- | --- | --- | --- |
| `local` | Fast local development | OAuth2 JWT required by default | H2 in-memory |
| `anvil` | Local Anvil chain | OAuth2 JWT required | MySQL |
| `sepolia` | Sepolia testnet | OAuth2 JWT required | MySQL |
| `docker` | Local container stack | disabled by default | MySQL container |
| `dev` | Shared developer environment | OAuth2 JWT required | MySQL |
| `staging` | Pre-production | OAuth2 JWT required | MySQL |
| `prod` | Production | OAuth2 JWT required | MySQL |

Run local:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Run the backend Docker stack from the project root:

```bash
docker compose -f backend/docker-compose.yml up --build
```

Or from inside `backend/`:

```bash
docker compose up --build
```

Services:

- API: `http://localhost:8080`
- Auth server: `http://localhost:9000`
- MySQL: `localhost:3306`
- External Anvil RPC for `anvil` and `local`: `http://172.25.179.4:8545`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- OTEL Collector internal metrics: `http://localhost:8888/metrics`
- OTLP HTTP: `http://localhost:4318`
- OTLP gRPC: `localhost:4317`

Run production-like:

```bash
set SPRING_PROFILES_ACTIVE=prod
set DB_URL=jdbc:mysql://localhost:3306/proofvault
set DB_USERNAME=proofvault
set DB_PASSWORD=proofvault
set OAUTH2_ISSUER_URI=https://issuer.example.com/
set OAUTH2_AUDIENCE=proofvault-api
set OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318/v1/traces
mvn spring-boot:run
```

## OAuth2

The API is implemented as an OAuth2 resource server. It validates:

- issuer;
- token signature through issuer discovery or JWKS;
- expiry and standard JWT constraints;
- configured audience through `OAUTH2_AUDIENCE`.

Provider examples that can work with this setup:

- Keycloak
- Auth0
- AWS Cognito
- Azure Entra ID
- Okta

Protected endpoints expect:

```http
Authorization: Bearer <access_token>
```

The local user database is keyed by JWT `issuer` plus `subject`.

## OpenTelemetry

The backend uses Spring Boot Actuator, Micrometer tracing, and the OTLP exporter.

Important settings:

```bash
OTEL_TRACING_ENABLED=true
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318/v1/traces
OTEL_TRACES_SAMPLER_PROBABILITY=1.0
```

Operational endpoints:

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/prometheus`

Logs include trace ID, span ID, and request ID when available.

Blockchain operations add spans and metrics:

- span `proofvault.blockchain.store`
- span `proofvault.blockchain.verify`
- span `proofvault.blockchain.insights`
- metric `proofvault.blockchain.anchors`
- metric `proofvault.blockchain.verifications`
- metric `proofvault.blockchain.errors`
- timer `proofvault.blockchain.anchor.duration`
- timer `proofvault.blockchain.verify.duration`

## Blockchain Profiles

Use `anvil` for a local chain:

```bash
SPRING_PROFILES_ACTIVE=anvil
BLOCKCHAIN_MODE=ethereum
BLOCKCHAIN_RPC_URL=http://172.25.179.4:8545
BLOCKCHAIN_CHAIN_ID=31337
```

Use `sepolia` for testnet:

```bash
SPRING_PROFILES_ACTIVE=sepolia
BLOCKCHAIN_MODE=ethereum
BLOCKCHAIN_RPC_URL=https://rpc.sepolia.org
BLOCKCHAIN_CHAIN_ID=11155111
EXPLORER_BASE_URL=https://sepolia.etherscan.io/tx/
```

When using real chain mode, provide the deployed UUPS proxy address and an anchor private key:

```bash
PROOFVAULT_CONTRACT_ADDRESS=0xYourProxyAddress
PROOFVAULT_ANCHOR_ADDRESS=0xYourAnchorAddress
PROOFVAULT_ANCHOR_PRIVATE_KEY=0xYourAnchorPrivateKey
BLOCKCHAIN_GAS_PRICE_WEI=1000000000
BLOCKCHAIN_GAS_LIMIT=500000
```

The API can start before these values are set. `/api/blockchain/status` will report the JSON-RPC connection plus any missing contract/key configuration. Upload anchoring and on-chain proof lookup require `PROOFVAULT_CONTRACT_ADDRESS`; anchoring also requires `PROOFVAULT_ANCHOR_PRIVATE_KEY`.

You can still force mock mode when needed:

```bash
BLOCKCHAIN_MODE=mock
```

Blockchain insight APIs:

- `GET /api/blockchain/status`
- `GET /api/blockchain/insights`
- `GET /api/blockchain/proofs/{fileHash}`
- `GET /api/observability/blockchain`

## Postman

Import:

- `postman/ProofVault.postman_collection.json`
- `postman/ProofVault.postman_environment.json`

For `anvil`, `sepolia`, `dev`, `staging`, and `prod`, set `accessToken` from the auth server wallet flow or use the OAuth2 token request in the collection.

## API Surface

- `GET /api/me`
- `GET /api/subscription`
- `GET /api/proofs`
- `POST /api/proofs/upload`
- `POST /api/proofs/verify`
- `GET /api/proofs/{proofId}/certificate`
- `GET /api/blockchain/status`
- `GET /api/blockchain/insights`
- `GET /api/blockchain/proofs/{fileHash}`
- `GET /api/observability/blockchain`

## Production Checklist

- Use MySQL with Flyway migrations enabled.
- Use a real OAuth2/OIDC issuer and audience.
- Keep `AUTHENTICATION_ENABLED=true` outside local development.
- Send OTLP traces to an OpenTelemetry Collector.
- Keep `/actuator/prometheus` behind network controls.
- Replace mock blockchain anchoring with a relayer/web3 adapter before mainnet.
