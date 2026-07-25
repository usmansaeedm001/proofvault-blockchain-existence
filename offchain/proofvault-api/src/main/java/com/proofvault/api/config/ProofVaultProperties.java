package com.proofvault.api.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "proofvault")
public record ProofVaultProperties(Cors cors, Security security, Blockchain blockchain) {
	public record Cors(List<String> allowedOrigins, List<String> allowedMethods) {}

	public record Security(boolean authenticationEnabled, String audience, String userEmailClaim, String userNameClaim) {}

	public record Blockchain(String mode, String networkName, String explorerBaseUrl, String rpcUrl, String contractAddress, String anchorPrivateKey,
	                         String anchorAddress, long chainId, long gasPriceWei, long gasLimit) {}
}
