package com.proofvault.api.service;

import com.proofvault.api.dto.BlockchainReceipt;
import com.proofvault.api.dto.ProofResponse;
import com.proofvault.api.dto.VerificationResponse;
import com.proofvault.api.model.Proof;
import com.proofvault.api.model.User;
import com.proofvault.api.repository.ProofRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProofService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProofService.class);
	private final ProofRepository proofRepository;
	private final HashingService hashingService;
	private final IBlockchainAnchorService blockchainAnchorService;

	public ProofService(ProofRepository proofRepository, HashingService hashingService, IBlockchainAnchorService blockchainAnchorService) {
		this.proofRepository = proofRepository;
		this.hashingService = hashingService;
		this.blockchainAnchorService = blockchainAnchorService;
	}

	@Transactional
	public ProofResponse createProof(MultipartFile file, User owner) {
		if (file == null || file.isEmpty()) {
			LOGGER.warn("Proof upload rejected user={} reason=empty_file", safeUser(owner));
			throw new IllegalArgumentException("Upload a non-empty file to create a proof.");
		}

		LOGGER.info("Proof upload received user={} sizeBytes={} contentType={}", safeUser(owner), file.getSize(), safeContentType(file.getContentType()));
		String fileHash = hashingService.sha256(file);
		LOGGER.debug("Proof upload hashed user={} fileHash={}", safeUser(owner), shortHash(fileHash));

		return proofRepository.findByFileHashAndOwner(fileHash, owner)
			.map(proof -> {
				LOGGER.info("Proof upload matched existing user proof user={} proofId={} fileHash={}", safeUser(owner), proof.getPublicId(), shortHash(fileHash));
				return ProofResponse.from(proof);
			})
			.orElseGet(() -> createNewUserProof(file, fileHash, owner));
	}

	@Transactional(readOnly = true)
	public List<ProofResponse> recentProofs(User owner) {
		List<ProofResponse> proofs = proofRepository.findTop25ByOwnerOrderByCreatedAtDesc(owner).stream().map(ProofResponse::from).toList();
		LOGGER.debug("Recent proofs loaded user={} count={}", safeUser(owner), proofs.size());
		return proofs;
	}

	@Transactional(readOnly = true)
	public VerificationResponse verify(String fileHash) {
		String normalizedHash = normalizeHash(fileHash);
		LOGGER.info("Proof verification requested fileHash={}", shortHash(normalizedHash));
		return proofRepository.findFirstByFileHashOrderByCreatedAtAsc(normalizedHash)
			.map(proof -> {
				LOGGER.info("Proof verification matched fileHash={} proofId={} network={}", shortHash(normalizedHash), proof.getPublicId(), proof.getNetwork());
				return new VerificationResponse(true, proof.getFileHash(), proof.getTransactionHash(), proof.getNetwork(), proof.getBlockchainTimestamp(),
					"This hash has a timestamped ProofVault certificate.");
			})
			.orElseGet(() -> {
				LOGGER.info("Proof verification not found fileHash={}", shortHash(normalizedHash));
				return new VerificationResponse(false, normalizedHash, null, null, null, "No proof exists for this hash.");
			});
	}

	@Transactional(readOnly = true)
	public Proof getProof(String proofId, User owner) {
		LOGGER.debug("Proof lookup requested user={} proofId={}", safeUser(owner), proofId);
		return proofRepository.findByPublicIdAndOwner(proofId, owner)
			.orElseThrow(() -> {
				LOGGER.warn("Proof lookup rejected user={} proofId={} reason=not_found", safeUser(owner), proofId);
				return new IllegalArgumentException("Proof not found.");
			});
	}

	private ProofResponse createNewUserProof(MultipartFile file, String fileHash, User owner) {
		long monthlyUsage = proofRepository.countByOwnerAndCreatedAtAfter(owner, Instant.now().minus(30, ChronoUnit.DAYS));
		LOGGER.debug("Proof usage checked user={} tier={} monthlyUsage={} limit={}", safeUser(owner), owner.getSubscriptionTier(), monthlyUsage,
			owner.getSubscriptionTier().monthlyProofLimit());
		if (monthlyUsage >= owner.getSubscriptionTier().monthlyProofLimit()) {
			LOGGER.warn("Proof upload rejected user={} reason=monthly_limit tier={} monthlyUsage={} limit={}", safeUser(owner), owner.getSubscriptionTier(), monthlyUsage,
				owner.getSubscriptionTier().monthlyProofLimit());
			throw new IllegalStateException("Monthly proof limit reached for your subscription tier.");
		}

		String metadataHash = hashingService.sha256Text(String.join("|", "proofvault.v1", fileHash, String.valueOf(file.getSize()),
			file.getContentType() == null ? "application/octet-stream" : file.getContentType(), owner.getPublicId()));

		BlockchainReceipt receipt = proofRepository.findFirstByFileHashOrderByCreatedAtAsc(fileHash)
			.map(proof -> {
				LOGGER.info("Reusing existing blockchain anchor user={} sourceProofId={} fileHash={} network={}", safeUser(owner), proof.getPublicId(), shortHash(fileHash),
					proof.getNetwork());
				return new BlockchainReceipt(proof.getTransactionHash(), proof.getNetwork(), proof.getBlockchainTimestamp());
			})
			.orElseGet(() -> {
				LOGGER.info("Creating blockchain anchor user={} fileHash={} metadataHash={}", safeUser(owner), shortHash(fileHash), shortHash(metadataHash));
				return blockchainAnchorService.storeProof(fileHash, metadataHash);
			});

		Proof proof = new Proof();
		proof.setOwner(owner);
		proof.setPublicId(UUID.randomUUID().toString());
		proof.setFileName(file.getOriginalFilename() == null ? "unnamed-file" : file.getOriginalFilename());
		proof.setFileHash(fileHash);
		proof.setFileSize(file.getSize());
		proof.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
		proof.setBlockchainTimestamp(receipt.timestamp());
		proof.setTransactionHash(receipt.transactionHash());
		proof.setNetwork(receipt.network());
		proof.setTierAtCreation(owner.getSubscriptionTier());
		owner.setUsageCount(owner.getUsageCount() + 1);

		Proof savedProof = proofRepository.save(proof);
		LOGGER.info("Proof created user={} proofId={} fileHash={} network={} tx={}", safeUser(owner), savedProof.getPublicId(), shortHash(fileHash),
			savedProof.getNetwork(), shortHash(savedProof.getTransactionHash()));
		return ProofResponse.from(savedProof);
	}

	private String normalizeHash(String fileHash) {
		String normalized = fileHash == null ? "" : fileHash.trim().toLowerCase();
		return normalized.startsWith("0x") ? normalized.substring(2) : normalized;
	}

	private String safeUser(User owner) {
		return owner == null || owner.getPublicId() == null ? "unknown" : owner.getPublicId();
	}

	private String safeContentType(String contentType) {
		return contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
	}

	private String shortHash(String hash) {
		if (hash == null || hash.isBlank()) {
			return "none";
		}
		String normalized = hash.startsWith("0x") ? hash.substring(2) : hash;
		if (normalized.length() <= 12) {
			return normalized;
		}
		return normalized.substring(0, 6) + "..." + normalized.substring(normalized.length() - 6);
	}
}
