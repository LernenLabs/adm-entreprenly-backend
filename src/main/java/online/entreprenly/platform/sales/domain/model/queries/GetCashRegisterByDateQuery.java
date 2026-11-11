package online.entreprenly.platform.sales.domain.model.queries;

import java.time.LocalDate;

/**
 * Query to retrieve a merchant's cash register for a given date and optional seller.
 *
 * @param ownerEmail the authenticated account email (tenant isolation)
 * @param sellerId   optional seller identifier
 * @param date       the business date (defaults to today in business timezone when null)
 */
public record GetCashRegisterByDateQuery(String ownerEmail, Long sellerId, LocalDate date) {

    public GetCashRegisterByDateQuery(String ownerEmail, LocalDate date) {
        this(ownerEmail, null, date);
    }
}
