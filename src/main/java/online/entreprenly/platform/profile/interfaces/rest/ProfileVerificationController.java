package online.entreprenly.platform.profile.interfaces.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;
import online.entreprenly.platform.iam.domain.repositories.UserRepository;
import online.entreprenly.platform.profile.infrastructure.persistence.jpa.assemblers.ProfilePersistenceAssembler;
import online.entreprenly.platform.profile.infrastructure.persistence.jpa.entities.ProfileAvatarEntity;
import online.entreprenly.platform.profile.infrastructure.persistence.jpa.entities.ProfilePersistenceEntity;
import online.entreprenly.platform.profile.infrastructure.persistence.jpa.repositories.ProfileAvatarRepository;
import online.entreprenly.platform.profile.infrastructure.persistence.jpa.repositories.ProfilePersistenceRepository;
import online.entreprenly.platform.profile.interfaces.rest.resources.ProfileResource;
import online.entreprenly.platform.profile.interfaces.rest.transform.ProfileResourceFromEntityAssembler;
import online.entreprenly.platform.shared.infrastructure.verification.VerificationChallenges;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class ProfileVerificationController {
    private final SecureRandom random = new SecureRandom();
    private final UserRepository users;
    private final ProfilePersistenceRepository profiles;
    private final ProfileAvatarRepository avatars;
    private final VerificationChallenges verification;

    public ProfileVerificationController(UserRepository users, ProfilePersistenceRepository profiles,
            ProfileAvatarRepository avatars, VerificationChallenges verification) {
        this.users = users;
        this.profiles = profiles;
        this.avatars = avatars;
        this.verification = verification;
    }

    @PostMapping(value = "/profiles/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> upload(@PathVariable long id, @RequestPart("image") MultipartFile image,
            Authentication authentication) throws IOException {
        var profile = ownedProfile(id, authentication);
        if (profile == null) return profileNotFound();
        if (image.getSize() > AvatarFormat.MAX_BYTES) {
            return VerificationChallenges.error(HttpStatus.PAYLOAD_TOO_LARGE, "The photo must be 5 MB or less.");
        }
        byte[] bytes = image.getBytes();
        String mime;
        try {
            mime = AvatarFormat.detect(bytes);
        } catch (IllegalArgumentException ex) {
            return VerificationChallenges.error(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        var key = UUID.randomUUID();
        avatars.save(new ProfileAvatarEntity(id, key, mime, bytes));
        profile.setAvatarUrl("/api/v1/profile-avatars/" + key);
        return ResponseEntity.ok(toResource(profile));
    }

    @GetMapping("/profile-avatars/{key}")
    public ResponseEntity<byte[]> avatar(@PathVariable UUID key) {
        return avatars.findByAccessKey(key)
                .map(avatar -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(avatar.getMimeType()))
                        .header("X-Content-Type-Options", "nosniff")
                        .header("Cache-Control", "private, no-store")
                        .body(avatar.getImage()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/profiles/{id}/phone-verifications")
    @Transactional
    public ResponseEntity<?> requestPhone(@PathVariable long id, @Valid @RequestBody PhoneRequest request,
            Authentication authentication) {
        if (ownedProfile(id, authentication) == null) return profileNotFound();
        var code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        return verification.issue("phone:" + id, request.phone(), code, code);
    }

    @PostMapping("/profiles/{id}/phone-verifications/confirm")
    @Transactional
    public ResponseEntity<?> confirmPhone(@PathVariable long id, @Valid @RequestBody PhoneConfirmation request,
            Authentication authentication) {
        var profile = ownedProfile(id, authentication);
        if (profile == null) return profileNotFound();
        var key = "phone:" + id;
        var check = verification.check(key, request.phone(), request.code());
        if (check.error() != null) return check.error();
        profile.setPhone(check.destination());
        verification.consume(key);
        return ResponseEntity.ok(toResource(profile));
    }

    private ProfilePersistenceEntity ownedProfile(long id, Authentication authentication) {
        return users.findByEmail(authentication.getName())
                .flatMap(user -> profiles.findOneByIdAndUserId(id, user.getId())).orElse(null);
    }

    private static ResponseEntity<?> profileNotFound() {
        return VerificationChallenges.error(HttpStatus.NOT_FOUND, "Profile not found.");
    }

    private static ProfileResource toResource(ProfilePersistenceEntity entity) {
        return ProfileResourceFromEntityAssembler.toResourceFromEntity(
                ProfilePersistenceAssembler.toDomainFromPersistence(entity));
    }

    public record PhoneRequest(@NotBlank @Pattern(regexp = "[+]?[0-9]{7,15}") String phone) {}

    public record PhoneConfirmation(
            @NotBlank @Pattern(regexp = "[+]?[0-9]{7,15}") String phone,
            @NotBlank @Pattern(regexp = "[0-9]{6}") String code) {}
}
