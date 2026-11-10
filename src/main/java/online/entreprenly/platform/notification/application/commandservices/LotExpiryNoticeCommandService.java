package online.entreprenly.platform.notification.application.commandservices;

import online.entreprenly.platform.notification.domain.model.commands.DispatchLotExpiryNoticesCommand;
import online.entreprenly.platform.notification.domain.model.valueobjects.LotExpiryDispatchSummary;

/**
 * Command service that delivers the lot expiry notices that are due.
 */
public interface LotExpiryNoticeCommandService {
    /**
     * Finds lots that expire within the next five days (including today) and sends one push notice per
     * lot to every device of its owner, honoring the owner's stock alert preference.
     *
     * @param command the dispatch parameters
     * @return counters describing what was sent and why other lots were skipped
     */
    LotExpiryDispatchSummary handle(DispatchLotExpiryNoticesCommand command);
}
