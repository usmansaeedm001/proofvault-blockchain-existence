package com.proofvault.authserver.dto;

import java.time.Instant;

public record WalletNonceResponse(String walletAddress, long chainId, String nonce, String message, Instant expiresAt) {}
