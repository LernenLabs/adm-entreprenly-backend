package online.entreprenly.platform.inventory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resource returned after uploading a product photo.
 */
@Schema(name = "ProductImageResponse", description = "An uploaded product photo, ready to be used as imageUrl")
public record ProductImageResource(
    @Schema(description = "Path of the photo, relative to the API base URL. Put it in the product imageUrl.",
            example = "/api/v1/inventory-product-images/3f2b6c1e-8a4d-4c55-9a3e-1d2f6b7c8e90")
    String imageUrl,

    @Schema(description = "Detected image type", example = "image/jpeg")
    String mimeType,

    @Schema(description = "Size of the photo in bytes", example = "184233")
    long sizeBytes
) {
}
