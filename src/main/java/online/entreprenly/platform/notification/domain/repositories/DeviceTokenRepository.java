package online.entreprenly.platform.notification.domain.repositories;

import online.entreprenly.platform.notification.domain.model.aggregates.DeviceToken;

import java.util.List;
import java.util.Optional;

/**
 * Device token repository port.
 */
public interface DeviceTokenRepository {
    List<DeviceToken> findAllByOwnerEmail(String ownerEmail);

    Optional<DeviceToken> findByToken(String token);

    DeviceToken save(DeviceToken deviceToken);

    void deleteByToken(String token);
}
