package online.entreprenly.platform.notification.interfaces.rest.transform;

import online.entreprenly.platform.notification.domain.model.valueobjects.LotExpiryDispatchSummary;
import online.entreprenly.platform.notification.interfaces.rest.resources.LotExpiryDispatchResource;

/**
 * Static assembler from the dispatch summary to its REST resource.
 */
public final class LotExpiryDispatchResourceFromSummaryAssembler {

    private LotExpiryDispatchResourceFromSummaryAssembler() {
    }

    public static LotExpiryDispatchResource toResourceFromSummary(LotExpiryDispatchSummary summary) {
        return new LotExpiryDispatchResource(
                summary.lotsInWindow(),
                summary.lotsNotified(),
                summary.deliveries(),
                summary.skippedByPreference(),
                summary.skippedWithoutDevice(),
                summary.skippedAlreadySent(),
                summary.invalidTokensRemoved());
    }
}
