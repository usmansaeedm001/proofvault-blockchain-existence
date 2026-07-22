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
- Anvil: `http://localhost:8545`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- OTEL Collector internal metrics: `http://localhost:8888/metrics`
- OTLP HTTP: `http://localhost:4318`
- OTLP gRPC: `localhost:4317`

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
