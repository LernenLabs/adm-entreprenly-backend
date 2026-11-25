package online.entreprenly.platform.inventory.infrastructure.persistence.jpa.repositories;

import online.entreprenly.platform.inventory.infrastructure.persistence.jpa.entities.ProductImagePersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data repository for uploaded product photos, keyed by their public access key.
 */
@Repository
public interface ProductImagePersistenceRepository extends JpaRepository<ProductImagePersistenceEntity, UUID> {
}
