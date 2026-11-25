package online.entreprenly.platform.sales.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import online.entreprenly.platform.sales.application.queryservices.IotScaleQueryService;
import online.entreprenly.platform.sales.domain.model.queries.GetScaleStatusQuery;
import online.entreprenly.platform.sales.domain.model.queries.GetScaleWeightQuery;
import online.entreprenly.platform.sales.domain.model.valueobjects.ScaleStatus;
import online.entreprenly.platform.sales.interfaces.rest.resources.IotScaleStatusResource;
import online.entreprenly.platform.sales.interfaces.rest.resources.IotScaleWeightResource;
import online.entreprenly.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that exposes the IoT scale integration contract for the Sales bounded context.
 *
 * <p>Two sub-resources are defined:
 * <ul>
 *   <li>{@code GET /api/v1/iot-scale} – returns the current connection status of the scale.</li>
 *   <li>{@code GET /api/v1/iot-scale/weight} – returns the current weight reading in kg.</li>
 * </ul>
 * </p>
 *
 * <p>Both endpoints require a valid JWT bearer token. The Android Kotlin client uses these
 * endpoints to drive the scale-integration dialog: it polls status first, then reads the
 * weight, applies the 800 ms auto-confirm window and falls back to manual entry when the
 * scale is disconnected or weight exceeds the available stock.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/iot-scale", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "IoT Scale", description = "IoT scale connection status and weight reading endpoints for Sales")
public class IotScaleController {

    private final IotScaleQueryService iotScaleQueryService;

    public IotScaleController(IotScaleQueryService iotScaleQueryService) {
        this.iotScaleQueryService = iotScaleQueryService;
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/iot-scale  –  connection status
    // -------------------------------------------------------------------------

    @GetMapping
    @Operation(
        summary = "Get IoT scale connection status",
        description = """
            Returns whether the IoT scale peripheral is currently connected and
            ready to provide weight readings. The Android client must call this
            endpoint before opening the scale dialog to decide whether to show
            the scale UI or go directly to the manual-entry fallback.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Status retrieved successfully",
            content = @Content(schema = @Schema(implementation = IotScaleStatusResource.class))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<IotScaleStatusResource> getScaleStatus(
            @AuthenticationPrincipal UserDetails userDetails) {

        var result = iotScaleQueryService.handle(new GetScaleStatusQuery(userDetails.getUsername()));
        return result.toOptional()
                .map(status -> new IotScaleStatusResource(status == ScaleStatus.CONNECTED))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(new IotScaleStatusResource(false)));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/iot-scale/weight  –  weight reading
    // -------------------------------------------------------------------------

    @GetMapping("/weight")
    @Operation(
        summary = "Read weight from IoT scale",
        description = """
            Returns the current weight reading in kilograms from the connected IoT scale.
            The Android client calls this endpoint after confirming the scale is connected.
            The returned weight is used to:
            (1) pre-fill the weight field in the sale dialog,
            (2) trigger automatic confirmation after 800 ms of stable reading,
            (3) validate that the weight does not exceed the available stock before
                adding the line item.
            When the scale is disconnected or the bridge is unreachable, a 422 error
            is returned and the Android client must switch to manual weight entry.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Weight reading retrieved successfully",
            content = @Content(schema = @Schema(implementation = IotScaleWeightResource.class))
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Scale disconnected or reading unavailable – use manual entry",
            content = @Content
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<?> getScaleWeight(
            @AuthenticationPrincipal UserDetails userDetails) {

        var result = iotScaleQueryService.handle(new GetScaleWeightQuery(userDetails.getUsername()));
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                weightKg -> new IotScaleWeightResource(weightKg, "kg"),
                HttpStatus.OK);
    }
}

