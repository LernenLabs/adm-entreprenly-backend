package online.entreprenly.platform.shared.infrastructure.verification;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "verification_challenges")
public class VerificationChallenge {
    @Id
    String id;
    String destination;
    String digest;
    Instant issuedAt;
    int attempts;

    protected VerificationChallenge() {}

    VerificationChallenge(String id, String destination, String digest, Instant issuedAt) {
        this.id = id;
        this.destination = destination;
        this.digest = digest;
        this.issuedAt = issuedAt;
    }
}
