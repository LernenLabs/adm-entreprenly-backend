package online.entreprenly.platform.notification.domain.model.commands;

import java.time.Instant;

/**
 * Command to deliver the expiry notices that are due.
 *
 * @param ownerEmail restricts the run to one account; {@code null} processes every account
 * @param force      when {@code true}, lots already notified today are notified again
 * @param now        reference instant used to compute the days left
 */
public record DispatchLotExpiryNoticesCommand(String ownerEmail, boolean force, Instant now) {
}
