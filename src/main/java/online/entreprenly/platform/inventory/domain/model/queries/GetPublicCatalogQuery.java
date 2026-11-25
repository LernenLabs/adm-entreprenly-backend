package online.entreprenly.platform.inventory.domain.model.queries;

/**
 * Query to get the storefront catalog of a store.
 *
 * @param ownerEmail the email of the account that owns the store
 */
public record GetPublicCatalogQuery(String ownerEmail) {
}
