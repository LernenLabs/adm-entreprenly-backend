package online.entreprenly.platform.notification.infrastructure.persistence.jpa.repositories;

import online.entreprenly.platform.notification.infrastructure.persistence.jpa.entities.DeviceTokenPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for device token persistence entities.
 */
@Repository
public interface DeviceTokenPersistenceRepository extends JpaRepository<DeviceTokenPersistenceEntity, Long> {
    List<DeviceTokenPersistenceEntity> findAllByOwnerEmail(String ownerEmail);

    Optional<DeviceTokenPersistenceEntity> findByToken(String token);
}
