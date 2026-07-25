// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import { Test } from "forge-std/Test.sol";
import { ERC1967Proxy } from "@openzeppelin/contracts/proxy/ERC1967/ERC1967Proxy.sol";
import { ProofVault } from "../src/ProofVault.sol";
import { IProofVault } from "../src/interfaces/IProofVault.sol";

contract ProofVaultV2 is ProofVault {
  function versionV2() external pure returns (string memory) {
    return "2.0.0";
  }
}

contract ProofVaultTest is Test {
  ProofVault private vault;

  address private admin;
  address private anchor;
  address private stranger;

  bytes32 private fileHash;
  bytes32 private metadataHash;

  function setUp() public {
    admin = makeAddr("admin");
    anchor = makeAddr("anchor");
    stranger = makeAddr("stranger");
    fileHash = sha256(bytes("proofvault-demo-file"));
    metadataHash = keccak256(bytes("canonical certificate metadata"));

    ProofVault implementation = new ProofVault();
    bytes memory initData = abi.encodeCall(ProofVault.initialize, (admin, anchor));
    ERC1967Proxy proxy = new ERC1967Proxy(address(implementation), initData);
    vault = ProofVault(address(proxy));
  }

  function testInitializesRolesAndMetadata() public view {
    assertEq(vault.CONTRACT_VERSION(), "1.0.0");
    assertEq(vault.MAX_BATCH_SIZE(), 100);
    assertTrue(vault.hasRole(vault.DEFAULT_ADMIN_ROLE(), admin));
    assertTrue(vault.hasRole(vault.ANCHOR_ROLE(), anchor));
    assertTrue(vault.hasRole(vault.PAUSER_ROLE(), admin));
    assertTrue(vault.hasRole(vault.UPGRADER_ROLE(), admin));
  }

  function testStoreAndVerifyProof() public {
    vm.prank(anchor);
    vault.storeProof(fileHash, metadataHash);

    (bool exists, address submitter, uint64 timestamp, bytes32 storedMetadataHash) =
      vault.verifyProof(fileHash);

    assertTrue(exists);
    assertEq(submitter, anchor);
    assertGt(timestamp, 0);
    assertEq(storedMetadataHash, metadataHash);
    assertEq(vault.totalProofs(), 1);
  }

  function testEmitsProofStoredEvent() public {
    vm.expectEmit(true, true, false, true, address(vault));
    emit IProofVault.ProofStored(fileHash, anchor, uint64(block.timestamp), metadataHash);

    vm.prank(anchor);
    vault.storeProof(fileHash, metadataHash);
  }

  function testOnlyAnchorCanStoreProof() public {
    vm.prank(stranger);
    vm.expectRevert();
    vault.storeProof(fileHash, metadataHash);
  }

  function testCannotInitializeImplementationDirectly() public {
    ProofVault implementation = new ProofVault();

    vm.expectRevert();
    implementation.initialize(admin, anchor);
  }

  function testRejectsZeroAddressInitializerArguments() public {
    ProofVault implementation = new ProofVault();
    bytes memory initData = abi.encodeCall(ProofVault.initialize, (address(0), anchor));

    vm.expectRevert(IProofVault.ZeroAddress.selector);
    new ERC1967Proxy(address(implementation), initData);
  }

  function testCannotStoreDuplicateProof() public {
    vm.startPrank(anchor);
    vault.storeProof(fileHash, metadataHash);

    vm.expectRevert(abi.encodeWithSelector(IProofVault.ProofAlreadyExists.selector, fileHash));
    vault.storeProof(fileHash, metadataHash);
    vm.stopPrank();
  }

  function testRejectsEmptyFileHash() public {
    vm.prank(anchor);
    vm.expectRevert(IProofVault.EmptyFileHash.selector);
    vault.storeProof(bytes32(0), metadataHash);
  }

  function testRejectsEmptyMetadataHash() public {
    vm.prank(anchor);
    vm.expectRevert(IProofVault.EmptyMetadataHash.selector);
    vault.storeProof(fileHash, bytes32(0));
  }

  function testBatchStoresProofs() public {
    bytes32[] memory fileHashes = new bytes32[](2);
    bytes32[] memory metadataHashes = new bytes32[](2);
    fileHashes[0] = sha256(bytes("file-1"));
    fileHashes[1] = sha256(bytes("file-2"));
    metadataHashes[0] = keccak256(bytes("metadata-1"));
    metadataHashes[1] = keccak256(bytes("metadata-2"));

    vm.prank(anchor);
    vault.storeProofs(fileHashes, metadataHashes);

    assertTrue(vault.proofExists(fileHashes[0]));
    assertTrue(vault.proofExists(fileHashes[1]));
    assertEq(vault.totalProofs(), 2);
  }

  function testBatchRejectsMismatchedArrays() public {
    bytes32[] memory fileHashes = new bytes32[](1);
    bytes32[] memory metadataHashes = new bytes32[](2);

    vm.prank(anchor);
    vm.expectRevert(IProofVault.ArrayLengthMismatch.selector);
    vault.storeProofs(fileHashes, metadataHashes);
  }

  function testPauseBlocksAnchoring() public {
    vm.prank(admin);
    vault.pause();

    vm.prank(anchor);
    vm.expectRevert();
    vault.storeProof(fileHash, metadataHash);
  }

  function testUpgradeRequiresPausedContract() public {
    ProofVaultV2 nextImplementation = new ProofVaultV2();

    vm.prank(admin);
    vm.expectRevert(IProofVault.UpgradeRequiresPause.selector);
    vault.upgradeToAndCall(address(nextImplementation), "");
  }

  function testAdminCanUpgradeWhenPaused() public {
    ProofVaultV2 nextImplementation = new ProofVaultV2();

    vm.startPrank(admin);
    vault.pause();
    vault.upgradeToAndCall(address(nextImplementation), "");
    vm.stopPrank();

    assertEq(ProofVaultV2(address(vault)).versionV2(), "2.0.0");
  }

  function testSupportsInterface() public view {
    assertTrue(vault.supportsInterface(type(IProofVault).interfaceId));
  }
}
