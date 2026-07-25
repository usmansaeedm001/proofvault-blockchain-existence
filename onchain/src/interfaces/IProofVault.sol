// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import { IERC165 } from "@openzeppelin/contracts/utils/introspection/IERC165.sol";

/// @title IProofVault
/// @notice Interface for the ProofVault proof-of-existence registry.
interface IProofVault is IERC165 {
  struct ProofRecord {
    address submitter;
    uint64 timestamp;
    bytes32 metadataHash;
  }

  event ProofStored(
    bytes32 indexed fileHash,
    address indexed submitter,
    uint64 timestamp,
    bytes32 indexed metadataHash
  );

  error EmptyFileHash();
  error EmptyMetadataHash();
  error ArrayLengthMismatch();
  error BatchTooLarge(uint256 length, uint256 maxLength);
  error ProofAlreadyExists(bytes32 fileHash);
  error ProofNotFound(bytes32 fileHash);
  error ZeroAddress();
  error UpgradeRequiresPause();

  function storeProof(bytes32 fileHash, bytes32 metadataHash) external;

  function storeProofs(bytes32[] calldata fileHashes, bytes32[] calldata metadataHashes) external;

  function verifyProof(bytes32 fileHash)
    external
    view
    returns (bool exists, address submitter, uint64 timestamp, bytes32 metadataHash);

  function proofOf(bytes32 fileHash) external view returns (ProofRecord memory proof);

  function proofExists(bytes32 fileHash) external view returns (bool);

  function totalProofs() external view returns (uint256);
}
