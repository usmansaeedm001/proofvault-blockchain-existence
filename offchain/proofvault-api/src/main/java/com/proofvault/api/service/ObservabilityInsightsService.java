package com.proofvault.api.service;

import com.proofvault.api.dto.OtelBlockchainMetricsResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ObservabilityInsightsService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ObservabilityInsightsService.class);
  private final MeterRegistry meterRegistry;

  public ObservabilityInsightsService(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public OtelBlockchainMetricsResponse blockchainMetrics() {
    LOGGER.debug("Blockchain OTEL metrics snapshot requested");
    Timer anchorTimer = meterRegistry.find("proofvault.blockchain.anchor.duration").timer();
    Timer verifyTimer = meterRegistry.find("proofvault.blockchain.verify.duration").timer();

    OtelBlockchainMetricsResponse response = new OtelBlockchainMetricsResponse(
      counter("proofvault.blockchain.anchors"),
      counter("proofvault.blockchain.verifications"),
      counter("proofvault.blockchain.errors"),
      anchorTimer == null ? 0 : anchorTimer.count(),
      anchorTimer == null ? 0 : anchorTimer.totalTime(java.util.concurrent.TimeUnit.SECONDS),
      verifyTimer == null ? 0 : verifyTimer.count(),
      verifyTimer == null ? 0 : verifyTimer.totalTime(java.util.concurrent.TimeUnit.SECONDS)
    );
    LOGGER.info("Blockchain OTEL metrics snapshot loaded anchors={} verifications={} errors={}", response.anchors(), response.verifications(), response.errors());
    return response;
  }

  private double counter(String name) {
    var counter = meterRegistry.find(name).counter();
    return counter == null ? 0 : counter.count();
  }
}
