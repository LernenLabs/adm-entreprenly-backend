package online.entreprenly.platform.notification.interfaces.rest.resources;

/**
 * Registered device token as exposed by the API (the token itself is not echoed back).
 */
public record DeviceTokenResource(Long id, String platform) {
}
