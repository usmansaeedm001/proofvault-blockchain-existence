package com.proofvault.authserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record WalletAuthenticationRequest(
	@NotBlank @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "walletAddress must be an Ethereum address") String walletAddress,
	Long chainId,
	@NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{32,128}$", message = "nonce must be a valid challenge nonce") String nonce,
	@NotBlank @Pattern(regexp = "^0x[a-fA-F0-9]{130}$", message = "signature must be a 65-byte Ethereum signature") String signature
) {}
