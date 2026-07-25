package com.proofvault.api.controller;

import com.proofvault.api.dto.BlockchainInsightsResponse;
import com.proofvault.api.dto.BlockchainStatusResponse;
import com.proofvault.api.dto.OnChainProofResponse;
import com.proofvault.api.service.BlockchainInsightsService;
import com.proofvault.api.service.CurrentUserService;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/blockchain")
public class BlockchainController {
  private final BlockchainInsightsService blockchainInsightsService;
  private final CurrentUserService currentUserService;

  public BlockchainController(
    BlockchainInsightsService blockchainInsightsService,
    CurrentUserService currentUserService
  ) {
    this.blockchainInsightsService = blockchainInsightsService;
    this.currentUserService = currentUserService;
  }

  @GetMapping("/status")
  public BlockchainStatusResponse status() {
    return blockchainInsightsService.status();
  }

  @GetMapping("/insights")
  public BlockchainInsightsResponse insights(Authentication authentication) {
    return blockchainInsightsService.insights(currentUserService.currentUser(authentication));
  }

  @GetMapping("/proofs/{fileHash}")
  public OnChainProofResponse onChainProof(
    @PathVariable
    @Pattern(regexp = "^(0x)?[a-fA-F0-9]{64}$", message = "fileHash must be a 32-byte hex value")
    String fileHash
  ) {
    return blockchainInsightsService.verifyOnChain(fileHash);
  }
}
