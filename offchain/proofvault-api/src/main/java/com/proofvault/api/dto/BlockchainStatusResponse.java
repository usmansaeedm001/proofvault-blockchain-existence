package com.proofvault.api.dto;

import java.math.BigInteger;

public record BlockchainStatusResponse(
  String mode,
  String network,
  boolean connected,
  BigInteger chainId,
  BigInteger latestBlockNumber,
  String contractAddress,
  String anchorAddress,
  String message
) {}
