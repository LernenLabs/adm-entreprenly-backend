package online.entreprenly.platform.notification.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import online.entreprenly.platform.notification.application.commandservices.LotExpiryNoticeCommandService;
import online.entreprenly.platform.notification.domain.model.commands.DispatchLotExpiryNoticesCommand;
import online.entreprenly.platform.notification.interfaces.rest.resources.LotExpiryDispatchResource;
import online.entreprenly.platform.notification.interfaces.rest.transform.LotExpiryDispatchResourceFromSummaryAssembler;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * REST controller to run the lot expiry notices on demand for the authenticated account.
 */
@RestController
@RequestMapping(value = "/api/v1/notifications/lot-expiry", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Notifications", description = "Push notification endpoints")
public class LotExpiryNoticesController {

    private final LotExpiryNoticeCommandService lotExpiryNoticeCommandService;

    public LotExpiryNoticesController(LotExpiryNoticeCommandService lotExpiryNoticeCommandService) {
        this.lotExpiryNoticeCommandService = lotExpiryNoticeCommandService;
    }

    @PostMapping("/dispatch")
    @Operation(
        summary = "Send the lot expiry notices now",
        description = "Runs the daily expiry check only for the authenticated account (lots expiring within "
                + "the next 5 days). Useful to verify the flow; with force=true lots already notified today "
                + "are notified again.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Run summary",
                    content = @Content(schema = @Schema(implementation = LotExpiryDispatchResource.class)))
    })
    public ResponseEntity<LotExpiryDispatchResource> dispatch(@AuthenticationPrincipal UserDetails userDetails,
                                                              @RequestParam(defaultValue = "false") boolean force) {
        var summary = lotExpiryNoticeCommandService.handle(
                new DispatchLotExpiryNoticesCommand(userDetails.getUsername(), force, Instant.now()));
        return ResponseEntity.ok(LotExpiryDispatchResourceFromSummaryAssembler.toResourceFromSummary(summary));
    }
}
