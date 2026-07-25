# ProofVault Cast Test Commands

These commands are for a local Anvil chain.

Your provided local Anvil account:

```bash
export RPC_URL=http://127.0.0.1:8545
export PRIVATE_KEY=0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80
export ADMIN=0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266
export ANCHOR=0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266
```

## 1. Start Anvil

```bash
anvil
```

## 2. Build Contracts

If you want to use `script/DeployProofVault.s.sol`, install `forge-std` first:

```bash
forge install foundry-rs/forge-std
```

The deploy script imports `forge-std/Script.sol`; without that dependency you will see:

```text
Source "lib/forge-std/src/Script.sol" not found
```

You can skip `forge-std` entirely by using the cast-only runner:

```bash
bash CAST_FULL_LOCAL_TEST.sh
```

For the no-`forge-std` path, build only the contract source:

```bash
forge build src/ProofVault.sol
```

## 3. Deploy UUPS Proxy

```bash
export PROOFVAULT_ADMIN=${PROOFVAULT_ADMIN:-$ADMIN}
export PROOFVAULT_ANCHOR=${PROOFVAULT_ANCHOR:-$ANCHOR}

forge script script/DeployProofVault.s.sol:DeployProofVault \
  --rpc-url $RPC_URL \
  --private-key $PRIVATE_KEY \
  --broadcast
```

Copy the deployed proxy address from the broadcast output, then set:

```bash
export PROXY=0xYourProxyAddress
```

## 4. Read Version And Constants

```bash
cast call $PROXY "CONTRACT_VERSION()(string)" --rpc-url $RPC_URL
cast call $PROXY "MAX_BATCH_SIZE()(uint256)" --rpc-url $RPC_URL
cast call $PROXY "totalProofs()(uint256)" --rpc-url $RPC_URL
```

## 5. Check Roles

```bash
export DEFAULT_ADMIN_ROLE=0x0000000000000000000000000000000000000000000000000000000000000000
export ANCHOR_ROLE=$(cast keccak "ANCHOR_ROLE")
export PAUSER_ROLE=$(cast keccak "PAUSER_ROLE")
export UPGRADER_ROLE=$(cast keccak "UPGRADER_ROLE")

cast call $PROXY "hasRole(bytes32,address)(bool)" $DEFAULT_ADMIN_ROLE $ADMIN --rpc-url $RPC_URL
cast call $PROXY "hasRole(bytes32,address)(bool)" $ANCHOR_ROLE $ANCHOR --rpc-url $RPC_URL
cast call $PROXY "hasRole(bytes32,address)(bool)" $PAUSER_ROLE $ADMIN --rpc-url $RPC_URL
cast call $PROXY "hasRole(bytes32,address)(bool)" $UPGRADER_ROLE $ADMIN --rpc-url $RPC_URL
```

## 6. Prepare Proof Hashes

```bash
export FILE_HASH=$(cast keccak "proofvault-demo-file")
export METADATA_HASH=$(cast keccak "canonical certificate metadata")

export FILE_HASH_2=$(cast keccak "proofvault-demo-file-2")
export METADATA_HASH_2=$(cast keccak "canonical certificate metadata 2")

export FILE_HASH_3=$(cast keccak "proofvault-demo-file-3")
export METADATA_HASH_3=$(cast keccak "canonical certificate metadata 3")
```

## 7. Store One Proof

```bash
cast send $PROXY \
  "storeProof(bytes32,bytes32)" \
  $FILE_HASH \
  $METADATA_HASH \
  --rpc-url $RPC_URL \
  --private-key $PRIVATE_KEY
```

## 8. Verify One Proof

```bash
cast call $PROXY \
  "verifyProof(bytes32)(bool,address,uint64,bytes32)" \
  $FILE_HASH \
  --rpc-url $RPC_URL

cast call $PROXY \
  "proofExists(bytes32)(bool)" \
  $FILE_HASH \
  --rpc-url $RPC_URL

cast call $PROXY \
  "proofOf(bytes32)((address,uint64,bytes32))" \
  $FILE_HASH \
  --rpc-url $RPC_URL

cast call $PROXY "totalProofs()(uint256)" --rpc-url $RPC_URL
```

## 9. Verify Unknown Proof

```bash
export UNKNOWN_HASH=$(cast keccak "missing-file")

cast call $PROXY \
  "verifyProof(bytes32)(bool,address,uint64,bytes32)" \
  $UNKNOWN_HASH \
  --rpc-url $RPC_URL

cast call $PROXY \
  "proofExists(bytes32)(bool)" \
  $UNKNOWN_HASH \
  --rpc-url $RPC_URL
```

This command should revert with `ProofNotFound`:

```bash
cast call $PROXY \
  "proofOf(bytes32)((address,uint64,bytes32))" \
  $UNKNOWN_HASH \
  --rpc-url $RPC_URL
```

## 10. Duplicate Proof Rejection

This command should revert with `ProofAlreadyExists`:

```bash
cast send $PROXY \
  "storeProof(bytes32,bytes32)" \
  $FILE_HASH \
  $METADATA_HASH \
  --rpc-url $RPC_URL \
  --private-key $PRIVATE_KEY
```

## 11. Empty Input Rejections

These commands should revert:

```bash
cast send $PROXY \
  "storeProof(bytes32,bytes32)" \
  0x0000000000000000000000000000000000000000000000000000000000000000 \
  $METADATA_HASH \
  --rpc-url $RPC_URL \
  --private-key $PRIVATE_KEY

cast send $PROXY \
  "storeProof(bytes32,bytes32)" \
  $FILE_HASH_2 \
  0x0000000000000000000000000000000000000000000000000000000000000000 \
  --rpc-url $RPC_URL \
  --private-key $PRIVATE_KEY
```

## 12. Batch Store Proofs

```bash
cast send $PROXY \
  "storeProofs(bytes32[],bytes32[])" \
  "[$FILE_HASH_2,$FILE_HASH_3]" \
  "[$METADATA_HASH_2,$METADATA_HASH_3]" \
  --rpc-url $RPC_URL \
  --private-key $PRIVATE_KEY

cast call $PROXY "proofExists(bytes32)(bool)" $FILE_HASH_2 --rpc-url $RPC_URL
cast call $PROXY "proofExists(bytes32)(bool)" $FILE_HASH_3 --rpc-url $RPC_URL
cast call $PROXY "totalProofs()(uint256)" --rpc-url $RPC_URL
```

## 13. Batch Array Length Rejection

This command should revert with `ArrayLengthMismatch`:

```bash
cast send $PROXY \
  "storeProofs(bytes32[],bytes32[])" \
  "[$(cast keccak "bad-file-1"),$(cast keccak "bad-file-2")]" \
  "[$(cast keccak "bad-metadata-1")]" \
  --rpc-url $RPC_URL \
  --private-key $PRIVATE_KEY
```

## 14. Pause Contract

```bash
cast send $PROXY \
  "pause()" \
  --rpc-url $RPC_URL \
  --private-key $PRIVATE_KEY

cast call $PROXY "paused()(bool)" --rpc-url $RPC_URL
```

## 15. Confirm Store Is Blocked While Paused

This command should revert:

```bash
cast send $PROXY \
  "storeProof(bytes32,bytes32)" \
  $(cast keccak "paused-file") \
  $(cast keccak "paused-metadata") \
  --rpc-url $RPC_URL \
  --private-key $PRIVATE_KEY
```

## 16. Upgrade Authorization Requires Pause

Deploy a new implementation:

```bash
export NEW_IMPL=$(forge create src/ProofVault.sol:ProofVault \
  --rpc-url $RPC_URL \
  --private-key $PRIVATE_KEY \
  --json | jq -r ".deployedTo")
```

Because the contract is currently paused, this upgrade authorization path should be allowed:

```bash
cast send $PROXY \
  "upgradeToAndCall(address,bytes)" \
  $NEW_IMPL \
  0x \
  --rpc-url $RPC_URL \
  --private-key $PRIVATE_KEY
```

## 17. Unpause Contract

```bash
cast send $PROXY \
  "unpause()" \
  --rpc-url $RPC_URL \
  --private-key $PRIVATE_KEY

cast call $PROXY "paused()(bool)" --rpc-url $RPC_URL
```

## 18. Confirm Upgrade Is Blocked When Unpaused

Deploy another implementation:

```bash
export NEW_IMPL_2=$(forge create src/ProofVault.sol:ProofVault \
  --rpc-url $RPC_URL \
  --private-key $PRIVATE_KEY \
  --json | jq -r ".deployedTo")
```

This command should revert with `UpgradeRequiresPause`:

```bash
cast send $PROXY \
  "upgradeToAndCall(address,bytes)" \
  $NEW_IMPL_2 \
  0x \
  --rpc-url $RPC_URL \
  --private-key $PRIVATE_KEY
```

## 19. Grant And Revoke Anchor Role

```bash
export NEW_ANCHOR=0x70997970C51812dc3A010C7d01b50e0d17dc79C8
export NEW_ANCHOR_PRIVATE_KEY=0x59c6995e998f97a5a004497e5da58e6e50e097945a453054d3fd6f2876eaa910f

cast send $PROXY \
  "grantRole(bytes32,address)" \
  $ANCHOR_ROLE \
  $NEW_ANCHOR \
  --rpc-url $RPC_URL \
  --private-key $PRIVATE_KEY

cast call $PROXY "hasRole(bytes32,address)(bool)" $ANCHOR_ROLE $NEW_ANCHOR --rpc-url $RPC_URL

cast send $PROXY \
  "storeProof(bytes32,bytes32)" \
  $(cast keccak "new-anchor-file") \
  $(cast keccak "new-anchor-metadata") \
  --rpc-url $RPC_URL \
  --private-key $NEW_ANCHOR_PRIVATE_KEY

cast send $PROXY \
  "revokeRole(bytes32,address)" \
  $ANCHOR_ROLE \
  $NEW_ANCHOR \
  --rpc-url $RPC_URL \
  --private-key $PRIVATE_KEY

cast call $PROXY "hasRole(bytes32,address)(bool)" $ANCHOR_ROLE $NEW_ANCHOR --rpc-url $RPC_URL
```

## 20. Final State Reads

```bash
cast call $PROXY "totalProofs()(uint256)" --rpc-url $RPC_URL
cast call $PROXY "paused()(bool)" --rpc-url $RPC_URL
cast call $PROXY "CONTRACT_VERSION()(string)" --rpc-url $RPC_URL
```
