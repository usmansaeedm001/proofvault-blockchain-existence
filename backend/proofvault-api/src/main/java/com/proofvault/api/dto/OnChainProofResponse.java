package com.proofvault.api.dto;

import java.time.Instant;

public record OnChainProofResponse(
  boolean exists,
  String fileHash,
  String submitter,
  Instant timestamp,
  String metadataHash,
  String network,
  String message
) {}
