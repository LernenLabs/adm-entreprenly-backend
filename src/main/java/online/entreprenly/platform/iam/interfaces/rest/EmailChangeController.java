package online.entreprenly.platform.iam.interfaces.rest;

import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import online.entreprenly.platform.iam.infrastructure.persistence.jpa.repositories.UserPersistenceRepository;
import online.entreprenly.platform.iam.interfaces.rest.resources.ChangeEmailResource;
import online.entreprenly.platform.shared.infrastructure.verification.VerificationChallenges;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/email-change-requests")
public class EmailChangeController {
    private final UserPersistenceRepository users;
    private final VerificationChallenges verification;
    private final String publicBaseUrl;

    public EmailChangeController(UserPersistenceRepository users, VerificationChallenges verification,
            @Value("${profile.verification.base-url:http://localhost:8092}") String publicBaseUrl) {
        this.users = users;
        this.verification = verification;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> request(@Valid @RequestBody ChangeEmailResource resource, Authentication authentication) {
        var user = users.findOneByEmail(authentication.getName()).orElse(null);
        if (user == null) return VerificationChallenges.error(HttpStatus.UNAUTHORIZED, "Sign in again.");
        var email = resource.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            return VerificationChallenges.error(HttpStatus.CONFLICT, "The email address is already in use.");
        }
        var token = UUID.randomUUID().toString();
        var link = publicBaseUrl + "/api/v1/users/me/email-change-requests/confirm/" + user.getId() + "/" + token;
        return verification.issue("email:" + user.getId(), email, token, link);
    }

    @GetMapping(value = "/confirm/{id}/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> showConfirmation(@PathVariable long id, @PathVariable UUID token) {
        return ResponseEntity.ok().header("Referrer-Policy", "no-referrer").header("Cache-Control", "no-store")
                .body("<!doctype html><html lang=\"es\"><meta charset=\"utf-8\"><title>Confirmar correo</title>"
                        + "<form method=\"post\"><button type=\"submit\">Confirmar cambio de correo</button></form></html>");
    }

    @PostMapping("/confirm/{id}/{token}")
    @Transactional
    public ResponseEntity<?> confirm(@PathVariable long id, @PathVariable UUID token) {
        var user = users.findOneById(id).orElse(null);
        if (user == null) return VerificationChallenges.error(HttpStatus.GONE, "Request a new link.");
        var key = "email:" + id;
        var check = verification.check(key, null, token.toString());
        if (check.error() != null) return check.error();
        if (users.existsByEmailIgnoreCase(check.destination())) {
            return VerificationChallenges.error(HttpStatus.CONFLICT, "The email address is already in use.");
        }
        user.setEmail(check.destination());
        user.setSessionVersion(user.getSessionVersion() + 1);
        user.setRetainedSessionId(null);
        users.saveAndFlush(user);
        verification.consume(key);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN)
                .body("Correo actualizado. Vuelve a iniciar sesión con la nueva dirección.");
    }
}
