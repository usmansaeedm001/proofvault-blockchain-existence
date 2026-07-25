# ProofVault Backend

Backend services are split into two Spring Boot microservices:

- `proofvault-api/`: ProofVault resource API for proofs, subscriptions, blockchain anchoring, certificates, Postman collection, and API Dockerfile.
- `authserver/`: Spring Authorization Server for OAuth2 and OpenID Connect.

Shared backend infrastructure lives here:

- `docker-compose.yml`
- `docker/`: MySQL init scripts, OpenTelemetry Collector, Prometheus, and Grafana dashboards.

## Backend Stack

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
- External Anvil RPC: `http://172.25.179.4:8545`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- OTEL Collector internal metrics: `http://localhost:8888/metrics`
- OTLP HTTP: `http://localhost:4318`
- OTLP gRPC: `localhost:4317`

## Chain Profiles

ProofVault now has explicit chain profiles:

| Profile | Chain | Chain ID | Purpose |
| --- | --- | --- | --- |
| `anvil` | Local Anvil | `31337` | Local blockchain development and demos |
| `sepolia` | Sepolia testnet | `11155111` | Client demos with a public testnet |
| `prod` | Production network | env driven | Deployed SaaS runtime |

For Anvil local chain:

```bash
SPRING_PROFILES_ACTIVE=anvil
```

Use these examples:

- `proofvault-api/.env.anvil.example`
- `authserver/.env.anvil.example`
- `../frontend/.env.anvil.example`

For Sepolia testnet:

```bash
SPRING_PROFILES_ACTIVE=sepolia
```

Use these examples:

- `proofvault-api/.env.sepolia.example`
- `authserver/.env.sepolia.example`
- `../frontend/.env.sepolia.example`

For production:

```bash
SPRING_PROFILES_ACTIVE=prod
```

Use these examples:

- `proofvault-api/.env.production.example`
- `authserver/.env.production.example`
- `../frontend/.env.production.example`

The frontend must use the same wallet chain ID as the auth server. The API must use the same blockchain chain ID and deployed proxy address.

Local bootstrap auth values:

- Username: `admin@proofvault.local`
- Password: `ChangeMeLocal123!`
- Client ID: `proofvault-web`
- Client secret: `proofvault-local-secret`

Postman collections:

- `proofvault-api/postman/ProofVault.postman_collection.json`
- `authserver/postman/ProofVaultAuthServer.postman_collection.json`

## Full App

From the project root:

```bash
docker compose -f backend/docker-compose.yml -f docker-compose.yml up --build
```
