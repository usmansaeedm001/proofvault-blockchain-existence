package com.proofvault.api.repository;

import com.proofvault.api.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  Optional<User> findByIssuerAndSubject(String issuer, String subject);

  Optional<User> findByPublicId(String publicId);
}
