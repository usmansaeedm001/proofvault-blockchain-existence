package com.proofvault.api.service;

import com.proofvault.api.dto.BlockchainInsightsResponse;
import com.proofvault.api.dto.BlockchainStatusResponse;
import com.proofvault.api.dto.OnChainProofResponse;
import com.proofvault.api.model.User;
import com.proofvault.api.repository.ProofRepository;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.math.BigInteger;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlockchainInsightsService {
  private final IBlockchainAnchorService blockchainAnchorService;
  private final ProofRepository proofRepository;
  private final ObservationRegistry observationRegistry;

  public BlockchainInsightsService(
    IBlockchainAnchorService blockchainAnchorService,
    ProofRepository proofRepository,
    ObservationRegistry observationRegistry
  ) {
    this.blockchainAnchorService = blockchainAnchorService;
    this.proofRepository = proofRepository;
    this.observationRegistry = observationRegistry;
  }

  @Transactional(readOnly = true)
  public BlockchainInsightsResponse insights(User owner) {
    return Observation.createNotStarted("proofvault.blockchain.insights", observationRegistry)
      .observe(() -> new BlockchainInsightsResponse(
        blockchainAnchorService.status(),
        safeTotalProofs(),
        proofRepository.count(),
        proofRepository.countByOwnerAndCreatedAtAfter(owner, Instant.EPOCH)
      ));
  }

  public BlockchainStatusResponse status() {
    return blockchainAnchorService.status();
  }

  public OnChainProofResponse verifyOnChain(String fileHash) {
    return blockchainAnchorService.verifyProof(fileHash);
  }

  private BigInteger safeTotalProofs() {
    try {
      return blockchainAnchorService.totalProofs();
    } catch (RuntimeException exception) {
      return null;
    }
  }
}
