package online.entreprenly.platform.notification.interfaces.rest.transform;

import online.entreprenly.platform.notification.domain.model.commands.RegisterDeviceTokenCommand;
import online.entreprenly.platform.notification.interfaces.rest.resources.RegisterDeviceTokenResource;

/**
 * Static assembler from the register-token resource to its command.
 */
public final class RegisterDeviceTokenCommandFromResourceAssembler {

    private RegisterDeviceTokenCommandFromResourceAssembler() {
    }

    public static RegisterDeviceTokenCommand toCommandFromResource(String ownerEmail,
                                                                   RegisterDeviceTokenResource resource) {
        return new RegisterDeviceTokenCommand(ownerEmail, resource.token(), resource.platform());
    }
}
