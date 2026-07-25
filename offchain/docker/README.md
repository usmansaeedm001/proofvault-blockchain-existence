# ProofVault Offchain Docker Stack

From `offchain/`, run the offchain-only local stack:

```bash
docker compose up --build
```

From the project root, run:

```bash
docker compose -f offchain/docker-compose.yml up --build
```

To run the full app with the frontend from the project root:

```bash
docker compose up --build
```

Use a specific root environment file when needed:

```bash
docker compose -f docker-compose.anvil.yml up --build
docker compose --env-file docker-compose.sepolia.env -f docker-compose.sepolia.yml up --build
docker compose --env-file docker-compose.prod.env -f docker-compose.prod.yml up --build
```

Services:

| Service | URL |
| --- | --- |
| Offchain API | `http://localhost:8080` |
| Auth Server | `http://localhost:9000` |
| MySQL | `localhost:3306` |
| External Anvil RPC | `http://172.25.179.4:8545` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |
| OTEL Collector internal metrics | `http://localhost:8888/metrics` |
| OTLP HTTP | `http://localhost:4318` |
| OTLP gRPC | `localhost:4317` |

Grafana local credentials:

```text
username: admin
password: proofvault_admin
```

Grafana automatically provisions these dashboards from `offchain/docker/grafana/dashboards`:

- ProofVault API Overview
- ProofVault Auth Server Overview
- ProofVault Blockchain & OTEL

The default offchain stack uses `BLOCKCHAIN_MODE=ethereum` with the local Anvil profile. Start Anvil separately and set the deployed proxy address before anchoring proofs.

If you already created the MySQL volume before the auth server was added, recreate it once so `proofvault_auth` is initialized:

```bash
docker compose down -v
docker compose up --build
```

To use the real smart contract path:

1. Deploy the UUPS ProofVault proxy to Anvil.
2. Set these API environment values in `offchain/docker-compose.yml` or an override file:

   ```yaml
   BLOCKCHAIN_MODE: ethereum
   BLOCKCHAIN_RPC_URL: http://172.25.179.4:8545
   BLOCKCHAIN_CHAIN_ID: "31337"
   PROOFVAULT_CONTRACT_ADDRESS: "0xYourProxyAddress"
   PROOFVAULT_ANCHOR_ADDRESS: "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266"
   PROOFVAULT_ANCHOR_PRIVATE_KEY: "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80"
   ```

3. Restart the API:

   ```bash
   docker compose up -d --build proofvault-api
   ```

Useful checks:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:9000/actuator/health
curl http://localhost:9000/.well-known/openid-configuration
curl http://localhost:8080/api/blockchain/status
curl http://localhost:8080/api/observability/blockchain
```

Get a local client-credentials access token:

```bash
curl -u proofvault-web:proofvault-local-secret \
  -d grant_type=client_credentials \
  -d "scope=proof:read proof:write" \
  http://localhost:9000/oauth2/token
```
