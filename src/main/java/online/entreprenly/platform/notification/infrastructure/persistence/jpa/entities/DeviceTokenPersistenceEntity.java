package online.entreprenly.platform.notification.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import online.entreprenly.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;

/**
 * JPA persistence entity for the push device tokens of an account.
 */
@Entity
@Table(name = "notification_device_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_device_tokens_token", columnNames = "token"))
@Getter
@Setter
@NoArgsConstructor
public class DeviceTokenPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "owner_email", nullable = false, length = 120)
    private String ownerEmail;

    @Column(name = "token", nullable = false, length = 1024)
    private String token;

    @Column(name = "platform", nullable = false, length = 20)
    private String platform;
}
