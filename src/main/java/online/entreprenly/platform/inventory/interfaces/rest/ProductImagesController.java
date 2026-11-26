package online.entreprenly.platform.inventory.interfaces.rest;

import online.entreprenly.platform.inventory.infrastructure.persistence.jpa.entities.ProductImagePersistenceEntity;
import online.entreprenly.platform.inventory.infrastructure.persistence.jpa.repositories.ProductImagePersistenceRepository;
import online.entreprenly.platform.inventory.interfaces.rest.resources.ProductImageResource;
import online.entreprenly.platform.shared.interfaces.rest.resources.ErrorResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import online.entreprenly.platform.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * REST controller for product photos uploaded from the seller's device.
 *
 * <p>A product's {@code imageUrl} accepts either an external link or the path returned here, so
 * sellers can paste a URL or upload a photo from their gallery. Uploading requires authentication;
 * reading a photo is public so the storefront can show it.</p>
 */
@RestController
@RequestMapping("/api/v1/inventory-product-images")
@Tag(name = "Inventory Product Images", description = "Upload product photos and serve them by URL")
public class ProductImagesController {

    static final String PATH = "/api/v1/inventory-product-images/";

    private final ProductImagePersistenceRepository images;

    public ProductImagesController(ProductImagePersistenceRepository images) {
        this.images = images;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    @Operation(
        summary = "Upload a product photo",
        description = "Stores a JPG, PNG or WebP photo (5 MB max) and returns its imageUrl. Send that value as "
                + "imageUrl when creating or updating a unit or weight product.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Photo stored",
                    content = @Content(schema = @Schema(implementation = ProductImageResource.class))),
            @ApiResponse(responseCode = "400", description = "Not a JPG, PNG or WebP image",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "413", description = "The photo is larger than 5 MB",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> upload(@AuthenticationPrincipal UserDetails userDetails,
                                    @Parameter(description = "Photo file (JPG, PNG or WebP, 5 MB max)")
                                    @RequestPart("image") MultipartFile image) throws IOException {
        if (image.getSize() > ProductImageFormat.MAX_BYTES) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(new ErrorResource("PAYLOAD_TOO_LARGE", "The photo must be 5 MB or less."));
        }
        var bytes = image.getBytes();
        String mimeType;
        try {
            mimeType = ProductImageFormat.detect(bytes);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResource("INVALID_IMAGE", ex.getMessage()));
        }
        var key = UUID.randomUUID();
        images.save(new ProductImagePersistenceEntity(key, UserDetailsImpl.ownerEmail(userDetails), mimeType, bytes, Instant.now()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ProductImageResource(PATH + key, mimeType, bytes.length));
    }

    @GetMapping("/{key}")
    @Operation(
        summary = "Get a product photo",
        description = "Returns the photo bytes. Public, so customers and the storefront can display it."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Photo found",
                    content = {@Content(mediaType = "image/jpeg"), @Content(mediaType = "image/png"),
                            @Content(mediaType = "image/webp")}),
            @ApiResponse(responseCode = "404", description = "Photo not found", content = @Content)
    })
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> get(@PathVariable UUID key) {
        // Keys are random and never reused, so the photo can be cached for a long time.
        return images.findById(key)
                .map(photo -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(photo.getMimeType()))
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                        .header("X-Content-Type-Options", "nosniff")
                        .body(photo.getImage()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
