package online.entreprenly.platform.notification.domain.model.valueobjects;

import java.util.Map;

/**
 * Push message delivered to a device.
 *
 * <p>{@code title} and {@code body} are shown by the system notification tray; {@code data} carries
 * the key/value pairs the app reads when the notification is opened (for example to navigate to a lot).</p>
 *
 * @param title notification title
 * @param body  notification text
 * @param data  string payload delivered with the notification
 */
public record PushMessage(String title, String body, Map<String, String> data) {

    public PushMessage {
        data = data == null ? Map.of() : Map.copyOf(data);
    }
}
