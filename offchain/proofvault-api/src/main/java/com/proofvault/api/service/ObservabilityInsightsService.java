package com.proofvault.api.service;

import com.proofvault.api.dto.OtelBlockchainMetricsResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class ObservabilityInsightsService {
  private final MeterRegistry meterRegistry;

  public ObservabilityInsightsService(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public OtelBlockchainMetricsResponse blockchainMetrics() {
    Timer anchorTimer = meterRegistry.find("proofvault.blockchain.anchor.duration").timer();
    Timer verifyTimer = meterRegistry.find("proofvault.blockchain.verify.duration").timer();

    return new OtelBlockchainMetricsResponse(
      counter("proofvault.blockchain.anchors"),
      counter("proofvault.blockchain.verifications"),
      counter("proofvault.blockchain.errors"),
      anchorTimer == null ? 0 : anchorTimer.count(),
      anchorTimer == null ? 0 : anchorTimer.totalTime(java.util.concurrent.TimeUnit.SECONDS),
      verifyTimer == null ? 0 : verifyTimer.count(),
      verifyTimer == null ? 0 : verifyTimer.totalTime(java.util.concurrent.TimeUnit.SECONDS)
    );
  }

  private double counter(String name) {
    var counter = meterRegistry.find(name).counter();
    return counter == null ? 0 : counter.count();
  }
}
