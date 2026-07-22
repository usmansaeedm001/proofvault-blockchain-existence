package com.proofvault.api.dto;

public record SubscriptionResponse(
  String currentTier,
  int monthlyLimit,
  long currentUsage,
  String upgradeMessage
) {}
