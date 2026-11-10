package online.entreprenly.platform.notification.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import online.entreprenly.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;

import java.time.LocalDate;

/**
 * JPA persistence entity that records a lot expiry notice sent on a given day.
 */
@Entity
@Table(name = "notification_lot_expiry_logs",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_lot_expiry_logs_lot_day",
                columnNames = {"lot_type", "lot_id", "notice_date"}))
@Getter
@Setter
@NoArgsConstructor
public class LotExpiryNoticeLogPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "owner_email", nullable = false, length = 120)
    private String ownerEmail;

    @Column(name = "lot_type", nullable = false, length = 20)
    private String lotType;

    @Column(name = "lot_id", nullable = false)
    private Long lotId;

    @Column(name = "notice_date", nullable = false)
    private LocalDate noticeDate;
}
