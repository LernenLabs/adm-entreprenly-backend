package online.entreprenly.platform.notification.infrastructure.persistence.jpa.adapters;

import online.entreprenly.platform.notification.domain.model.aggregates.DeviceToken;
import online.entreprenly.platform.notification.domain.repositories.DeviceTokenRepository;
import online.entreprenly.platform.notification.infrastructure.persistence.jpa.assemblers.DeviceTokenPersistenceAssembler;
import online.entreprenly.platform.notification.infrastructure.persistence.jpa.repositories.DeviceTokenPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository adapter that bridges the device token domain repository port with Spring Data JPA.
 */
@Repository
public class DeviceTokenRepositoryImpl implements DeviceTokenRepository {

    private final DeviceTokenPersistenceRepository deviceTokenPersistenceRepository;

    public DeviceTokenRepositoryImpl(DeviceTokenPersistenceRepository deviceTokenPersistenceRepository) {
        this.deviceTokenPersistenceRepository = deviceTokenPersistenceRepository;
    }

    @Override
    public List<DeviceToken> findAllByOwnerEmail(String ownerEmail) {
        return deviceTokenPersistenceRepository.findAllByOwnerEmail(ownerEmail).stream()
                .map(DeviceTokenPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Optional<DeviceToken> findByToken(String token) {
        return deviceTokenPersistenceRepository.findByToken(token)
                .map(DeviceTokenPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public DeviceToken save(DeviceToken deviceToken) {
        var saved = deviceTokenPersistenceRepository.save(
                DeviceTokenPersistenceAssembler.toPersistenceFromDomain(deviceToken));
        return DeviceTokenPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public void deleteByToken(String token) {
        deviceTokenPersistenceRepository.findByToken(token).ifPresent(deviceTokenPersistenceRepository::delete);
    }
}
