package com.proofvault.api.dto;

import java.time.Instant;

public record VerificationResponse(
  boolean exists,
  String fileHash,
  String transactionHash,
  String network,
  Instant blockchainTimestamp,
  String message
) {}
