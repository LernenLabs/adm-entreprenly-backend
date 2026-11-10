package online.entreprenly.platform.shared.infrastructure.verification;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationChallengeRepository extends JpaRepository<VerificationChallenge, String> {}
