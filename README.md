# ProofVault

ProofVault is a blockchain proof-of-existence SaaS platform for proving that a digital file existed at a specific point in time. The application hashes uploaded files, anchors the hash on a blockchain, stores only proof metadata, and lets users verify authenticity later without exposing or storing the original file.

This repository is structured as a production-oriented MVP that can be shown to clients as a complete full-stack proof-of-concept: Next.js frontend, Spring Boot API, Spring Authorization Server, MySQL, Foundry smart contracts, Docker Compose, OpenTelemetry, Prometheus, and Grafana dashboards.

## Product Overview

ProofVault helps creators, freelancers, legal professionals, designers, developers, and businesses establish evidence of originality or authorship for digital work.

Core user flow:

1. User signs in through OAuth2/OpenID Connect.
2. User uploads a file through the web app.
3. The API generates a SHA-256 hash.
4. Only the hash and metadata are stored; the raw file is not stored.
5. The backend anchors the proof on-chain.
6. User receives a timestamped proof record and downloadable certificate.
7. Anyone can later verify the file hash.

## Key Features

- File proof creation with SHA-256 hashing
- Hash-only blockchain anchoring
- Public hash verification
- User-scoped proof history
- Certificate download
- OAuth2/OpenID Connect authentication
- Browser-safe PKCE login flow
- Professional blockchain insights dashboard
- Blockchain health and proof analytics
- OpenTelemetry tracing
- Prometheus metrics
- Grafana dashboards
- Docker Compose full-stack environment
- Upgradeable Solidity smart contract using OpenZeppelin UUPS

## Project Snaps

| Professional dashboard | Proof workflow | Observability |
| --- | --- | --- |
| ![ProofVault dashboard](./docs/screenshots/proofvault-dashboard.svg) | ![ProofVault proof workflow](./docs/screenshots/proofvault-proof-flow.svg) | ![ProofVault observability](./docs/screenshots/proofvault-observability.svg) |

## Architecture

```text
frontend/
  Next.js dashboard and OAuth PKCE browser client

backend/
  proofvault-api/
    Spring Boot resource API for proofs, certificates, blockchain, and insights

  authserver/
    Spring Authorization Server for OAuth2 and OpenID Connect

  docker/
    MySQL init scripts, OTEL Collector, Prometheus, Grafana dashboards

blockchain/
  Foundry smart contract package using Solidity and OpenZeppelin
```

High-level flow:

```text
User -> Next.js Frontend -> Auth Server -> ProofVault API -> MySQL
                                             |
                                             v
                                      Blockchain / Anvil
```

The frontend never writes directly to the blockchain. All blockchain operations are handled by the backend so hashing, authorization, wallet control, persistence, and observability stay centralized.

## Tech Stack

| Layer | Technology |
| --- | --- |
| Frontend | Next.js, React, Recharts, Lucide icons |
| API | Spring Boot, Spring Security, Spring Data JPA |
| Auth | Spring Authorization Server, OAuth2, OIDC, PKCE |
| Database | MySQL, Flyway migrations |
| Blockchain | Solidity, Foundry, OpenZeppelin |
| Observability | OpenTelemetry, Prometheus, Grafana |
| Local chain | Anvil |
| Runtime | Docker Compose |

## Repository Structure

```text
proofvault/
  frontend/
  backend/
    proofvault-api/
    authserver/
    docker/
    docker-compose.yml
  blockchain/
  docs/
  docker-compose.yml
```

## Full Stack Local Run

Prerequisites:

- Docker Desktop
- Node.js 20+ for frontend-only development
- Java 21 and Maven for backend-only development
- Foundry for smart contract development

Run the full stack from the project root:

```bash
docker compose -f backend/docker-compose.yml -f docker-compose.yml up --build
```

Available services:

| Service | URL |
| --- | --- |
| Frontend | `http://localhost:5173` |
| ProofVault API | `http://localhost:8080` |
| Auth Server | `http://localhost:9000` |
| MySQL | `localhost:3306` |
| Anvil | `http://localhost:8545` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |
| OTEL Collector metrics | `http://localhost:8888/metrics` |

Grafana credentials:

```text
username: admin
password: proofvault_admin
```

## Demo Login

The local auth server bootstraps demo credentials for client review:

```text
Email: admin@proofvault.local
Password: ChangeMeLocal123!
```

OAuth clients:

```text
Browser PKCE client: proofvault-spa
Service/Postman client: proofvault-web
Service client secret: proofvault-local-secret
```

If you previously ran the database before the auth server was added, reset local Docker volumes once so the auth bootstrap records are created:

```bash
docker compose -f backend/docker-compose.yml down -v
docker compose -f backend/docker-compose.yml -f docker-compose.yml up --build
```

## Backend Only

Run only backend services:

```bash
docker compose -f backend/docker-compose.yml up --build
```

Or from inside `backend/`:

```bash
docker compose up --build
```

Backend services include:

- `proofvault-api`
- `proofvault-authserver`
- MySQL
- Anvil
- OpenTelemetry Collector
- Prometheus
- Grafana

More backend details are available in [`backend/README.md`](./backend/README.md).

## Frontend Development

```bash
cd frontend
npm install
npm run dev -- -p 5173
```

Frontend environment variables:

```text
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_AUTH_BASE_URL=http://localhost:9000
NEXT_PUBLIC_AUTH_CLIENT_ID=proofvault-spa
NEXT_PUBLIC_AUTH_REDIRECT_URI=http://localhost:5173/oauth2/callback
NEXT_PUBLIC_AUTH_SCOPES=openid profile email proof:read proof:write
```

The dashboard includes demo fallback data so the UI remains presentable even before the backend is running. Once signed in and connected, it loads live proof, blockchain, and observability data from the API.

## API Overview

Main API endpoints:

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/me` | Current authenticated user |
| `GET` | `/api/proofs` | Recent user proofs |
| `POST` | `/api/proofs/upload` | Upload and anchor a file proof |
| `POST` | `/api/proofs/verify` | Public hash verification |
| `GET` | `/api/proofs/{proofId}/certificate` | Download proof certificate |
| `GET` | `/api/blockchain/status` | Blockchain connection status |
| `GET` | `/api/blockchain/insights` | Proof and chain analytics |
| `GET` | `/api/blockchain/proofs/{fileHash}` | On-chain proof lookup |
| `GET` | `/api/observability/blockchain` | Blockchain OTEL metrics |

Postman collections:

- [`backend/proofvault-api/postman/ProofVault.postman_collection.json`](./backend/proofvault-api/postman/ProofVault.postman_collection.json)
- [`backend/authserver/postman/ProofVaultAuthServer.postman_collection.json`](./backend/authserver/postman/ProofVaultAuthServer.postman_collection.json)

## Authentication

The project includes a dedicated Spring Authorization Server microservice.

Supported auth capabilities:

- OAuth2 authorization code flow with PKCE
- OpenID Connect discovery
- JWT access tokens
- JWKS endpoint
- JDBC-backed registered clients
- JDBC-backed users
- Local bootstrap user and clients
- Profile-specific configuration for local, docker, dev, staging, and prod

The frontend uses the public `proofvault-spa` client. The API validates JWTs issued by the auth server and checks the configured audience `proofvault-api`.

## Blockchain Layer

The smart contract package is in [`blockchain/`](./blockchain).

Highlights:

- Solidity smart contract for immutable proof registry
- OpenZeppelin UUPS upgradeability
- OpenZeppelin role-based access control
- Pausable emergency stop
- Append-only proof records
- Batch anchoring support
- No raw files, filenames, or readable file data stored on-chain

Common commands:

```bash
cd blockchain
npm install
forge install foundry-rs/forge-std
forge build
forge test
```

More details are available in [`blockchain/README.md`](./blockchain/README.md) and [`blockchain/SECURITY.md`](./blockchain/SECURITY.md).

## Observability

The stack includes production-style observability:

- OpenTelemetry tracing from backend services
- Prometheus metrics scraping
- Grafana dashboards for:
  - ProofVault API overview
  - Auth server overview
  - Blockchain and OTEL activity

Grafana dashboards are provisioned from:

```text
backend/docker/grafana/dashboards/
```

## Security Notes

- Raw uploaded files are not stored.
- Only SHA-256 hashes and metadata are persisted.
- The frontend uses PKCE instead of storing a client secret.
- The API validates JWT issuer and audience.
- Smart contract upgrades require a privileged role and paused state.
- Production signing keys should be configured explicitly for the auth server.
- Production blockchain roles should use a multisig or controlled relayer.
