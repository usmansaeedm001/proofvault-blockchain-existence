package com.proofvault.api.controller;

import com.proofvault.api.dto.OtelBlockchainMetricsResponse;
import com.proofvault.api.service.ObservabilityInsightsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/observability")
public class ObservabilityController {
  private final ObservabilityInsightsService observabilityInsightsService;

  public ObservabilityController(ObservabilityInsightsService observabilityInsightsService) {
    this.observabilityInsightsService = observabilityInsightsService;
  }

  @GetMapping("/blockchain")
  public OtelBlockchainMetricsResponse blockchainMetrics() {
    return observabilityInsightsService.blockchainMetrics();
  }
}
