package online.entreprenly.platform.sales.infrastructure.persistence.jpa.adapters;

import online.entreprenly.platform.sales.domain.model.aggregates.CashRegister;
import online.entreprenly.platform.sales.domain.repositories.CashRegisterRepository;
import online.entreprenly.platform.sales.infrastructure.persistence.jpa.assemblers.CashRegisterPersistenceAssembler;
import online.entreprenly.platform.sales.infrastructure.persistence.jpa.repositories.CashRegisterPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository adapter bridging the CashRegister domain repository port with Spring Data JPA.
 */
@Repository
public class CashRegisterRepositoryImpl implements CashRegisterRepository {

    private final CashRegisterPersistenceRepository persistenceRepository;

    public CashRegisterRepositoryImpl(CashRegisterPersistenceRepository persistenceRepository) {
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public Optional<CashRegister> findByOwnerEmailAndDate(String ownerEmail, LocalDate date) {
        return persistenceRepository.findByOwnerEmailAndRegisterDate(ownerEmail, date)
                .map(CashRegisterPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<CashRegister> findByOwnerEmailAndSellerIdAndDate(String ownerEmail, Long sellerId, LocalDate date) {
        return persistenceRepository.findByOwnerEmailAndSellerIdAndRegisterDate(ownerEmail, sellerId, date)
                .map(CashRegisterPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<CashRegister> findAllByOwnerEmail(String ownerEmail) {
        return persistenceRepository.findAllByOwnerEmail(ownerEmail).stream()
                .map(CashRegisterPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public CashRegister save(CashRegister cashRegister) {
        var entity = CashRegisterPersistenceAssembler.toPersistenceFromDomain(cashRegister);
        var saved = persistenceRepository.save(entity);
        return CashRegisterPersistenceAssembler.toDomainFromPersistence(saved);
    }
}

