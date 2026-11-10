package online.entreprenly.platform.iam.interfaces.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import online.entreprenly.platform.iam.application.internal.outboundservices.hashing.HashingService;
import online.entreprenly.platform.iam.infrastructure.persistence.jpa.entities.UserPersistenceEntity;
import online.entreprenly.platform.iam.infrastructure.persistence.jpa.repositories.UserPersistenceRepository;
import online.entreprenly.platform.iam.interfaces.rest.resources.ChangeEmailResource;
import online.entreprenly.platform.shared.infrastructure.verification.VerificationChallenges;
import online.entreprenly.platform.iam.infrastructure.notifications.ResendRecoveryEmail;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/authentication/password-resets")
public class PasswordResetController {
    private final UserPersistenceRepository users;
    private final VerificationChallenges verification;
    private final HashingService hashing;
    private final ResendRecoveryEmail resend;

    public PasswordResetController(UserPersistenceRepository users, VerificationChallenges verification,
            HashingService hashing, ResendRecoveryEmail resend) {
        this.users = users;
        this.verification = verification;
        this.hashing = hashing;
        this.resend = resend;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> request(@Valid @RequestBody ChangeEmailResource resource) {
        if (!verification.deliveryAvailable() && !resend.available()) {
            return VerificationChallenges.error(HttpStatus.SERVICE_UNAVAILABLE, "Password recovery is unavailable.");
        }
        users.findOneByEmail(resource.email().trim()).ifPresent(user -> {
            var token = UUID.randomUUID();
            if (verification.deliveryAvailable()) {
                var link = "entreprenly-av2://reset-password/" + user.getId() + "/" + token;
                verification.issue(key(user), user.getEmail(), versionedToken(user, token), link);
            } else {
                var email = user.getEmail();
                var link = resend.link("/api/v1/authentication/password-resets/open/" + user.getId() + "/" + token);
                verification.issueEmail(key(user), email, versionedToken(user, token), () -> resend.send(email, link));
            }
        });
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/validate")
    @Transactional(readOnly = true)
    public ResponseEntity<?> validate(@Valid @RequestBody ResetLink link) {
        var user = users.findById(link.userId()).orElse(null);
        return valid(user, link.token()) ? ResponseEntity.noContent().build() : invalidLink();
    }

    @PostMapping("/confirm")
    @Transactional
    public ResponseEntity<?> confirm(@Valid @RequestBody ResetPassword request) {
        var user = users.findOneById(request.userId()).orElse(null);
        if (!valid(user, request.token())) return invalidLink();
        if (!validPassword(request.newPassword())) {
            return VerificationChallenges.error(HttpStatus.BAD_REQUEST, "Password must contain at least 8 characters and at most 72 bytes.");
        }
        reset(user, request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/open/{id}/{token}", produces = MediaType.TEXT_HTML_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<String> open(@PathVariable long id, @PathVariable UUID token) {
        return valid(users.findById(id).orElse(null), token)
                ? page(HttpStatus.OK, form(""))
                : expiredPage();
    }

    @PostMapping(value = "/open/{id}/{token}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    @Transactional
    public ResponseEntity<String> submit(@PathVariable long id, @PathVariable UUID token,
            @RequestParam String newPassword, @RequestParam String confirmation) {
        var user = users.findOneById(id).orElse(null);
        if (!valid(user, token)) return expiredPage();
        if (!validPassword(newPassword) || !newPassword.equals(confirmation)) {
            return page(HttpStatus.BAD_REQUEST, form("<p>Las contraseñas deben coincidir y tener al menos 8 caracteres y un máximo de 72 bytes.</p>"));
        }
        reset(user, newPassword);
        return page(HttpStatus.OK, "<p>Contraseña actualizada. Abre Entreprenly e inicia sesión con tu nueva contraseña.</p>");
    }

    private void reset(UserPersistenceEntity user, String password) {
        user.setPassword(hashing.encode(password));
        user.setSessionVersion(user.getSessionVersion() + 1);
        user.setRetainedSessionId(null);
        verification.consume(key(user));
    }

    private static boolean validPassword(String password) {
        return !password.isBlank() && password.length() >= 8 && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    private static ResponseEntity<String> expiredPage() {
        return page(HttpStatus.GONE, "<p>El enlace es inválido o venció. Solicita otro desde la pantalla de inicio de sesión.</p>");
    }

    private static String form(String message) {
        return message + """
                <form method="post">
                  <p><label>Nueva contraseña <input name="newPassword" type="password"
                    autocomplete="new-password" required minlength="8" maxlength="72"></label></p>
                  <p><label>Confirmar contraseña <input name="confirmation" type="password"
                    autocomplete="new-password" required minlength="8" maxlength="72"></label></p>
                  <button type="submit">Guardar nueva contraseña</button>
                </form>
                """;
    }

    private static ResponseEntity<String> page(HttpStatus status, String body) {
        return ResponseEntity.status(status).contentType(MediaType.TEXT_HTML)
                .header("Cache-Control", "no-store").header("Referrer-Policy", "no-referrer")
                .header("Content-Security-Policy", "default-src 'none'; form-action 'self'; frame-ancestors 'none'")
                .body("<!doctype html><html lang=\"es\"><meta charset=\"utf-8\">"
                        + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                        + "<title>Restablecer contraseña · Entreprenly</title>"
                        + "<main><h1>Restablecer contraseña</h1>" + body + "</main></html>");
    }

    private boolean valid(UserPersistenceEntity user, UUID token) {
        return user != null && verification.checkLink(key(user), user.getEmail(), versionedToken(user, token)).error() == null;
    }

    private static ResponseEntity<?> invalidLink() {
        return VerificationChallenges.error(HttpStatus.GONE, "The recovery link is invalid or expired. Request another one.");
    }

    private static String key(UserPersistenceEntity user) {
        return "password-reset:" + user.getId();
    }

    private static String versionedToken(UserPersistenceEntity user, UUID token) {
        return token + ":" + user.getSessionVersion();
    }

    public record ResetLink(@NotNull @Positive Long userId, @NotNull UUID token) {}

    public record ResetPassword(@NotNull @Positive Long userId, @NotNull UUID token,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {}
}
