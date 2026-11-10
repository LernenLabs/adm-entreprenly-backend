package online.entreprenly.platform.notification.domain.model.valueobjects;

/**
 * Counters describing one run of the lot expiry notice dispatch.
 *
 * @param lotsInWindow          lots that expire within the notice window and have stock
 * @param lotsNotified          lots for which at least one device received the notice
 * @param deliveries            total push messages accepted by the provider
 * @param skippedByPreference   lots skipped because the owner turned stock alerts off
 * @param skippedWithoutDevice  lots skipped because the owner has no registered device
 * @param skippedAlreadySent    lots skipped because they were already notified today
 * @param invalidTokensRemoved  device tokens removed after the provider rejected them
 */
public record LotExpiryDispatchSummary(int lotsInWindow, int lotsNotified, int deliveries,
                                       int skippedByPreference, int skippedWithoutDevice,
                                       int skippedAlreadySent, int invalidTokensRemoved) {
}
