package com.proofvault.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
  name = "proofs",
  indexes = {
    @Index(name = "idx_proofs_public_id", columnList = "public_id", unique = true),
    @Index(name = "idx_proofs_file_hash", columnList = "file_hash"),
    @Index(name = "idx_proofs_owner_created_at", columnList = "owner_id,created_at")
  }
)
public class Proof {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @Column(name = "public_id", nullable = false, unique = true, length = 36)
  private String publicId;

  @Column(name = "file_name", nullable = false, length = 512)
  private String fileName;

  @Column(name = "file_hash", nullable = false, length = 64)
  private String fileHash;

  @Column(name = "file_size", nullable = false)
  private long fileSize;

  @Column(name = "content_type", nullable = false, length = 255)
  private String contentType;

  @Column(name = "blockchain_timestamp", nullable = false)
  private Instant blockchainTimestamp;

  @Column(name = "transaction_hash", nullable = false, length = 128)
  private String transactionHash;

  @Column(nullable = false, length = 64)
  private String network;

  @Enumerated(EnumType.STRING)
  @Column(name = "tier_at_creation", nullable = false, length = 32)
  private SubscriptionTier tierAtCreation = SubscriptionTier.FREE;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() {
    return id;
  }

  public User getOwner() {
    return owner;
  }

  public void setOwner(User owner) {
    this.owner = owner;
  }

  public String getPublicId() {
    return publicId;
  }

  public void setPublicId(String publicId) {
    this.publicId = publicId;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getFileHash() {
    return fileHash;
  }

  public void setFileHash(String fileHash) {
    this.fileHash = fileHash;
  }

  public long getFileSize() {
    return fileSize;
  }

  public void setFileSize(long fileSize) {
    this.fileSize = fileSize;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public Instant getBlockchainTimestamp() {
    return blockchainTimestamp;
  }

  public void setBlockchainTimestamp(Instant blockchainTimestamp) {
    this.blockchainTimestamp = blockchainTimestamp;
  }

  public String getTransactionHash() {
    return transactionHash;
  }

  public void setTransactionHash(String transactionHash) {
    this.transactionHash = transactionHash;
  }

  public String getNetwork() {
    return network;
  }

  public void setNetwork(String network) {
    this.network = network;
  }

  public SubscriptionTier getTierAtCreation() {
    return tierAtCreation;
  }

  public void setTierAtCreation(SubscriptionTier tierAtCreation) {
    this.tierAtCreation = tierAtCreation;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
