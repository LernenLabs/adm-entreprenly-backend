package online.entreprenly.platform.notification.domain.model.aggregates;

import online.entreprenly.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

/**
 * Device token aggregate root.
 *
 * <p>Registers a mobile device (its push registration token) so the account identified by
 * {@code ownerEmail} can receive push notifications on it. A token belongs to one account at a
 * time: when the same device signs in with another account, the token is reassigned.</p>
 */
public class DeviceToken extends AbstractDomainAggregateRoot<DeviceToken> {

    /** Platform value used by the Android client. */
    public static final String PLATFORM_ANDROID = "android";

    private Long id;
    private String ownerEmail;
    private String token;
    private String platform;

    public DeviceToken() {
    }

    public DeviceToken(String ownerEmail, String token, String platform) {
        this.ownerEmail = ownerEmail;
        this.token = token;
        this.platform = platform;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public String getToken() {
        return token;
    }

    public String getPlatform() {
        return platform;
    }

    /**
     * Assigns this device token to an account, e.g. when another user signs in on the same device.
     *
     * @param ownerEmail the account that now owns the token
     * @param platform   the device platform
     * @return this device token
     */
    public DeviceToken reassignTo(String ownerEmail, String platform) {
        this.ownerEmail = ownerEmail;
        this.platform = platform;
        return this;
    }

    /**
     * Restores an aggregate from persistence. Used by assemblers when reconstructing
     * a device token that already carries identity and full state.
     */
    public void restoreState(Long id, String ownerEmail, String token, String platform) {
        this.id = id;
        this.ownerEmail = ownerEmail;
        this.token = token;
        this.platform = platform;
    }
}
