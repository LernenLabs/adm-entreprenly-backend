package online.entreprenly.platform.profile.domain.repositories;

import online.entreprenly.platform.profile.domain.model.aggregates.Profile;

import java.util.Optional;

/**
 * Profile repository port.
 */
public interface ProfileRepository {
    Optional<Profile> findById(Long id);

    Optional<Profile> findByUserId(Long userId);

    Optional<Profile> findByIdForUpdate(Long id);
    Optional<Profile> findByUserIdForUpdate(Long userId);
    boolean existsByUserId(Long userId);

    Profile save(Profile profile);
}
