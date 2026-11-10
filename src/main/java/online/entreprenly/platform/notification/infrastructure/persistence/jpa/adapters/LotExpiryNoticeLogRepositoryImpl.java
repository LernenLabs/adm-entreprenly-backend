package online.entreprenly.platform.notification.infrastructure.persistence.jpa.adapters;

import online.entreprenly.platform.notification.domain.repositories.LotExpiryNoticeLog;
import online.entreprenly.platform.notification.infrastructure.persistence.jpa.entities.LotExpiryNoticeLogPersistenceEntity;
import online.entreprenly.platform.notification.infrastructure.persistence.jpa.repositories.LotExpiryNoticeLogPersistenceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * Adapter that stores the lot expiry notices already sent, using Spring Data JPA.
 */
@Repository
public class LotExpiryNoticeLogRepositoryImpl implements LotExpiryNoticeLog {

    private final LotExpiryNoticeLogPersistenceRepository persistenceRepository;

    public LotExpiryNoticeLogRepositoryImpl(LotExpiryNoticeLogPersistenceRepository persistenceRepository) {
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public boolean wasSent(String lotType, Long lotId, LocalDate noticeDate) {
        return persistenceRepository.existsByLotTypeAndLotIdAndNoticeDate(lotType, lotId, noticeDate);
    }

    @Override
    public void markSent(String ownerEmail, String lotType, Long lotId, LocalDate noticeDate) {
        if (wasSent(lotType, lotId, noticeDate)) {
            return;
        }
        var entity = new LotExpiryNoticeLogPersistenceEntity();
        entity.setOwnerEmail(ownerEmail);
        entity.setLotType(lotType);
        entity.setLotId(lotId);
        entity.setNoticeDate(noticeDate);
        try {
            persistenceRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            // Another run recorded the same lot and day first; nothing to do.
        }
    }
}
