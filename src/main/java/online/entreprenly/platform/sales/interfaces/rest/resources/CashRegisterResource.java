package online.entreprenly.platform.sales.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Resource representing a daily cash register accumulated totals.
 */
@Schema(name = "CashRegister", description = "Daily cash register accumulated totals by payment method")
public record CashRegisterResource(
    @Schema(description = "Cash register record identifier", example = "1")
    Long id,

    @Schema(description = "Seller identifier", example = "1")
    Long sellerId,

    @Schema(description = "Merchant owner account email", example = "merchant@shop.com")
    String ownerEmail,

    @Schema(description = "Business date of the register", example = "2026-11-11")
    LocalDate date,

    @Schema(description = "Accumulated cash sales total", example = "150.0")
    double cashTotal,

    @Schema(description = "Accumulated Yape sales total", example = "50.0")
    double yapeTotal,

    @Schema(description = "Accumulated Plin sales total", example = "25.0")
    double plinTotal,

    @Schema(description = "Accumulated Card sales total", example = "30.0")
    double cardTotal,

    @Schema(description = "Accumulated digital sales total (Yape + Plin + Card)", example = "105.0")
    double digitalTotal,

    @Schema(description = "Grand total of all sales for the day", example = "255.0")
    double total,

    @Schema(description = "Total count of sales made today", example = "7")
    int salesCount,

    @Schema(description = "Count of cash sales", example = "4")
    int cashSalesCount,

    @Schema(description = "Count of digital sales", example = "3")
    int digitalSalesCount
) {

    // Extra aliases for frontend / Kotlin DTO compatibility
    @JsonProperty("cash")
    public double getCash() {
        return cashTotal;
    }

    @JsonProperty("digital")
    public double getDigital() {
        return digitalTotal;
    }

    @JsonProperty("totalAmount")
    public double getTotalAmount() {
        return total;
    }

    @JsonProperty("totalSales")
    public int getTotalSales() {
        return salesCount;
    }
}

