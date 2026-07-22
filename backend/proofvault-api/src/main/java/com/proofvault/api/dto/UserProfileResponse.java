package com.proofvault.api.dto;

import com.proofvault.api.model.User;

public record UserProfileResponse(
  String id,
  String email,
  String displayName,
  String subscriptionTier,
  int monthlyProofLimit,
  int usageCount
) {
  public static UserProfileResponse from(User user) {
    return new UserProfileResponse(
      user.getPublicId(),
      user.getEmail(),
      user.getDisplayName(),
      user.getSubscriptionTier().name(),
      user.getSubscriptionTier().monthlyProofLimit(),
      user.getUsageCount()
    );
  }
}
