package com.proofvault.api.model;

public enum SubscriptionTier {
  FREE(5),
  BASIC(50),
  PRO(Integer.MAX_VALUE);

  private final int monthlyProofLimit;

  SubscriptionTier(int monthlyProofLimit) {
    this.monthlyProofLimit = monthlyProofLimit;
  }

  public int monthlyProofLimit() {
    return monthlyProofLimit;
  }
}
