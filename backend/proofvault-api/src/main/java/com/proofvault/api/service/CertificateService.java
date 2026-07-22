package com.proofvault.api.service;

import com.proofvault.api.model.Proof;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

@Service
public class CertificateService {
  public byte[] buildCertificate(Proof proof) {
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

    return certificate.getBytes(StandardCharsets.UTF_8);
  }
}
