package online.entreprenly.platform.profile.interfaces.rest.transform;

import online.entreprenly.platform.iam.domain.repositories.UserRepository;
import online.entreprenly.platform.profile.domain.model.aggregates.Profile;
import org.springframework.stereotype.Component;
import online.entreprenly.platform.profile.interfaces.rest.resources.NotificationSettingsResource;
import online.entreprenly.platform.profile.interfaces.rest.resources.PreferencesResource;
import online.entreprenly.platform.profile.interfaces.rest.resources.ProfileResource;

/**
 * Assembler that converts {@link Profile} aggregates into {@link ProfileResource} objects.
 */
@Component
public class ProfileResourceFromEntityAssembler {
    private final UserRepository users;

    public ProfileResourceFromEntityAssembler(UserRepository users) {
        this.users = users;
    }

    /**
     * Converts a profile aggregate to its REST representation.
     *
     * @param profile profile aggregate
     * @return profile resource
     */
    public ProfileResource toResourceFromEntity(Profile profile) {
        var preferences = new PreferencesResource(
                profile.getPreferences().language(),
                profile.getPreferences().timezone(),
                profile.getPreferences().theme(),
                profile.getPreferences().currency());
        var notificationSettings = new NotificationSettingsResource(
                profile.getNotificationSettings().stockAlerts());
        return new ProfileResource(
                profile.getId(),
                profile.getUserId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPhone(),
                profile.getPhoneVerifiedAt() != null,
                users.findById(profile.getUserId()).filter(owner -> owner.getEmailVerifiedAt() != null).isPresent(),
                profile.getAvatarUrl(),
                profile.getBiography(),
                profile.getRole(),
                profile.getPlan(),
                preferences,
                notificationSettings);
    }
}
