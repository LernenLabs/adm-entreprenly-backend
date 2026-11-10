package online.entreprenly.platform.notification.domain.services;

import online.entreprenly.platform.notification.domain.model.valueobjects.LotExpiryNotice;
import online.entreprenly.platform.notification.domain.model.valueobjects.PushMessage;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Domain service that composes the push message for a lot that is about to expire.
 *
 * <p>The text always names the product, the days left and the expiry date. The {@code data}
 * payload carries what the Android app needs to open the lot detail when the notification is tapped:
 * {@code type}, {@code lotType} and {@code lotId} (plus display fields).</p>
 */
public final class LotExpiryMessageComposer {

    /** Value of the {@code type} data key identifying a lot expiry notification. */
    public static final String TYPE_LOT_EXPIRING = "lot_expiring";

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);

    private LotExpiryMessageComposer() {
    }

    /**
     * Composes the push message for a lot expiry notice.
     *
     * @param notice   facts about the lot
     * @param language user language code; Spanish when it starts with {@code es}, English otherwise
     * @return the message to deliver
     */
    public static PushMessage compose(LotExpiryNotice notice, String language) {
        boolean spanish = language != null && language.toLowerCase(Locale.ROOT).startsWith("es");
        String product = isBlank(notice.productName())
                ? (spanish ? "Producto" : "Product")
                : notice.productName().trim();
        String lot = isBlank(notice.lotCode())
                ? "L-%04d".formatted(notice.lotId())
                : notice.lotCode().trim();
        String date = DISPLAY_DATE.format(notice.expiryDate());
        String when = whenLabel(notice.daysLeft(), spanish);

        String title = spanish ? "Lote próximo a vencer" : "Lot expiring soon";
        String body = spanish
                ? "%s: el lote %s vence %s (%s).".formatted(product, lot, when, date)
                : "%s: lot %s expires %s (%s).".formatted(product, lot, when, date);

        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", TYPE_LOT_EXPIRING);
        data.put("lotType", notice.lotType());
        data.put("lotId", String.valueOf(notice.lotId()));
        data.put("lotCode", lot);
        data.put("productId", String.valueOf(notice.productId()));
        data.put("productName", product);
        data.put("daysLeft", String.valueOf(notice.daysLeft()));
        data.put("expiryDate", notice.expiryDate().toString());
        return new PushMessage(title, body, data);
    }

    private static String whenLabel(long daysLeft, boolean spanish) {
        if (daysLeft <= 0) {
            return spanish ? "hoy" : "today";
        }
        if (daysLeft == 1) {
            return spanish ? "mañana" : "tomorrow";
        }
        return spanish ? "en %d días".formatted(daysLeft) : "in %d days".formatted(daysLeft);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
