package com.proofvault.api.service;

import com.proofvault.api.model.Proof;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CertificateService {
  private static final Logger LOGGER = LoggerFactory.getLogger(CertificateService.class);

  public byte[] buildCertificate(Proof proof) {
    LOGGER.info("Certificate generation requested proofId={} fileHash={} network={}", proof.getPublicId(), shortHash(proof.getFileHash()), proof.getNetwork());
    String certificate = """
      ProofVault Certificate of Existence

      File name: %s
      SHA-256 hash: %s
      File size: %d bytes
      Network: %s
      Transaction: %s
      Blockchain timestamp: %s
      Certificate created: %s

      ProofVault stores only cryptographic fingerprints and metadata, not the original file.
      """.formatted(
      proof.getFileName(),
      proof.getFileHash(),
      proof.getFileSize(),
      proof.getNetwork(),
      proof.getTransactionHash(),
      proof.getBlockchainTimestamp(),
      proof.getCreatedAt()
    );

    byte[] bytes = certificate.getBytes(StandardCharsets.UTF_8);
    LOGGER.debug("Certificate generated proofId={} sizeBytes={}", proof.getPublicId(), bytes.length);
    return bytes;
  }

  private String shortHash(String hash) {
    if (hash == null || hash.isBlank()) {
      return "none";
    }
    String normalized = hash.startsWith("0x") ? hash.substring(2) : hash;
    if (normalized.length() <= 12) {
      return normalized;
    }
    return normalized.substring(0, 6) + "..." + normalized.substring(normalized.length() - 6);
  }
}
