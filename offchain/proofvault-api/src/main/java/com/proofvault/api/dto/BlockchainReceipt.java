package com.proofvault.api.dto;

import java.time.Instant;

public record BlockchainReceipt(
  String transactionHash,
  String network,
  Instant timestamp
) {}
