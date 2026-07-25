package com.proofvault.api.dto;

import com.proofvault.api.model.Proof;
import java.time.Instant;

public record ProofResponse(
  String id,
  String fileName,
  String fileHash,
  long fileSize,
  String transactionHash,
  String network,
  Instant blockchainTimestamp,
  Instant createdAt
) {
  public static ProofResponse from(Proof proof) {
    return new ProofResponse(
      proof.getPublicId(),
      proof.getFileName(),
      proof.getFileHash(),
      proof.getFileSize(),
      proof.getTransactionHash(),
      proof.getNetwork(),
      proof.getBlockchainTimestamp(),
      proof.getCreatedAt()
    );
  }
}
