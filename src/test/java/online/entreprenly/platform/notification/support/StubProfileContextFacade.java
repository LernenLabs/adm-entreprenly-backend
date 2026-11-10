package online.entreprenly.platform.notification.support;

import online.entreprenly.platform.profile.interfaces.acl.NotificationPreferences;
import online.entreprenly.platform.profile.interfaces.acl.ProfileContextFacade;

import java.util.HashMap;
import java.util.Map;

public class StubProfileContextFacade implements ProfileContextFacade {

    private final Map<String, NotificationPreferences> byOwner = new HashMap<>();

    public void set(String ownerEmail, boolean stockAlerts, String language) {
        byOwner.put(ownerEmail, new NotificationPreferences(stockAlerts, language));
    }

    @Override
    public NotificationPreferences fetchNotificationPreferences(String ownerEmail) {
        return byOwner.getOrDefault(ownerEmail, new NotificationPreferences(true, "es"));
    }
}
