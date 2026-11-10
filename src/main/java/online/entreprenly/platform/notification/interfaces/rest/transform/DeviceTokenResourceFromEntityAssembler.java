package online.entreprenly.platform.notification.interfaces.rest.transform;

import online.entreprenly.platform.notification.domain.model.aggregates.DeviceToken;
import online.entreprenly.platform.notification.interfaces.rest.resources.DeviceTokenResource;

/**
 * Static assembler from the device token aggregate to its REST resource.
 */
public final class DeviceTokenResourceFromEntityAssembler {

    private DeviceTokenResourceFromEntityAssembler() {
    }

    public static DeviceTokenResource toResourceFromEntity(DeviceToken deviceToken) {
        return new DeviceTokenResource(deviceToken.getId(), deviceToken.getPlatform());
    }
}
