package online.entreprenly.platform.shared.infrastructure.verification;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import online.entreprenly.platform.shared.interfaces.rest.resources.ErrorResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class VerificationChallenges {
    private static final Duration LIFETIME = Duration.ofMinutes(10);
    private static final Duration RESEND_DELAY = Duration.ofSeconds(30);
    private static final int MAX_ATTEMPTS = 5;

    private final VerificationChallengeRepository challenges;
    private final Environment environment;
    private final Path inbox;
    private final String secret;

    public VerificationChallenges(VerificationChallengeRepository challenges, Environment environment,
            @Value("${profile.verification.inbox:target/av2-inbox}") String inbox,
            @Value("${authorization.jwt.secret}") String secret) {
        this.challenges = challenges;
        this.environment = environment;
        this.inbox = Path.of(inbox);
        this.secret = secret;
    }

    /** Called under the owning user's or profile's database lock. */
    public ResponseEntity<?> issue(String id, String destination, String code, String message) {
        if (!environment.acceptsProfiles(Profiles.of("av2"))) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, "Verification delivery is unavailable.");
        }
        var now = Instant.now();
        var previous = challenges.findById(id);
        if (previous.isPresent() && now.isBefore(previous.get().issuedAt.plus(RESEND_DELAY))) {
            return error(HttpStatus.TOO_MANY_REQUESTS, "Wait 30 seconds before requesting another code or link.");
        }
        try {
            Files.createDirectories(inbox);
            Files.writeString(inbox.resolve(id.replace(':', '-') + ".txt"), destination + System.lineSeparator() + message);
        } catch (IOException ex) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, "The test inbox could not accept the message.");
        }
        challenges.save(new VerificationChallenge(id, destination, digest(id, code), now));
        return ResponseEntity.accepted().build();
    }

    /** A failed attempt is committed by the caller, so retries cannot bypass the attempt limit. */
    public Check check(String id, String destination, String code) {
        var challenge = challenges.findById(id).orElse(null);
        if (challenge == null || !Instant.now().isBefore(challenge.issuedAt.plus(LIFETIME))
                || challenge.attempts >= MAX_ATTEMPTS) {
            return new Check(null, error(HttpStatus.GONE, "The code or link expired. Request another one."));
        }
        if ((destination != null && !destination.equals(challenge.destination))
                || !MessageDigest.isEqual(HexFormat.of().parseHex(challenge.digest), HexFormat.of().parseHex(digest(id, code)))) {
            challenge.attempts++;
            return new Check(null, error(HttpStatus.BAD_REQUEST, "Invalid verification code or link."));
        }
        return new Check(challenge.destination, null);
    }

    public void consume(String id) {
        challenges.deleteById(id);
    }

    public static ResponseEntity<ErrorResource> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResource(status.name(), message));
    }

    private String digest(String id, String code) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(("profile-verification:" + id + ":" + code)
                    .getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to protect verification secret", ex);
        }
    }

    public record Check(String destination, ResponseEntity<?> error) {}
}
