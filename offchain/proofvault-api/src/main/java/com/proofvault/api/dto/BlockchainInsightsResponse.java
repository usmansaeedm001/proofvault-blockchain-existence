package com.proofvault.api.dto;

import java.math.BigInteger;

public record BlockchainInsightsResponse(
  BlockchainStatusResponse status,
  BigInteger onChainTotalProofs,
  long offChainTotalProofs,
  long offChainUserProofs
) {}
