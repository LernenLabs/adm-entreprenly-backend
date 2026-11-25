package online.entreprenly.platform.inventory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resource representing a storefront product returned by the public catalog.
 */
@Schema(name = "PublicCatalogProductResponse", description = "A store product as shown to customers")
public record PublicCatalogProductResource(
    @Schema(description = "Product identifier, unique within its product type", example = "1")
    Long id,

    @Schema(description = "Measurement type", example = "unit", allowableValues = {"unit", "weight"})
    String productType,

    @Schema(description = "Product display name", example = "Inca Kola 500 ml")
    String name,

    @Schema(description = "Product description", example = "Gaseosa sabor original en botella personal")
    String description,

    @Schema(description = "Product brand, only for unit products", example = "Inca Kola")
    String brand,

    @Schema(description = "Public URL of the product photo")
    String imageUrl,

    @Schema(description = "Unit price, or price per kilogram for weight products", example = "2.8")
    double price,

    @Schema(description = "Sales unit", example = "unidad", allowableValues = {"unidad", "kg"})
    String unit,

    @Schema(description = "Available stock, in units or kilograms", example = "36")
    double stock,

    @Schema(description = "Whether the product has stock available", example = "true")
    boolean available
) {
}
