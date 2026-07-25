package com.proofvault.api.repository;

import com.proofvault.api.model.Proof;
import com.proofvault.api.model.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProofRepository extends JpaRepository<Proof, Long> {
  Optional<Proof> findFirstByFileHashOrderByCreatedAtAsc(String fileHash);

  Optional<Proof> findByFileHashAndOwner(String fileHash, User owner);

  Optional<Proof> findByPublicIdAndOwner(String publicId, User owner);

  List<Proof> findTop25ByOwnerOrderByCreatedAtDesc(User owner);

  long countByOwnerAndCreatedAtAfter(User owner, Instant createdAt);
}
