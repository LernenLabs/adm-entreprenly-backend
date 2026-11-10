package online.entreprenly.platform.notification.application.commandservices;

import online.entreprenly.platform.notification.domain.model.aggregates.DeviceToken;
import online.entreprenly.platform.notification.domain.model.commands.RegisterDeviceTokenCommand;
import online.entreprenly.platform.notification.domain.model.commands.UnregisterDeviceTokenCommand;
import online.entreprenly.platform.shared.application.result.ApplicationError;
import online.entreprenly.platform.shared.application.result.Result;

/**
 * Command service for the push device tokens of an account.
 */
public interface DeviceTokenCommandService {
    /**
     * Registers a device token for the account, or reassigns it when another account used it before.
     *
     * @param command the registration data
     * @return the stored device token, or a validation error
     */
    Result<DeviceToken, ApplicationError> handle(RegisterDeviceTokenCommand command);

    /**
     * Removes a device token owned by the account. Removing a token that does not exist is not an error.
     *
     * @param command the token to remove
     * @return {@code true} when a token was removed, {@code false} when there was nothing to remove
     */
    Result<Boolean, ApplicationError> handle(UnregisterDeviceTokenCommand command);
}
