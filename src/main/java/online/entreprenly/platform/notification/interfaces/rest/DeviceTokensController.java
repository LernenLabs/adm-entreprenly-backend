package online.entreprenly.platform.notification.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.entreprenly.platform.notification.application.commandservices.DeviceTokenCommandService;
import online.entreprenly.platform.notification.domain.model.commands.UnregisterDeviceTokenCommand;
import online.entreprenly.platform.notification.interfaces.rest.resources.DeviceTokenResource;
import online.entreprenly.platform.notification.interfaces.rest.resources.RegisterDeviceTokenResource;
import online.entreprenly.platform.notification.interfaces.rest.transform.DeviceTokenResourceFromEntityAssembler;
import online.entreprenly.platform.notification.interfaces.rest.transform.RegisterDeviceTokenCommandFromResourceAssembler;
import online.entreprenly.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that lets the mobile app register or remove the push token of a device.
 */
@RestController
@RequestMapping(value = "/api/v1/notifications/device-tokens", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Notifications", description = "Push notification endpoints")
public class DeviceTokensController {

    private final DeviceTokenCommandService deviceTokenCommandService;

    public DeviceTokensController(DeviceTokenCommandService deviceTokenCommandService) {
        this.deviceTokenCommandService = deviceTokenCommandService;
    }

    @PostMapping
    @Operation(
        summary = "Register a device token",
        description = "Registers (or reassigns to the authenticated account) the push token of the device. "
                + "The app calls it once the notification permission is granted.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Device token registered",
                    content = @Content(schema = @Schema(implementation = DeviceTokenResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<?> registerDeviceToken(@AuthenticationPrincipal UserDetails userDetails,
                                                 @Valid @RequestBody RegisterDeviceTokenResource resource) {
        var command = RegisterDeviceTokenCommandFromResourceAssembler.toCommandFromResource(
                userDetails.getUsername(), resource);
        var result = deviceTokenCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result, DeviceTokenResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.CREATED);
    }

    @DeleteMapping
    @Operation(
        summary = "Remove a device token",
        description = "Stops sending pushes to the device. The app calls it when the notification permission "
                + "is revoked or the user signs out. Removing an unknown token is not an error.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Device token removed", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<?> unregisterDeviceToken(@AuthenticationPrincipal UserDetails userDetails,
                                                   @RequestParam("token") String token) {
        var result = deviceTokenCommandService.handle(
                new UnregisterDeviceTokenCommand(userDetails.getUsername(), token));
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result, removed -> null, HttpStatus.NO_CONTENT);
    }
}
