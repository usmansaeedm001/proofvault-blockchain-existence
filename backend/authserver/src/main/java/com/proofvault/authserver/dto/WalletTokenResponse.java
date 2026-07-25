package com.proofvault.authserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WalletTokenResponse(
	@JsonProperty("access_token") String accessToken,
	@JsonProperty("token_type") String tokenType,
	@JsonProperty("expires_in") long expiresIn,
	String scope,
	@JsonProperty("wallet_address") String walletAddress
) {}
