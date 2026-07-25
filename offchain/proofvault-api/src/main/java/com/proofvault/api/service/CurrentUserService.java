package com.proofvault.api.service;

import com.proofvault.api.config.ProofVaultProperties;
import com.proofvault.api.model.User;
import com.proofvault.api.repository.UserRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {
  private static final String LOCAL_ISSUER = "local";
  private static final String LOCAL_SUBJECT = "local-user";

  private final ProofVaultProperties properties;
  private final UserRepository userRepository;

  public CurrentUserService(ProofVaultProperties properties, UserRepository userRepository) {
    this.properties = properties;
    this.userRepository = userRepository;
  }

  @Transactional
  public User currentUser(Authentication authentication) {
    if (!properties.security().authenticationEnabled()) {
      return userRepository.findByIssuerAndSubject(LOCAL_ISSUER, LOCAL_SUBJECT)
        .orElseGet(this::createLocalUser);
    }

    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      throw new IllegalStateException("OAuth2 authentication is required.");
    }

    String issuer = jwt.getIssuer() == null ? "unknown-issuer" : jwt.getIssuer().toString();
    String subject = jwt.getSubject();
    if (subject == null || subject.isBlank()) {
      throw new IllegalStateException("OAuth2 token is missing subject.");
    }

    String email = claim(jwt, properties.security().userEmailClaim(), subject + "@unknown.local");
    String displayName = claim(jwt, properties.security().userNameClaim(), email);

    return userRepository.findByIssuerAndSubject(issuer, subject)
      .map(user -> refreshUser(user, email, displayName))
      .orElseGet(() -> createUser(issuer, subject, email, displayName));
  }

  private User createLocalUser() {
    return createUser(LOCAL_ISSUER, LOCAL_SUBJECT, "local@proofvault.test", "Local ProofVault User");
  }

  private User createUser(String issuer, String subject, String email, String displayName) {
    User user = new User();
    user.setPublicId(UUID.randomUUID().toString());
    user.setIssuer(issuer);
    user.setSubject(subject);
    user.setEmail(email);
    user.setDisplayName(displayName);
    return userRepository.save(user);
  }

  private User refreshUser(User user, String email, String displayName) {
    if (!Objects.equals(user.getEmail(), email)) {
      user.setEmail(email);
    }
    if (!Objects.equals(user.getDisplayName(), displayName)) {
      user.setDisplayName(displayName);
    }
    return user;
  }

  private String claim(Jwt jwt, String claimName, String fallback) {
    Object value = jwt.getClaims().get(claimName);
    if (value == null || value.toString().isBlank()) {
      return fallback;
    }
    return value.toString();
  }
}
