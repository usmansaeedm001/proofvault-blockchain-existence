package com.proofvault.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificationRequest(
  @NotBlank
  @Pattern(regexp = "^[a-fA-F0-9]{64}$", message = "fileHash must be a 64-character SHA-256 hash")
  String fileHash
) {}
