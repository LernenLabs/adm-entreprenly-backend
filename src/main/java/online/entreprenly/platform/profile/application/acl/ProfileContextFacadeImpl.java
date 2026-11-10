package online.entreprenly.platform.profile.application.acl;

import online.entreprenly.platform.iam.interfaces.acl.IamContextFacade;
import online.entreprenly.platform.profile.domain.model.valueobjects.NotificationSettings;
import online.entreprenly.platform.profile.domain.model.valueobjects.Preferences;
import online.entreprenly.platform.profile.domain.repositories.ProfileRepository;
import online.entreprenly.platform.profile.interfaces.acl.NotificationPreferences;
import online.entreprenly.platform.profile.interfaces.acl.ProfileContextFacade;
import org.springframework.stereotype.Service;

/**
 * Application-layer implementation of the Profile ACL facade.
 *
 * <p>Resolves the account's profile through its IAM user identifier and returns the notification
 * preferences, falling back to the defaults for accounts that have no profile yet.</p>
 */
@Service
public class ProfileContextFacadeImpl implements ProfileContextFacade {

    private final IamContextFacade iamContextFacade;
    private final ProfileRepository profileRepository;

    public ProfileContextFacadeImpl(IamContextFacade iamContextFacade, ProfileRepository profileRepository) {
        this.iamContextFacade = iamContextFacade;
        this.profileRepository = profileRepository;
    }

    @Override
    public NotificationPreferences fetchNotificationPreferences(String ownerEmail) {
        var defaults = new NotificationPreferences(
                NotificationSettings.defaults().stockAlerts(), Preferences.defaults().language());
        if (ownerEmail == null || ownerEmail.isBlank()) {
            return defaults;
        }
        Long userId = iamContextFacade.fetchUserIdByEmail(ownerEmail);
        if (userId == null || userId == 0L) {
            return defaults;
        }
        return profileRepository.findByUserId(userId)
                .map(profile -> new NotificationPreferences(
                        profile.getNotificationSettings() == null
                                ? defaults.stockAlerts()
                                : profile.getNotificationSettings().stockAlerts(),
                        profile.getPreferences() == null || profile.getPreferences().language() == null
                                ? defaults.language()
                                : profile.getPreferences().language()))
                .orElse(defaults);
    }
}
