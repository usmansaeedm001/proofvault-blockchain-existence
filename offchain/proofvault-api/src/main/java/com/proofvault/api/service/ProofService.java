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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProofService {
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
			throw new IllegalArgumentException("Upload a non-empty file to create a proof.");
		}

		String fileHash = hashingService.sha256(file);

		return proofRepository.findByFileHashAndOwner(fileHash, owner).map(ProofResponse::from).orElseGet(() -> createNewUserProof(file, fileHash, owner));
	}

	@Transactional(readOnly = true)
	public List<ProofResponse> recentProofs(User owner) {
		return proofRepository.findTop25ByOwnerOrderByCreatedAtDesc(owner).stream().map(ProofResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public VerificationResponse verify(String fileHash) {
		String normalizedHash = normalizeHash(fileHash);
		return proofRepository.findFirstByFileHashOrderByCreatedAtAsc(normalizedHash)
			.map(proof -> new VerificationResponse(true, proof.getFileHash(), proof.getTransactionHash(), proof.getNetwork(), proof.getBlockchainTimestamp(),
				"This hash has a timestamped ProofVault certificate."))
			.orElseGet(() -> new VerificationResponse(false, normalizedHash, null, null, null, "No proof exists for this hash."));
	}

	@Transactional(readOnly = true)
	public Proof getProof(String proofId, User owner) {
		return proofRepository.findByPublicIdAndOwner(proofId, owner).orElseThrow(() -> new IllegalArgumentException("Proof not found."));
	}

	private ProofResponse createNewUserProof(MultipartFile file, String fileHash, User owner) {
		long monthlyUsage = proofRepository.countByOwnerAndCreatedAtAfter(owner, Instant.now().minus(30, ChronoUnit.DAYS));
		if (monthlyUsage >= owner.getSubscriptionTier().monthlyProofLimit()) {
			throw new IllegalStateException("Monthly proof limit reached for your subscription tier.");
		}

		String metadataHash = hashingService.sha256Text(String.join("|", "proofvault.v1", fileHash, String.valueOf(file.getSize()),
			file.getContentType() == null ? "application/octet-stream" : file.getContentType(), owner.getPublicId()));

		BlockchainReceipt receipt = proofRepository.findFirstByFileHashOrderByCreatedAtAsc(fileHash)
			.map(proof -> new BlockchainReceipt(proof.getTransactionHash(), proof.getNetwork(), proof.getBlockchainTimestamp()))
			.orElseGet(() -> blockchainAnchorService.storeProof(fileHash, metadataHash));

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

		return ProofResponse.from(proofRepository.save(proof));
	}

	private String normalizeHash(String fileHash) {
		String normalized = fileHash == null ? "" : fileHash.trim().toLowerCase();
		return normalized.startsWith("0x") ? normalized.substring(2) : normalized;
	}
}
