package online.entreprenly.platform.inventory.interfaces.rest;

import online.entreprenly.platform.inventory.application.internal.outboundservices.ExternalProductCatalog;
import online.entreprenly.platform.inventory.application.internal.outboundservices.ExternalProductCatalog.ExternalCatalogUnavailableException;
import online.entreprenly.platform.inventory.interfaces.rest.resources.BarcodeLookupResource;
import online.entreprenly.platform.shared.interfaces.rest.resources.ErrorResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

/**
 * REST controller that suggests product data from the barcode printed on a package, so a seller
 * can register a product by scanning it and only add the price and stock.
 */
@RestController
@RequestMapping(value = "/api/v1/inventory-barcode-lookups", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Inventory Barcode Lookups", description = "Pre-fill a new product from its package barcode")
public class BarcodeLookupsController {

    /** EAN-8, UPC-E (8), UPC-A (12), EAN-13 and GTIN-14 barcodes. */
    private static final Pattern BARCODE = Pattern.compile("^(\\d{8}|\\d{12,14})$");

    private final ExternalProductCatalog externalProductCatalog;

    public BarcodeLookupsController(ExternalProductCatalog externalProductCatalog) {
        this.externalProductCatalog = externalProductCatalog;
    }

    @GetMapping("/{barcode}")
    @Operation(
        summary = "Look up a product by barcode",
        description = "Searches the barcode in Open Food Facts and returns a suggested name, brand, description "
                + "and photo for a new product. Nothing is saved: create the product with the usual endpoint.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product data found",
                    content = @Content(schema = @Schema(implementation = BarcodeLookupResource.class))),
            @ApiResponse(responseCode = "400", description = "Not a valid barcode",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "404", description = "No data for this barcode",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "503", description = "The product database is not available",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> lookup(@Parameter(description = "Barcode digits", example = "7750236173896")
                                    @PathVariable String barcode) {
        var code = barcode == null ? "" : barcode.strip();
        if (!BARCODE.matcher(code).matches()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResource("INVALID_BARCODE", "Use the 8, 12, 13 or 14 digits of the barcode."));
        }
        try {
            return externalProductCatalog.findByBarcode(code)
                    .<ResponseEntity<?>>map(info -> ResponseEntity.ok(new BarcodeLookupResource(info.barcode(),
                            info.name(), info.brand(), info.description(), info.imageUrl(), "Open Food Facts")))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ErrorResource("BARCODE_NOT_FOUND", "No product data for this barcode.")));
        } catch (ExternalCatalogUnavailableException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResource("LOOKUP_UNAVAILABLE", "The product database is not available right now."));
        }
    }
}
