package online.entreprenly.platform.notification.domain.model.valueobjects;

import java.time.LocalDate;

/**
 * Facts about a lot that is about to expire, used to compose the push notice.
 *
 * @param ownerEmail  account that owns the lot
 * @param lotType     wire lot type ({@code unit})
 * @param lotId       lot identifier, scoped to its lot type
 * @param lotCode     QR code of the lot, or {@code null} when it has none
 * @param productId   identifier of the product the lot belongs to
 * @param productName display name of the product
 * @param expiryDate  day the lot expires
 * @param daysLeft    whole days between today and {@code expiryDate}
 */
public record LotExpiryNotice(String ownerEmail, String lotType, Long lotId, String lotCode, Long productId,
                              String productName, LocalDate expiryDate, long daysLeft) {
}
