package com.proofvault.authserver.service;

import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class WalletMessageFactory {
	public String create(String domain, String uri, String walletAddress, long chainId, String nonce, Instant issuedAt, Instant expiresAt) {
		return domain + " wants you to sign in with your Ethereum account:\n"
			+ walletAddress + "\n\n"
			+ "Register and authenticate with ProofVault using this wallet address. No blockchain transaction or gas fee is required.\n\n"
			+ "URI: " + uri + "\n"
			+ "Version: 1\n"
			+ "Chain ID: " + chainId + "\n"
			+ "Nonce: " + nonce + "\n"
			+ "Issued At: " + issuedAt + "\n"
			+ "Expiration Time: " + expiresAt;
	}
}
