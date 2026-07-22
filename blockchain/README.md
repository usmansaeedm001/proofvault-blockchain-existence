# ProofVault Smart Contracts

Production-oriented Foundry package for ProofVault's hash-only proof-of-existence registry.

## Architecture

- `src/ProofVault.sol`: UUPS upgradeable proof registry.
- `src/interfaces/IProofVault.sol`: external interface, events, and custom errors.
- `script/DeployProofVault.s.sol`: deploys implementation plus ERC-1967 proxy.
- `test/ProofVault.t.sol`: role, pause, duplicate, batch, and UUPS upgrade coverage.

## Security Posture

- OpenZeppelin UUPS upgradeability.
- OpenZeppelin `AccessControlUpgradeable` roles.
- OpenZeppelin `PausableUpgradeable` emergency stop.
- Constructor disables implementation initialization.
- Upgrades require `UPGRADER_ROLE` and a paused proxy.
- Raw files, filenames, and user-readable certificate data are not stored on-chain.
- Proof records are append-only: no update and no delete path.
- Batch anchoring is capped by `MAX_BATCH_SIZE`.
- Anchoring paths do not perform external calls, keeping reentrancy risk out of the execution path.

## Roles

- `DEFAULT_ADMIN_ROLE`: grants and revokes roles.
- `ANCHOR_ROLE`: stores proofs. Use a backend-controlled hot wallet or relayer.
- `PAUSER_ROLE`: pauses and unpauses anchoring.
- `UPGRADER_ROLE`: authorizes UUPS upgrades while paused.

## Install Dependencies

Install pinned OpenZeppelin and compiler packages:

```bash
npm install
```

Install Foundry test helpers:

```bash
forge install foundry-rs/forge-std
```

For a local compiler sanity check without Foundry:

```bash
npm run compile
```

## Build And Test

```bash
forge fmt
forge build
forge test
forge test --profile ci
```

## Deploy

```bash
set PROOFVAULT_ADMIN=0xYourAdminMultisig
set PROOFVAULT_ANCHOR=0xYourBackendAnchorWallet

forge script script/DeployProofVault.s.sol:DeployProofVault ^
  --rpc-url <RPC_URL> ^
  --broadcast ^
  --verify
```

## Audit Notes

Before audit, freeze:

- exact OpenZeppelin versions in `package-lock.json`;
- deployment chain and compiler settings;
- role ownership model, ideally a multisig/timelock for admin and upgrader roles;
- off-chain metadata canonicalization rules for `metadataHash`.

This code is prepared for audit review, but it is not a substitute for a completed independent audit.
