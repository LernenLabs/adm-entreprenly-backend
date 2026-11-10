package online.entreprenly.platform.notification.support;

import online.entreprenly.platform.inventory.interfaces.acl.CatalogItem;
import online.entreprenly.platform.inventory.interfaces.acl.ExpiringLot;
import online.entreprenly.platform.inventory.interfaces.acl.InventoryContextFacade;
import online.entreprenly.platform.inventory.interfaces.acl.StockDeductionItem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StubInventoryContextFacade implements InventoryContextFacade {

    public final List<ExpiringLot> lots = new ArrayList<>();
    public LocalDate requestedFrom;
    public LocalDate requestedTo;

    @Override
    public List<CatalogItem> fetchCatalogByOwner(String ownerEmail) {
        return List.of();
    }

    @Override
    public void decrementStockForItems(String ownerEmail, List<StockDeductionItem> items) {
    }

    @Override
    public List<ExpiringLot> fetchLotsExpiringBetween(LocalDate from, LocalDate to) {
        requestedFrom = from;
        requestedTo = to;
        return lots.stream()
                .filter(l -> !l.expiryDate().isBefore(from) && !l.expiryDate().isAfter(to))
                .toList();
    }
}
