package com.proofvault.api.service;

import com.proofvault.api.config.ProofVaultProperties;
import com.proofvault.api.model.User;
import com.proofvault.api.repository.UserRepository;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {
  private static final Logger LOGGER = LoggerFactory.getLogger(CurrentUserService.class);
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
      LOGGER.debug("Resolving local user because API authentication is disabled");
      return userRepository.findByIssuerAndSubject(LOCAL_ISSUER, LOCAL_SUBJECT)
        .orElseGet(this::createLocalUser);
    }

    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      LOGGER.warn("Current user resolution rejected reason=missing_jwt_principal");
      throw new IllegalStateException("OAuth2 authentication is required.");
    }

    String issuer = jwt.getIssuer() == null ? "unknown-issuer" : jwt.getIssuer().toString();
    String subject = jwt.getSubject();
    if (subject == null || subject.isBlank()) {
      LOGGER.warn("Current user resolution rejected issuer={} reason=missing_subject", issuer);
      throw new IllegalStateException("OAuth2 token is missing subject.");
    }

    String email = claim(jwt, properties.security().userEmailClaim(), subject + "@unknown.local");
    String displayName = claim(jwt, properties.security().userNameClaim(), email);

    LOGGER.debug("Resolving current user issuer={} subject={}", issuer, shortSubject(subject));
    return userRepository.findByIssuerAndSubject(issuer, subject)
      .map(user -> refreshUser(user, email, displayName))
      .orElseGet(() -> createUser(issuer, subject, email, displayName));
  }

  private User createLocalUser() {
    LOGGER.info("Creating local ProofVault user");
    return createUser(LOCAL_ISSUER, LOCAL_SUBJECT, "local@proofvault.test", "Local ProofVault User");
  }

  private User createUser(String issuer, String subject, String email, String displayName) {
    User user = new User();
    user.setPublicId(UUID.randomUUID().toString());
    user.setIssuer(issuer);
    user.setSubject(subject);
    user.setEmail(email);
    user.setDisplayName(displayName);
    User savedUser = userRepository.save(user);
    LOGGER.info("Created API user user={} issuer={} subject={}", savedUser.getPublicId(), issuer, shortSubject(subject));
    return savedUser;
  }

  private User refreshUser(User user, String email, String displayName) {
    boolean changed = false;
    if (!Objects.equals(user.getEmail(), email)) {
      user.setEmail(email);
      changed = true;
    }
    if (!Objects.equals(user.getDisplayName(), displayName)) {
      user.setDisplayName(displayName);
      changed = true;
    }
    if (changed) {
      LOGGER.info("Refreshed API user profile user={}", user.getPublicId());
    } else {
      LOGGER.debug("API user profile unchanged user={}", user.getPublicId());
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

  private String shortSubject(String subject) {
    if (subject == null || subject.isBlank()) {
      return "unknown";
    }
    if (subject.startsWith("wallet:0x") && subject.length() >= 19) {
      return subject.substring(0, 13) + "..." + subject.substring(subject.length() - 6);
    }
    if (subject.length() <= 18) {
      return subject;
    }
    return subject.substring(0, 9) + "..." + subject.substring(subject.length() - 6);
  }
}
