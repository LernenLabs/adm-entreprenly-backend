package online.entreprenly.platform.notification.domain.model.commands;

/**
 * Command to register (or refresh) a device push token for an account.
 *
 * @param ownerEmail account email of the authenticated user
 * @param token      push registration token issued to the device
 * @param platform   device platform; {@code android} when omitted
 */
public record RegisterDeviceTokenCommand(String ownerEmail, String token, String platform) {
}
