package online.entreprenly.platform.inventory.domain.model.entities;

import online.entreprenly.platform.inventory.domain.model.valueobjects.ProductType;

/**
 * Storefront read model of an inventory product.
 *
 * <p>Joins a unit or weight product with the stock available across its lots, so customers can
 * browse a store's catalog without seeing internal lot data. It is never persisted.</p>
 *
 * @param id          the product identifier, unique within its {@link ProductType}
 * @param productType whether the product is sold per unit or by weight
 * @param name        the product display name
 * @param description the product description
 * @param brand       the product brand, only for unit products
 * @param imageUrl    the public URL of the product photo
 * @param price       the unit price, or the price per kilogram for weight products
 * @param stock       the available stock, in units or kilograms
 */
public record CatalogProduct(Long id, ProductType productType, String name, String description, String brand,
                             String imageUrl, double price, double stock) {
}
