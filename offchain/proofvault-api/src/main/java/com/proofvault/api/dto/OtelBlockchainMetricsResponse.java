package com.proofvault.api.dto;

public record OtelBlockchainMetricsResponse(
  double anchors,
  double verifications,
  double errors,
  double anchorDurationCount,
  double anchorDurationTotalSeconds,
  double verifyDurationCount,
  double verifyDurationTotalSeconds
) {}
