package online.entreprenly.platform.inventory.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA persistence entity for a product photo uploaded by a seller.
 *
 * <p>The photo bytes live in the database (the container file system is ephemeral) and are served
 * publicly by their random {@code accessKey}, so a product can reference it as its image URL.</p>
 */
@Entity
@Table(name = "inventory_product_images")
@Getter
public class ProductImagePersistenceEntity {

    @Id
    private UUID accessKey;

    @Column(name = "owner_email", nullable = false, length = 120)
    private String ownerEmail;

    @Column(name = "mime_type", nullable = false, length = 32)
    private String mimeType;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "image", nullable = false, columnDefinition = "bytea")
    private byte[] image;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProductImagePersistenceEntity() {
    }

    public ProductImagePersistenceEntity(UUID accessKey, String ownerEmail, String mimeType, byte[] image,
                                         Instant createdAt) {
        this.accessKey = accessKey;
        this.ownerEmail = ownerEmail;
        this.mimeType = mimeType;
        this.image = image;
        this.createdAt = createdAt;
    }
}
