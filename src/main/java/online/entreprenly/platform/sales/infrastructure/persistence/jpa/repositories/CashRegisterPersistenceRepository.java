package online.entreprenly.platform.sales.infrastructure.persistence.jpa.repositories;

import online.entreprenly.platform.sales.infrastructure.persistence.jpa.entities.CashRegisterPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for cash register persistence entities, scoped by owner account.
 */
@Repository
public interface CashRegisterPersistenceRepository extends JpaRepository<CashRegisterPersistenceEntity, Long> {

    List<CashRegisterPersistenceEntity> findAllByOwnerEmail(String ownerEmail);

    Optional<CashRegisterPersistenceEntity> findByOwnerEmailAndRegisterDate(String ownerEmail, LocalDate registerDate);

    Optional<CashRegisterPersistenceEntity> findByOwnerEmailAndSellerIdAndRegisterDate(
            String ownerEmail, Long sellerId, LocalDate registerDate);
}

