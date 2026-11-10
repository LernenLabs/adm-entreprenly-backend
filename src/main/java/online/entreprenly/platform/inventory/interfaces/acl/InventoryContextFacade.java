package online.entreprenly.platform.inventory.interfaces.acl;

import java.time.LocalDate;
import java.util.List;

/**
 * ACL facade that exposes Inventory bounded context capabilities to other contexts.
 *
 * <p>Provides a simplified integration surface for reading a seller's catalog with computed
 * stock and for deducting confirmed-order quantities, without leaking the Inventory internal
 * model.</p>
 */
public interface InventoryContextFacade {
    /**
     * Returns the catalog (products with price and computed stock) owned by a seller.
     *
     * @param ownerEmail the seller's account email that owns the products
     * @return the seller's catalog items, or an empty list when there are none
     */
    List<CatalogItem> fetchCatalogByOwner(String ownerEmail);

    /**
     * Deducts the given quantities from the seller's stock, consuming lots oldest-first.
     *
     * @param ownerEmail the seller's account email that owns the products
     * @param items      the products and quantities to deduct, matched by trimmed name
     */
    void decrementStockForItems(String ownerEmail, List<StockDeductionItem> items);

    /**
     * Returns the lots that still have stock and expire within the given days (both inclusive,
     * UTC), across every account.
     *
     * @param from first expiry day to include
     * @param to   last expiry day to include
     * @return the expiring lots with their product name, or an empty list when there are none
     */
    List<ExpiringLot> fetchLotsExpiringBetween(LocalDate from, LocalDate to);
}
