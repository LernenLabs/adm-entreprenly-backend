package online.entreprenly.platform.notification.infrastructure.persistence.jpa.assemblers;

import online.entreprenly.platform.notification.domain.model.aggregates.DeviceToken;
import online.entreprenly.platform.notification.infrastructure.persistence.jpa.entities.DeviceTokenPersistenceEntity;

/**
 * Static assembler between device token domain and persistence representations.
 */
public final class DeviceTokenPersistenceAssembler {

    private DeviceTokenPersistenceAssembler() {
    }

    public static DeviceToken toDomainFromPersistence(DeviceTokenPersistenceEntity entity) {
        if (entity == null) return null;
        var deviceToken = new DeviceToken();
        deviceToken.restoreState(
                entity.getId(),
                entity.getOwnerEmail(),
                entity.getToken(),
                entity.getPlatform());
        return deviceToken;
    }

    public static DeviceTokenPersistenceEntity toPersistenceFromDomain(DeviceToken deviceToken) {
        if (deviceToken == null) return null;
        var entity = new DeviceTokenPersistenceEntity();
        if (deviceToken.getId() != null) {
            entity.setId(deviceToken.getId());
        }
        entity.setOwnerEmail(deviceToken.getOwnerEmail());
        entity.setToken(deviceToken.getToken());
        entity.setPlatform(deviceToken.getPlatform());
        return entity;
    }
}
