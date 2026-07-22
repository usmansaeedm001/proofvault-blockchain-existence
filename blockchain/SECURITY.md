# ProofVault Contract Security Notes

## In Scope

- `src/ProofVault.sol`
- `src/interfaces/IProofVault.sol`
- UUPS proxy deployment flow in `script/DeployProofVault.s.sol`

## Trust Model

ProofVault is a SaaS-oriented registry. End users do not write directly to the contract by default. A backend or relayer wallet with `ANCHOR_ROLE` anchors file hashes and certificate metadata hashes.

Recommended production role holders:

- `DEFAULT_ADMIN_ROLE`: multisig or timelock-controlled multisig.
- `UPGRADER_ROLE`: same multisig/timelock path as admin.
- `PAUSER_ROLE`: multisig plus limited emergency responder if your incident process supports it.
- `ANCHOR_ROLE`: backend relayer wallet with low operational balance and monitoring.

## Upgrade Policy

The contract uses UUPS. Upgrades are only authorized when:

- caller has `UPGRADER_ROLE`;
- the proxy is paused.

Before each upgrade:

1. Pause anchoring.
2. Verify the new implementation storage layout.
3. Execute the upgrade from the authorized upgrade owner.
4. Smoke-test reads and one staging proof.
5. Unpause anchoring.

## On-Chain Data Policy

The contract stores:

- file hash;
- submitter address;
- block timestamp;
- metadata hash.

The contract must not store raw files, filenames, email addresses, legal names, or readable certificate JSON.

## Known Assumptions

- `metadataHash` is produced from a canonical off-chain certificate metadata document.
- Timestamp comes from the block producer and should be treated as blockchain timestamp evidence, not wall-clock precision.
- The SaaS backend handles customer identity, quota checks, billing, and certificate rendering off-chain.
