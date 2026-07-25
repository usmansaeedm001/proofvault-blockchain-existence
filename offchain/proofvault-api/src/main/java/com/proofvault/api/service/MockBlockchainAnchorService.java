package com.proofvault.api.service;

import com.proofvault.api.dto.BlockchainReceipt;
import com.proofvault.api.dto.BlockchainStatusResponse;
import com.proofvault.api.dto.OnChainProofResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "proofvault.blockchain", name = "mode", havingValue = "mock", matchIfMissing = true)
public class MockBlockchainAnchorService implements IBlockchainAnchorService {
  private final String networkName;
  private final Counter anchorCounter;
  private final Timer anchorTimer;
  private final ObservationRegistry observationRegistry;
  private final ConcurrentMap<String, MockProof> proofs = new ConcurrentHashMap<>();

  public MockBlockchainAnchorService(
    @Value("${proofvault.blockchain.network-name}") String networkName,
    MeterRegistry meterRegistry,
    ObservationRegistry observationRegistry
  ) {
    this.networkName = networkName;
    this.observationRegistry = observationRegistry;
    this.anchorCounter = Counter.builder("proofvault.blockchain.anchors")
      .description("Number of proof anchoring attempts")
      .tag("mode", "mock")
      .tag("network", networkName)
      .register(meterRegistry);
    this.anchorTimer = Timer.builder("proofvault.blockchain.anchor.duration")
      .description("Proof anchoring duration")
      .tag("mode", "mock")
      .tag("network", networkName)
      .register(meterRegistry);
  }

  @Override
  public BlockchainReceipt storeProof(String fileHash, String metadataHash) {
    return Observation.createNotStarted("proofvault.blockchain.store", observationRegistry)
      .lowCardinalityKeyValue("blockchain.mode", "mock")
      .lowCardinalityKeyValue("blockchain.network", networkName)
      .observe(() -> anchorTimer.record(() -> {
        anchorCounter.increment();
        Instant timestamp = Instant.now();
        String normalizedFileHash = normalizeHash(fileHash);
        String transactionHash = "0x" + deterministicTransactionHash(normalizedFileHash + metadataHash, timestamp);
        proofs.putIfAbsent(
          normalizedFileHash,
          new MockProof(transactionHash, normalizeHash(metadataHash), timestamp)
        );
        return new BlockchainReceipt(transactionHash, networkName, timestamp);
      }));
  }

  @Override
  public OnChainProofResponse verifyProof(String fileHash) {
    String normalizedFileHash = normalizeHash(fileHash);
    MockProof proof = proofs.get(normalizedFileHash);
    return new OnChainProofResponse(
      proof != null,
      normalizedFileHash,
      proof == null ? null : "mock-submitter",
      proof == null ? null : proof.timestamp(),
      proof == null ? null : proof.metadataHash(),
      networkName,
      proof == null ? "Mock proof does not exist." : "Mock proof exists."
    );
  }

  @Override
  public BlockchainStatusResponse status() {
    return new BlockchainStatusResponse(
      "mock",
      networkName,
      true,
      null,
      null,
      null,
      null,
      "Mock blockchain anchor is active."
    );
  }

  @Override
  public BigInteger totalProofs() {
    return BigInteger.valueOf(proofs.size());
  }

  private String deterministicTransactionHash(String fileHash, Instant timestamp) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest((fileHash + ":" + timestamp.toEpochMilli()).getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder(bytes.length * 2);
      for (byte value : bytes) {
        builder.append(String.format("%02x", value));
      }
      return builder.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Unable to generate mock transaction hash", exception);
    }
  }

  private String normalizeHash(String hash) {
    String normalized = hash == null ? "" : hash.toLowerCase();
    return normalized.startsWith("0x") ? normalized : "0x" + normalized;
  }

  private record MockProof(String transactionHash, String metadataHash, Instant timestamp) {}
}
