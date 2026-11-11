package online.entreprenly.platform.sales.domain.repositories;

import online.entreprenly.platform.sales.domain.model.aggregates.CashRegister;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * CashRegister repository port.
 *
 * <p>All operations are strictly scoped to the tenant/merchant account ({@code ownerEmail})
 * ensuring merchant isolation.</p>
 */
public interface CashRegisterRepository {

    /**
     * Finds the cash register of an owner on a specific date.
     */
    Optional<CashRegister> findByOwnerEmailAndDate(String ownerEmail, LocalDate date);

    /**
     * Finds the cash register of an owner and seller on a specific date.
     */
    Optional<CashRegister> findByOwnerEmailAndSellerIdAndDate(String ownerEmail, Long sellerId, LocalDate date);

    /**
     * Finds all cash registers for a given owner.
     */
    List<CashRegister> findAllByOwnerEmail(String ownerEmail);

    /**
     * Persists or updates the cash register aggregate.
     */
    CashRegister save(CashRegister cashRegister);
}
