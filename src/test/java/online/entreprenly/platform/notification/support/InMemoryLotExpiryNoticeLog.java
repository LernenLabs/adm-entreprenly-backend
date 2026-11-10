package online.entreprenly.platform.notification.support;

import online.entreprenly.platform.notification.domain.repositories.LotExpiryNoticeLog;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class InMemoryLotExpiryNoticeLog implements LotExpiryNoticeLog {

    private final Set<String> sent = new HashSet<>();

    @Override
    public boolean wasSent(String lotType, Long lotId, LocalDate noticeDate) {
        return sent.contains(key(lotType, lotId, noticeDate));
    }

    @Override
    public void markSent(String ownerEmail, String lotType, Long lotId, LocalDate noticeDate) {
        sent.add(key(lotType, lotId, noticeDate));
    }

    private static String key(String lotType, Long lotId, LocalDate noticeDate) {
        return lotType + ":" + lotId + ":" + noticeDate;
    }
}
