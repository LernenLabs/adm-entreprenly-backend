package online.entreprenly.platform.notification.infrastructure.persistence.jpa.repositories;

import online.entreprenly.platform.notification.infrastructure.persistence.jpa.entities.LotExpiryNoticeLogPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * Spring Data repository for lot expiry notice log entries.
 */
@Repository
public interface LotExpiryNoticeLogPersistenceRepository
        extends JpaRepository<LotExpiryNoticeLogPersistenceEntity, Long> {
    boolean existsByLotTypeAndLotIdAndNoticeDate(String lotType, Long lotId, LocalDate noticeDate);
}
