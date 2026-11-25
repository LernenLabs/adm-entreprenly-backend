package online.entreprenly.platform.sales.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resource returned by the IoT scale status endpoint.
 *
 * @param connected {@code true} when the scale is reachable and ready to provide weight readings
 */
@Schema(name = "IotScaleStatusResponse", description = "Current connection status of the IoT scale peripheral")
public record IotScaleStatusResource(
    @Schema(description = "Whether the scale is connected and ready", example = "true")
    boolean connected
) {
}

