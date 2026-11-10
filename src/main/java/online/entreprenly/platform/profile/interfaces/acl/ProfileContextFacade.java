package online.entreprenly.platform.profile.interfaces.acl;

/**
 * ACL facade that exposes Profile bounded context capabilities to other contexts.
 *
 * <p>Provides a simplified integration surface for reading a user's notification preferences
 * without leaking the Profile internal model.</p>
 */
public interface ProfileContextFacade {
    /**
     * Returns the notification preferences of the account with the given email.
     *
     * @param ownerEmail account email
     * @return the stored preferences, or the defaults (alerts on, English) when the account has no profile
     */
    NotificationPreferences fetchNotificationPreferences(String ownerEmail);
}
