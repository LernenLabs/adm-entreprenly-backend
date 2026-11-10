package online.entreprenly.platform.profile.infrastructure.persistence.jpa.repositories;

import java.util.Optional;
import java.util.UUID;
import online.entreprenly.platform.profile.infrastructure.persistence.jpa.entities.ProfileAvatarEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileAvatarRepository extends JpaRepository<ProfileAvatarEntity, Long> {
    Optional<ProfileAvatarEntity> findByAccessKey(UUID key);
}
