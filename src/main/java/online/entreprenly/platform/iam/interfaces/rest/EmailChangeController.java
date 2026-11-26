package online.entreprenly.platform.iam.interfaces.rest;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import online.entreprenly.platform.iam.infrastructure.notifications.ResendRecoveryEmail;
import online.entreprenly.platform.iam.infrastructure.persistence.jpa.entities.UserPersistenceEntity;
import online.entreprenly.platform.iam.infrastructure.persistence.jpa.repositories.UserPersistenceRepository;
import online.entreprenly.platform.iam.interfaces.rest.resources.ChangeEmailResource;
import online.entreprenly.platform.shared.infrastructure.verification.VerificationChallenges;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/email-change-requests")
public class EmailChangeController {
    private static final String CONFIRM_PATH = "/api/v1/users/me/email-change-requests/confirm/";
    private final UserPersistenceRepository users;
    private final VerificationChallenges verification;
    private final ResendRecoveryEmail resend;
    private final String localBaseUrl;

    public EmailChangeController(UserPersistenceRepository users, VerificationChallenges verification,
            ResendRecoveryEmail resend, @Value("${profile.verification.base-url:http://localhost:8092}") String localBaseUrl) {
        this.users = users;
        this.verification = verification;
        this.resend = resend;
        this.localBaseUrl = localBaseUrl.replaceAll("/+$", "");
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> request(@Valid @RequestBody ChangeEmailResource resource, Authentication authentication) {
        var user = users.findOneByEmail(authentication.getName()).orElse(null);
        if (user == null) return VerificationChallenges.error(HttpStatus.UNAUTHORIZED, "Sign in again.");
        var email = resource.email().trim().toLowerCase(Locale.ROOT);
        if (unavailable(email, user.getId())) {
            return VerificationChallenges.error(HttpStatus.CONFLICT, "The email address is already in use.");
        }
        var token = UUID.randomUUID();
        var path = CONFIRM_PATH + user.getId() + "/" + token;
        if (verification.deliveryAvailable()) {
            return verification.issue(key(user), email, versionedToken(user, token), localBaseUrl + path);
        }
        if (!resend.available()) {
            return VerificationChallenges.error(HttpStatus.SERVICE_UNAVAILABLE, "Email delivery is unavailable.");
        }
        return verification.issueDeliveredEmail(key(user), email, versionedToken(user, token),
                () -> resend.sendEmailChange(email, resend.link(path)));
    }

    @GetMapping(value = "/confirm/{id}/{token}", produces = MediaType.TEXT_HTML_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<String> showConfirmation(@PathVariable long id, @PathVariable UUID token) {
        var user = users.findById(id).orElse(null);
        if (!valid(user, token)) return expiredPage();
        return page(HttpStatus.OK, "<form method=\"post\"><button type=\"submit\">Confirmar cambio de correo</button></form>");
    }

    @PostMapping(value = "/confirm/{id}/{token}", produces = MediaType.TEXT_HTML_VALUE)
    @Transactional
    public ResponseEntity<String> confirm(@PathVariable long id, @PathVariable UUID token) {
        var user = users.findOneById(id).orElse(null);
        if (user == null) return expiredPage();
        var check = verification.checkLink(key(user), null, versionedToken(user, token));
        if (check.error() != null) return expiredPage();
        if (unavailable(check.destination(), user.getId())) {
            return emailInUse();
        }
        // Existing merchant records identify their owner by the original email.
        if (user.getMerchantOwnerEmail() == null) user.setMerchantOwnerEmail(user.getEmail());
        user.setEmail(check.destination());
        user.setEmailVerifiedAt(Instant.now());
        user.setSessionVersion(user.getSessionVersion() + 1);
        user.setRetainedSessionId(null);
        users.saveAndFlush(user);
        verification.consume(key(user));
        return page(HttpStatus.OK, "<p>Correo verificado y actualizado. Vuelve a iniciar sesión con la nueva dirección.</p>");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> emailInUse() {
        return page(HttpStatus.CONFLICT, "<p>Ese correo ya está en uso. Solicita otro cambio desde Entreprenly.</p>");
    }

    private boolean unavailable(String email, long userId) {
        return users.existsByEmailIgnoreCase(email)
                || users.existsByMerchantOwnerEmailIgnoreCaseAndIdNot(email, userId);
    }

    private boolean valid(UserPersistenceEntity user, UUID token) {
        return user != null && verification.checkLink(key(user), null, versionedToken(user, token)).error() == null;
    }

    private static String key(UserPersistenceEntity user) {
        return "email:" + user.getId();
    }

    private static String versionedToken(UserPersistenceEntity user, UUID token) {
        return user.getSessionVersion() + ":" + token;
    }

    private static ResponseEntity<String> expiredPage() {
        return page(HttpStatus.GONE, "<p>El enlace es inválido o venció. Abre Entreprenly y solicita otro en Contraseña y seguridad.</p>");
    }

    private static ResponseEntity<String> page(HttpStatus status, String body) {
        return ResponseEntity.status(status).contentType(MediaType.TEXT_HTML)
                .header("Cache-Control", "no-store").header("Referrer-Policy", "no-referrer")
                .header("Content-Security-Policy", "default-src 'none'; form-action 'self'; frame-ancestors 'none'")
                .body("<!doctype html><html lang=\"es\"><meta charset=\"utf-8\">"
                        + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                        + "<title>Confirmar correo · Entreprenly</title>"
                        + "<main><h1>Confirmar correo</h1>" + body + "</main></html>");
    }
}
