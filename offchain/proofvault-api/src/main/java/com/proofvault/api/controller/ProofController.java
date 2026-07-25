package com.proofvault.api.controller;

import com.proofvault.api.dto.ProofResponse;
import com.proofvault.api.dto.VerificationRequest;
import com.proofvault.api.dto.VerificationResponse;
import com.proofvault.api.model.Proof;
import com.proofvault.api.service.CertificateService;
import com.proofvault.api.service.CurrentUserService;
import com.proofvault.api.service.ProofService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/proofs")
public class ProofController {
  private final ProofService proofService;
  private final CertificateService certificateService;
  private final CurrentUserService currentUserService;

  public ProofController(
    ProofService proofService,
    CertificateService certificateService,
    CurrentUserService currentUserService
  ) {
    this.proofService = proofService;
    this.certificateService = certificateService;
    this.currentUserService = currentUserService;
  }

  @GetMapping
  public List<ProofResponse> recentProofs(Authentication authentication) {
    return proofService.recentProofs(currentUserService.currentUser(authentication));
  }

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ProofResponse upload(@RequestParam("file") MultipartFile file, Authentication authentication) {
    return proofService.createProof(file, currentUserService.currentUser(authentication));
  }

  @PostMapping("/verify")
  public VerificationResponse verify(@Valid @RequestBody VerificationRequest request) {
    return proofService.verify(request.fileHash());
  }

  @GetMapping("/{proofId}/certificate")
  public ResponseEntity<byte[]> certificate(@PathVariable String proofId, Authentication authentication) {
    Proof proof = proofService.getProof(proofId, currentUserService.currentUser(authentication));
    byte[] certificate = certificateService.buildCertificate(proof);

    return ResponseEntity.ok()
      .contentType(MediaType.TEXT_PLAIN)
      .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
        .filename("proofvault-certificate-%s.txt".formatted(proof.getPublicId()))
        .build()
        .toString())
      .body(certificate);
  }
}
