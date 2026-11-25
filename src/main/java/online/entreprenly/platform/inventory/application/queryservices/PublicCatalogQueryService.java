package online.entreprenly.platform.inventory.application.queryservices;

import online.entreprenly.platform.inventory.domain.model.entities.CatalogProduct;
import online.entreprenly.platform.inventory.domain.model.queries.GetPublicCatalogQuery;

import java.util.List;

/**
 * Query service that builds the storefront catalog shown to customers.
 */
public interface PublicCatalogQueryService {
    /**
     * Returns every product of the store with its available stock, sorted by name.
     *
     * @param query the query naming the store owner
     * @return the catalog products, or an empty list when the store has none
     */
    List<CatalogProduct> handle(GetPublicCatalogQuery query);
}
