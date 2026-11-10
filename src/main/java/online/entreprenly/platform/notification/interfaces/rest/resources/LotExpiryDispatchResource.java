package online.entreprenly.platform.notification.interfaces.rest.resources;

/**
 * Summary of a lot expiry notice run.
 */
public record LotExpiryDispatchResource(
        int lotsInWindow,
        int lotsNotified,
        int deliveries,
        int skippedByPreference,
        int skippedWithoutDevice,
        int skippedAlreadySent,
        int invalidTokensRemoved) {
}
