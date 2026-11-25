package online.entreprenly.platform.sales.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resource returned by the IoT scale weight-reading endpoint.
 *
 * @param weightKg the current weight reading in kilograms, rounded to 3 decimal places
 * @param unit     the unit of the weight reading; always {@code "kg"}
 */
@Schema(name = "IotScaleWeightResponse", description = "Current weight reading from the IoT scale peripheral")
public record IotScaleWeightResource(
    @Schema(description = "Current weight reading in kilograms", example = "1.234")
    double weightKg,

    @Schema(description = "Unit of the weight reading", example = "kg")
    String unit
) {
}

