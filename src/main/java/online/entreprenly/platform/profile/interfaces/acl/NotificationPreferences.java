package online.entreprenly.platform.profile.interfaces.acl;

/**
 * Notification-related preferences of an account, exposed to other contexts.
 *
 * @param stockAlerts whether the user wants stock and expiry alerts
 * @param language    UI language code (for example {@code es} or {@code en}) used to localize messages
 */
public record NotificationPreferences(boolean stockAlerts, String language) {
}
