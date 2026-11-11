package online.entreprenly.platform.sales.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import online.entreprenly.platform.sales.application.queryservices.CashRegisterQueryService;
import online.entreprenly.platform.sales.domain.model.queries.GetCashRegisterByDateQuery;
import online.entreprenly.platform.sales.interfaces.rest.resources.CashRegisterResource;
import online.entreprenly.platform.sales.interfaces.rest.transform.CashRegisterResourceFromEntityAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST controller that exposes cash register resources for the Sales bounded context.
 *
 * <p>Enables merchants to query their accumulated daily cash register broken down by
 * payment method (cash vs digital channels: Yape, Plin, Card). All queries are strictly
 * isolated by the authenticated account email.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/cash-registers", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Cash Registers", description = "Daily cash register accumulated totals endpoints")
public class CashRegistersController {

    private final CashRegisterQueryService cashRegisterQueryService;

    public CashRegistersController(CashRegisterQueryService cashRegisterQueryService) {
        this.cashRegisterQueryService = cashRegisterQueryService;
    }

    @GetMapping
    @Operation(
        summary = "Get daily cash register totals",
        description = """
            Retrieves the accumulated cash register totals for the authenticated merchant,
            broken down by payment method (cash, Yape, Plin, Card, digital aggregate and grand total).
            When the optional 'date' parameter is omitted, the current business day's register is returned.
            When no sales have been registered yet today, a zeroed register is returned.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Cash register retrieved successfully",
            content = @Content(schema = @Schema(implementation = CashRegisterResource.class))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<CashRegisterResource> getCashRegister(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long sellerId) {

        var query = new GetCashRegisterByDateQuery(userDetails.getUsername(), sellerId, date);
        var cashRegister = cashRegisterQueryService.handle(query);
        return ResponseEntity.ok(CashRegisterResourceFromEntityAssembler.toResourceFromEntity(cashRegister));
    }
}

