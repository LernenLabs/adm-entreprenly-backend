package online.entreprenly.platform.inventory.interfaces.acl;

import java.time.LocalDate;

/**
 * Read model exposed to other contexts describing a lot that is about to expire.
 *
 * @param ownerEmail  account that owns the lot
 * @param lotType     wire lot type ({@code unit}); only unit lots carry an expiry date
 * @param lotId       lot identifier, scoped to its lot type
 * @param lotCode     QR code of the lot, or {@code null} when it has none
 * @param productId   identifier of the product the lot belongs to
 * @param productName display name of the product, or {@code null} when it is unknown
 * @param expiryDate  day the lot expires (UTC)
 * @param quantity    units still in stock in the lot
 */
public record ExpiringLot(String ownerEmail, String lotType, Long lotId, String lotCode, Long productId,
                          String productName, LocalDate expiryDate, int quantity) {
}
