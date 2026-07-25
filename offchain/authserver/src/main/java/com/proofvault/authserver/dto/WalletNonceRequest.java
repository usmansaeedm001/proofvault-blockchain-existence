package com.proofvault.authserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record WalletNonceRequest(
	@NotBlank @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "walletAddress must be an Ethereum address") String walletAddress,
	Long chainId
) {}
