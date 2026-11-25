package online.entreprenly.platform.inventory.interfaces.rest.transform;

import online.entreprenly.platform.inventory.domain.model.entities.CatalogProduct;
import online.entreprenly.platform.inventory.domain.model.valueobjects.ProductType;
import online.entreprenly.platform.inventory.interfaces.rest.resources.PublicCatalogProductResource;

/**
 * Assembler that converts {@link CatalogProduct} read models into {@link PublicCatalogProductResource} objects.
 */
public class PublicCatalogProductResourceFromEntityAssembler {

    public static PublicCatalogProductResource toResourceFromEntity(CatalogProduct product) {
        var byWeight = product.productType() == ProductType.WEIGHT;
        return new PublicCatalogProductResource(
                product.id(),
                product.productType().getValue(),
                product.name(),
                product.description(),
                product.brand(),
                product.imageUrl(),
                product.price(),
                byWeight ? "kg" : "unidad",
                product.stock(),
                product.stock() > 0);
    }
}
