package com.proofvault.api.controller;

import com.proofvault.api.dto.SubscriptionResponse;
import com.proofvault.api.model.User;
import com.proofvault.api.repository.ProofRepository;
import com.proofvault.api.service.CurrentUserService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {
  private final ProofRepository proofRepository;
  private final CurrentUserService currentUserService;

  public SubscriptionController(ProofRepository proofRepository, CurrentUserService currentUserService) {
    this.proofRepository = proofRepository;
    this.currentUserService = currentUserService;
  }

  @GetMapping
  public SubscriptionResponse currentSubscription(Authentication authentication) {
    User user = currentUserService.currentUser(authentication);
    long usage = proofRepository.countByOwnerAndCreatedAtAfter(
      user,
      Instant.now().minus(30, ChronoUnit.DAYS)
    );

    return new SubscriptionResponse(
      user.getSubscriptionTier().name(),
      user.getSubscriptionTier().monthlyProofLimit(),
      usage,
      "Stripe checkout can be wired here when billing is enabled."
    );
  }
}
