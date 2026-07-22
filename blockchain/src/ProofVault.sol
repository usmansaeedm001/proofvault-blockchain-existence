// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import { AccessControlUpgradeable } from
  "@openzeppelin/contracts-upgradeable/access/AccessControlUpgradeable.sol";
import { Initializable } from
  "@openzeppelin/contracts-upgradeable/proxy/utils/Initializable.sol";
import { UUPSUpgradeable } from
  "@openzeppelin/contracts-upgradeable/proxy/utils/UUPSUpgradeable.sol";
import { PausableUpgradeable } from
  "@openzeppelin/contracts-upgradeable/utils/PausableUpgradeable.sol";
import { IProofVault } from "./interfaces/IProofVault.sol";
import { IERC165 } from "@openzeppelin/contracts/utils/introspection/IERC165.sol";

/// @title ProofVault
/// @notice Upgradeable hash-only proof-of-existence registry.
/// @dev Uses UUPS. Upgrades are restricted to UPGRADER_ROLE and require the contract to be paused.
contract ProofVault is
  Initializable,
  AccessControlUpgradeable,
  PausableUpgradeable,
  UUPSUpgradeable,
  IProofVault
{
  bytes32 public constant ANCHOR_ROLE = keccak256("ANCHOR_ROLE");
  bytes32 public constant PAUSER_ROLE = keccak256("PAUSER_ROLE");
  bytes32 public constant UPGRADER_ROLE = keccak256("UPGRADER_ROLE");

  uint256 public constant MAX_BATCH_SIZE = 100;
  string public constant CONTRACT_VERSION = "1.0.0";

  mapping(bytes32 fileHash => ProofRecord proof) private _proofs;
  uint256 private _totalProofs;

  /// @custom:oz-upgrades-unsafe-allow constructor
  constructor() {
    _disableInitializers();
  }

  function initialize(address admin, address initialAnchor) external initializer {
    if (admin == address(0) || initialAnchor == address(0)) {
      revert ZeroAddress();
    }

    __AccessControl_init();
    __Pausable_init();

    _grantRole(DEFAULT_ADMIN_ROLE, admin);
    _grantRole(PAUSER_ROLE, admin);
    _grantRole(UPGRADER_ROLE, admin);
    _grantRole(ANCHOR_ROLE, admin);
    _grantRole(ANCHOR_ROLE, initialAnchor);
  }

  /// @notice Pauses new proof anchoring during incident response or before upgrades.
  function pause() external onlyRole(PAUSER_ROLE) {
    _pause();
  }

  /// @notice Resumes proof anchoring after an incident response or completed upgrade.
  function unpause() external onlyRole(PAUSER_ROLE) {
    _unpause();
  }

  /// @notice Anchors one file hash and certificate metadata hash.
  /// @dev `metadataHash` should be the keccak256/SHA-256 digest of canonical off-chain metadata.
  function storeProof(bytes32 fileHash, bytes32 metadataHash)
    external
    onlyRole(ANCHOR_ROLE)
    whenNotPaused
  {
    _storeProof(fileHash, metadataHash, _msgSender());
  }

  /// @notice Anchors multiple proofs in one transaction.
  function storeProofs(bytes32[] calldata fileHashes, bytes32[] calldata metadataHashes)
    external
    onlyRole(ANCHOR_ROLE)
    whenNotPaused
  {
    uint256 length = fileHashes.length;
    if (length != metadataHashes.length) {
      revert ArrayLengthMismatch();
    }
    if (length == 0 || length > MAX_BATCH_SIZE) {
      revert BatchTooLarge(length, MAX_BATCH_SIZE);
    }

    address submitter = _msgSender();
    for (uint256 index = 0; index < length; ++index) {
      _storeProof(fileHashes[index], metadataHashes[index], submitter);
    }
  }

  function verifyProof(bytes32 fileHash)
    external
    view
    returns (bool exists, address submitter, uint64 timestamp, bytes32 metadataHash)
  {
    ProofRecord memory proof = _proofs[fileHash];
    exists = proof.timestamp != 0;
    submitter = proof.submitter;
    timestamp = proof.timestamp;
    metadataHash = proof.metadataHash;
  }

  function proofOf(bytes32 fileHash) external view returns (ProofRecord memory proof) {
    proof = _proofs[fileHash];
    if (proof.timestamp == 0) {
      revert ProofNotFound(fileHash);
    }
  }

  function proofExists(bytes32 fileHash) external view returns (bool) {
    return _proofs[fileHash].timestamp != 0;
  }

  function totalProofs() external view returns (uint256) {
    return _totalProofs;
  }

  function supportsInterface(bytes4 interfaceId)
    public
    view
    override(AccessControlUpgradeable, IERC165)
    returns (bool)
  {
    return interfaceId == type(IProofVault).interfaceId || super.supportsInterface(interfaceId);
  }

  function _authorizeUpgrade(address) internal view override onlyRole(UPGRADER_ROLE) {
    if (!paused()) {
      revert UpgradeRequiresPause();
    }
  }

  function _storeProof(bytes32 fileHash, bytes32 metadataHash, address submitter) private {
    if (fileHash == bytes32(0)) {
      revert EmptyFileHash();
    }
    if (metadataHash == bytes32(0)) {
      revert EmptyMetadataHash();
    }
    if (_proofs[fileHash].timestamp != 0) {
      revert ProofAlreadyExists(fileHash);
    }

    uint64 timestamp = uint64(block.timestamp);
    _proofs[fileHash] =
      ProofRecord({ submitter: submitter, timestamp: timestamp, metadataHash: metadataHash });

    unchecked {
      ++_totalProofs;
    }

    emit ProofStored(fileHash, submitter, timestamp, metadataHash);
  }

  uint256[49] private __gap;
}
