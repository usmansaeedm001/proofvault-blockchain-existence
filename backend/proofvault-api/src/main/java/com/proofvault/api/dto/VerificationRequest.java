package com.proofvault.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificationRequest(
  @NotBlank
  @Pattern(regexp = "^(0x)?[a-fA-F0-9]{64}$", message = "fileHash must be a 32-byte hex SHA-256 hash")
  String fileHash
) {}
