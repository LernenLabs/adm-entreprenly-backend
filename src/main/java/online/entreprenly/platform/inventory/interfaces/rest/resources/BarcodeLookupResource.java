package online.entreprenly.platform.inventory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Suggested data for a new product, found by the barcode printed on its package.
 */
@Schema(name = "BarcodeLookupResponse", description = "Product data suggested for a barcode")
public record BarcodeLookupResource(
    @Schema(description = "The looked-up barcode; use it as the product codeQR", example = "7750236173896")
    String barcode,

    @Schema(description = "Suggested product name", example = "Inca Kola Gaseosa X 500 ML. 500 ml")
    String name,

    @Schema(description = "Suggested brand", example = "Inca Kola")
    String brand,

    @Schema(description = "Suggested description", example = "Gaseosa")
    String description,

    @Schema(description = "Public URL of the product photo")
    String imageUrl,

    @Schema(description = "Where the data comes from", example = "Open Food Facts")
    String source
) {
}
