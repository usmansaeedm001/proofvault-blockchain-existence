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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlockchainInsightsService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BlockchainInsightsService.class);
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
    LOGGER.debug("Blockchain insights requested user={}", safeUser(owner));
    return Observation.createNotStarted("proofvault.blockchain.insights", observationRegistry)
      .observe(() -> {
        BlockchainStatusResponse status = blockchainAnchorService.status();
        BigInteger onChainTotal = safeTotalProofs();
        long offChainTotal = proofRepository.count();
        long offChainUserProofs = proofRepository.countByOwnerAndCreatedAtAfter(owner, Instant.EPOCH);
        LOGGER.info("Blockchain insights loaded user={} mode={} network={} connected={} onChainTotal={} offChainTotal={} offChainUserProofs={}",
          safeUser(owner), status.mode(), status.network(), status.connected(), onChainTotal, offChainTotal, offChainUserProofs);
        return new BlockchainInsightsResponse(status, onChainTotal, offChainTotal, offChainUserProofs);
      });
  }

  public BlockchainStatusResponse status() {
    LOGGER.debug("Blockchain status requested");
    BlockchainStatusResponse status = blockchainAnchorService.status();
    LOGGER.info("Blockchain status loaded mode={} network={} connected={} chainId={} latestBlock={}", status.mode(), status.network(), status.connected(),
      status.chainId(), status.latestBlockNumber());
    return status;
  }

  public OnChainProofResponse verifyOnChain(String fileHash) {
    LOGGER.info("On-chain proof lookup requested fileHash={}", shortHash(fileHash));
    OnChainProofResponse response = blockchainAnchorService.verifyProof(fileHash);
    LOGGER.info("On-chain proof lookup completed fileHash={} exists={} network={}", shortHash(fileHash), response.exists(), response.network());
    return response;
  }

  private BigInteger safeTotalProofs() {
    try {
      return blockchainAnchorService.totalProofs();
    } catch (RuntimeException exception) {
      LOGGER.warn("Unable to load on-chain proof total reason={}", exception.getMessage());
      return null;
    }
  }

  private String safeUser(User owner) {
    return owner == null || owner.getPublicId() == null ? "unknown" : owner.getPublicId();
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
