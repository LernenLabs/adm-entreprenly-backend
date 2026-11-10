package online.entreprenly.platform.notification.domain.model.commands;

/**
 * Command to stop sending push notifications to a device, e.g. when the user signs out or
 * revokes the notification permission.
 *
 * @param ownerEmail account email of the authenticated user
 * @param token      push registration token to remove
 */
public record UnregisterDeviceTokenCommand(String ownerEmail, String token) {
}
