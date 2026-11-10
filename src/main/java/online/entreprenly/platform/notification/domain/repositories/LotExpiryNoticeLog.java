package online.entreprenly.platform.notification.domain.repositories;

import java.time.LocalDate;

/**
 * Port that remembers which lots were already notified on a given day, so a run that is repeated
 * (a restart, a second instance, a manual trigger) does not send the same notice twice.
 */
public interface LotExpiryNoticeLog {
    boolean wasSent(String lotType, Long lotId, LocalDate noticeDate);

    void markSent(String ownerEmail, String lotType, Long lotId, LocalDate noticeDate);
}
