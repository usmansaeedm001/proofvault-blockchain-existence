package com.proofvault.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class HashingService {
  public String sha256(MultipartFile file) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      try (InputStream inputStream = file.getInputStream();
           DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
        byte[] buffer = new byte[8192];
        while (digestInputStream.read(buffer) != -1) {
          // DigestInputStream updates the digest as bytes are consumed.
        }
      }

      return toHex(digest.digest());
    } catch (IOException | NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Unable to hash uploaded file", exception);
    }
  }

  public String sha256Text(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return toHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Unable to hash text value", exception);
    }
  }

  private String toHex(byte[] bytes) {
    StringBuilder builder = new StringBuilder(bytes.length * 2);
    for (byte value : bytes) {
      builder.append(String.format("%02x", value));
    }
    return builder.toString();
  }
}
