package com.proofvault.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
  name = "users",
  indexes = {
    @Index(name = "idx_users_issuer_subject", columnList = "issuer,subject", unique = true),
    @Index(name = "idx_users_email", columnList = "email")
  }
)
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, unique = true, length = 36)
  private String publicId;

  @Column(nullable = false, length = 512)
  private String issuer;

  @Column(nullable = false, length = 255)
  private String subject;

  @Column(nullable = false, length = 320)
  private String email;

  @Column(name = "display_name", nullable = false, length = 255)
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(name = "subscription_tier", nullable = false, length = 32)
  private SubscriptionTier subscriptionTier = SubscriptionTier.FREE;

  @Column(name = "usage_count", nullable = false)
  private int usageCount = 0;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public String getPublicId() {
    return publicId;
  }

  public void setPublicId(String publicId) {
    this.publicId = publicId;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public SubscriptionTier getSubscriptionTier() {
    return subscriptionTier;
  }

  public void setSubscriptionTier(SubscriptionTier subscriptionTier) {
    this.subscriptionTier = subscriptionTier;
  }

  public int getUsageCount() {
    return usageCount;
  }

  public void setUsageCount(int usageCount) {
    this.usageCount = usageCount;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
