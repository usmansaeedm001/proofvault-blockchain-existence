#!/usr/bin/env bash
set -euo pipefail

# ProofVault full local cast test runner.
# Run from the blockchain folder while Anvil is running.

export RPC_URL="${RPC_URL:-http://127.0.0.1:8545}"
export PRIVATE_KEY="${PRIVATE_KEY:-0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80}"
export ADMIN="${ADMIN:-0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266}"
export ANCHOR="${ANCHOR:-0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266}"

export STRANGER_PRIVATE_KEY="${STRANGER_PRIVATE_KEY:-0x59c6995e998f97a5a004497e5da58e6e50e097945a453054d3fd6f2876eaa910f}"
export STRANGER="${STRANGER:-0x70997970C51812dc3A010C7d01b50e0d17dc79C8}"

ZERO_BYTES32=0x0000000000000000000000000000000000000000000000000000000000000000

deploy_contract() {
  local target="$1"
  shift

  local output
  output=$(forge create "$target" --rpc-url "$RPC_URL" --private-key "$PRIVATE_KEY" "$@")
  printf "%s\n" "$output" >&2
  printf "%s\n" "$output" | awk '/Deployed to:/ {print $3}' | tail -n 1
}

expect_revert() {
  set +e
  "$@"
  local status=$?
  set -e

  if [ "$status" -eq 0 ]; then
    echo "Expected revert, but command succeeded: $*" >&2
    exit 1
  fi

  echo "Expected revert observed: $*"
}

echo "== ProofVault local test =="
echo "RPC_URL=$RPC_URL"
echo "ADMIN=$ADMIN"
echo "ANCHOR=$ANCHOR"

echo
echo "== Build =="
forge build src/ProofVault.sol

echo
echo "== Deploy implementation =="
IMPLEMENTATION=$(deploy_contract "src/ProofVault.sol:ProofVault")
echo "IMPLEMENTATION=$IMPLEMENTATION"

echo
echo "== Deploy ERC1967 proxy =="
INIT_DATA=$(cast calldata "initialize(address,address)" "$ADMIN" "$ANCHOR")
PROXY=$(deploy_contract \
  "node_modules/@openzeppelin/contracts/proxy/ERC1967/ERC1967Proxy.sol:ERC1967Proxy" \
  "$IMPLEMENTATION" \
  "$INIT_DATA")
echo "PROXY=$PROXY"

echo
echo "== Read constants =="
cast call "$PROXY" "CONTRACT_VERSION()(string)" --rpc-url "$RPC_URL"
cast call "$PROXY" "MAX_BATCH_SIZE()(uint256)" --rpc-url "$RPC_URL"
cast call "$PROXY" "totalProofs()(uint256)" --rpc-url "$RPC_URL"

echo
echo "== Check roles =="
DEFAULT_ADMIN_ROLE=0x0000000000000000000000000000000000000000000000000000000000000000
ANCHOR_ROLE=$(cast keccak "ANCHOR_ROLE")
PAUSER_ROLE=$(cast keccak "PAUSER_ROLE")
UPGRADER_ROLE=$(cast keccak "UPGRADER_ROLE")

cast call "$PROXY" "hasRole(bytes32,address)(bool)" "$DEFAULT_ADMIN_ROLE" "$ADMIN" --rpc-url "$RPC_URL"
cast call "$PROXY" "hasRole(bytes32,address)(bool)" "$ANCHOR_ROLE" "$ANCHOR" --rpc-url "$RPC_URL"
cast call "$PROXY" "hasRole(bytes32,address)(bool)" "$PAUSER_ROLE" "$ADMIN" --rpc-url "$RPC_URL"
cast call "$PROXY" "hasRole(bytes32,address)(bool)" "$UPGRADER_ROLE" "$ADMIN" --rpc-url "$RPC_URL"

echo
echo "== Prepare hashes =="
FILE_HASH=$(cast keccak "proofvault-demo-file")
METADATA_HASH=$(cast keccak "canonical certificate metadata")
FILE_HASH_2=$(cast keccak "proofvault-demo-file-2")
METADATA_HASH_2=$(cast keccak "canonical certificate metadata 2")
FILE_HASH_3=$(cast keccak "proofvault-demo-file-3")
METADATA_HASH_3=$(cast keccak "canonical certificate metadata 3")
UNKNOWN_HASH=$(cast keccak "missing-file")

echo "FILE_HASH=$FILE_HASH"
echo "METADATA_HASH=$METADATA_HASH"

echo
echo "== Store one proof =="
cast send "$PROXY" \
  "storeProof(bytes32,bytes32)" \
  "$FILE_HASH" \
  "$METADATA_HASH" \
  --rpc-url "$RPC_URL" \
  --private-key "$PRIVATE_KEY"

echo
echo "== Verify stored proof =="
cast call "$PROXY" "verifyProof(bytes32)(bool,address,uint64,bytes32)" "$FILE_HASH" --rpc-url "$RPC_URL"
cast call "$PROXY" "proofExists(bytes32)(bool)" "$FILE_HASH" --rpc-url "$RPC_URL"
cast call "$PROXY" "proofOf(bytes32)((address,uint64,bytes32))" "$FILE_HASH" --rpc-url "$RPC_URL"
cast call "$PROXY" "totalProofs()(uint256)" --rpc-url "$RPC_URL"

echo
echo "== Verify unknown proof =="
cast call "$PROXY" "verifyProof(bytes32)(bool,address,uint64,bytes32)" "$UNKNOWN_HASH" --rpc-url "$RPC_URL"
cast call "$PROXY" "proofExists(bytes32)(bool)" "$UNKNOWN_HASH" --rpc-url "$RPC_URL"
expect_revert cast call "$PROXY" "proofOf(bytes32)((address,uint64,bytes32))" "$UNKNOWN_HASH" --rpc-url "$RPC_URL"

echo
echo "== Duplicate and input validation reverts =="
expect_revert cast send "$PROXY" \
  "storeProof(bytes32,bytes32)" \
  "$FILE_HASH" \
  "$METADATA_HASH" \
  --rpc-url "$RPC_URL" \
  --private-key "$PRIVATE_KEY"

expect_revert cast send "$PROXY" \
  "storeProof(bytes32,bytes32)" \
  "$ZERO_BYTES32" \
  "$METADATA_HASH" \
  --rpc-url "$RPC_URL" \
  --private-key "$PRIVATE_KEY"

expect_revert cast send "$PROXY" \
  "storeProof(bytes32,bytes32)" \
  "$FILE_HASH_2" \
  "$ZERO_BYTES32" \
  --rpc-url "$RPC_URL" \
  --private-key "$PRIVATE_KEY"

echo
echo "== Unauthorized anchor revert =="
expect_revert cast send "$PROXY" \
  "storeProof(bytes32,bytes32)" \
  "$(cast keccak "unauthorized-file")" \
  "$(cast keccak "unauthorized-metadata")" \
  --rpc-url "$RPC_URL" \
  --private-key "$STRANGER_PRIVATE_KEY"

echo
echo "== Batch store proofs =="
cast send "$PROXY" \
  "storeProofs(bytes32[],bytes32[])" \
  "[$FILE_HASH_2,$FILE_HASH_3]" \
  "[$METADATA_HASH_2,$METADATA_HASH_3]" \
  --rpc-url "$RPC_URL" \
  --private-key "$PRIVATE_KEY"

cast call "$PROXY" "proofExists(bytes32)(bool)" "$FILE_HASH_2" --rpc-url "$RPC_URL"
cast call "$PROXY" "proofExists(bytes32)(bool)" "$FILE_HASH_3" --rpc-url "$RPC_URL"
cast call "$PROXY" "totalProofs()(uint256)" --rpc-url "$RPC_URL"

echo
echo "== Batch length mismatch revert =="
BAD_FILE_1=$(cast keccak "bad-file-1")
BAD_FILE_2=$(cast keccak "bad-file-2")
BAD_METADATA_1=$(cast keccak "bad-metadata-1")
expect_revert cast send "$PROXY" \
  "storeProofs(bytes32[],bytes32[])" \
  "[$BAD_FILE_1,$BAD_FILE_2]" \
  "[$BAD_METADATA_1]" \
  --rpc-url "$RPC_URL" \
  --private-key "$PRIVATE_KEY"

echo
echo "== Pause blocks anchoring =="
cast send "$PROXY" "pause()" --rpc-url "$RPC_URL" --private-key "$PRIVATE_KEY"
cast call "$PROXY" "paused()(bool)" --rpc-url "$RPC_URL"

expect_revert cast send "$PROXY" \
  "storeProof(bytes32,bytes32)" \
  "$(cast keccak "paused-file")" \
  "$(cast keccak "paused-metadata")" \
  --rpc-url "$RPC_URL" \
  --private-key "$PRIVATE_KEY"

echo
echo "== UUPS upgrade allowed while paused =="
NEW_IMPL=$(deploy_contract "src/ProofVault.sol:ProofVault")
cast send "$PROXY" \
  "upgradeToAndCall(address,bytes)" \
  "$NEW_IMPL" \
  0x \
  --rpc-url "$RPC_URL" \
  --private-key "$PRIVATE_KEY"

echo
echo "== Unpause =="
cast send "$PROXY" "unpause()" --rpc-url "$RPC_URL" --private-key "$PRIVATE_KEY"
cast call "$PROXY" "paused()(bool)" --rpc-url "$RPC_URL"

echo
echo "== UUPS upgrade blocked while unpaused =="
NEW_IMPL_2=$(deploy_contract "src/ProofVault.sol:ProofVault")
expect_revert cast send "$PROXY" \
  "upgradeToAndCall(address,bytes)" \
  "$NEW_IMPL_2" \
  0x \
  --rpc-url "$RPC_URL" \
  --private-key "$PRIVATE_KEY"

echo
echo "== Grant and revoke anchor role =="
cast send "$PROXY" \
  "grantRole(bytes32,address)" \
  "$ANCHOR_ROLE" \
  "$STRANGER" \
  --rpc-url "$RPC_URL" \
  --private-key "$PRIVATE_KEY"

cast call "$PROXY" "hasRole(bytes32,address)(bool)" "$ANCHOR_ROLE" "$STRANGER" --rpc-url "$RPC_URL"

cast send "$PROXY" \
  "storeProof(bytes32,bytes32)" \
  "$(cast keccak "new-anchor-file")" \
  "$(cast keccak "new-anchor-metadata")" \
  --rpc-url "$RPC_URL" \
  --private-key "$STRANGER_PRIVATE_KEY"

cast send "$PROXY" \
  "revokeRole(bytes32,address)" \
  "$ANCHOR_ROLE" \
  "$STRANGER" \
  --rpc-url "$RPC_URL" \
  --private-key "$PRIVATE_KEY"

cast call "$PROXY" "hasRole(bytes32,address)(bool)" "$ANCHOR_ROLE" "$STRANGER" --rpc-url "$RPC_URL"

expect_revert cast send "$PROXY" \
  "storeProof(bytes32,bytes32)" \
  "$(cast keccak "revoked-anchor-file")" \
  "$(cast keccak "revoked-anchor-metadata")" \
  --rpc-url "$RPC_URL" \
  --private-key "$STRANGER_PRIVATE_KEY"

echo
echo "== Final reads =="
cast call "$PROXY" "totalProofs()(uint256)" --rpc-url "$RPC_URL"
cast call "$PROXY" "paused()(bool)" --rpc-url "$RPC_URL"
cast call "$PROXY" "CONTRACT_VERSION()(string)" --rpc-url "$RPC_URL"

echo
echo "All ProofVault local cast checks completed successfully."
