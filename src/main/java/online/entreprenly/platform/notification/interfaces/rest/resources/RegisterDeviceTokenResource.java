package online.entreprenly.platform.notification.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body to register the push token of a device.
 */
public record RegisterDeviceTokenResource(
        @Schema(description = "FCM registration token of the device")
        @NotBlank @Size(max = 1024) String token,
        @Schema(description = "Device platform; only 'android' is supported", example = "android")
        String platform) {
}
