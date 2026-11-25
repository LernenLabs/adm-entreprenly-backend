package online.entreprenly.platform.inventory.interfaces.rest;

import online.entreprenly.platform.inventory.application.queryservices.PublicCatalogQueryService;
import online.entreprenly.platform.inventory.domain.model.queries.GetPublicCatalogQuery;
import online.entreprenly.platform.inventory.interfaces.rest.resources.PublicCatalogProductResource;
import online.entreprenly.platform.inventory.interfaces.rest.transform.PublicCatalogProductResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public, read-only REST controller for the storefront catalog consumed by the customer app.
 *
 * <p>It needs no authentication and never exposes owner data, lots or QR codes: only what a
 * customer needs to browse and build an order.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/public-catalog", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Public Catalog", description = "Read-only storefront catalog for customers (no authentication)")
public class PublicCatalogController {

    private final PublicCatalogQueryService publicCatalogQueryService;
    private final String storeOwnerEmail;

    public PublicCatalogController(PublicCatalogQueryService publicCatalogQueryService,
                                   @Value("${inventory.public-catalog.owner-email:test.beta@entreprenly.online}")
                                   String storeOwnerEmail) {
        this.publicCatalogQueryService = publicCatalogQueryService;
        this.storeOwnerEmail = storeOwnerEmail;
    }

    @GetMapping
    @Operation(
        summary = "List the storefront catalog",
        description = "Retrieves the products of the demo store with photo, description, price, sales unit and "
                + "available stock, sorted by name. No authentication is required."
    )
    @ApiResponse(responseCode = "200", description = "Catalog found",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = PublicCatalogProductResource.class))))
    public ResponseEntity<List<PublicCatalogProductResource>> getCatalog() {
        var resources = publicCatalogQueryService.handle(new GetPublicCatalogQuery(storeOwnerEmail)).stream()
                .map(PublicCatalogProductResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }
}
