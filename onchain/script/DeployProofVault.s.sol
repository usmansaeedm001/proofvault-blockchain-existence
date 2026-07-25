// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import { Script } from "forge-std/Script.sol";
import { ERC1967Proxy } from "@openzeppelin/contracts/proxy/ERC1967/ERC1967Proxy.sol";
import { ProofVault } from "../src/ProofVault.sol";

contract DeployProofVault is Script {
  function run() external returns (address proxy, address implementation) {
    uint256 deployerPrivateKey = vm.envUint("PRIVATE_KEY");
    address deployer = vm.addr(deployerPrivateKey);
    address admin = vm.envOr("PROOFVAULT_ADMIN", deployer);
    address anchor = vm.envOr("PROOFVAULT_ANCHOR", deployer);

    vm.startBroadcast(deployerPrivateKey);
    ProofVault proofVaultImplementation = new ProofVault();
    bytes memory initData = abi.encodeCall(ProofVault.initialize, (admin, anchor));
    ERC1967Proxy proofVaultProxy = new ERC1967Proxy(address(proofVaultImplementation), initData);
    vm.stopBroadcast();

    proxy = address(proofVaultProxy);
    implementation = address(proofVaultImplementation);
  }
}
