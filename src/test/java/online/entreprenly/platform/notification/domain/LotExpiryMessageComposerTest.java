package online.entreprenly.platform.notification.domain;

import online.entreprenly.platform.notification.domain.model.valueobjects.LotExpiryNotice;
import online.entreprenly.platform.notification.domain.services.LotExpiryMessageComposer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LotExpiryMessageComposerTest {

    private static LotExpiryNotice notice(long daysLeft, String code) {
        return new LotExpiryNotice("a@b.com", "unit", 7L, code, 3L, "Leche Gloria",
                LocalDate.of(2026, 11, 15), daysLeft);
    }

    @Test
    void includesProductDaysLeftAndExpiryDate() {
        var message = LotExpiryMessageComposer.compose(notice(3, "LOT-A"), "es");

        assertEquals("Lote próximo a vencer", message.title());
        assertTrue(message.body().contains("Leche Gloria"));
        assertTrue(message.body().contains("en 3 días"));
        assertTrue(message.body().contains("15/11/2026"));
        assertEquals("lot_expiring", message.data().get("type"));
        assertEquals("unit", message.data().get("lotType"));
        assertEquals("7", message.data().get("lotId"));
        assertEquals("3", message.data().get("daysLeft"));
        assertEquals("2026-11-15", message.data().get("expiryDate"));
        assertEquals("Leche Gloria", message.data().get("productName"));
    }

    @Test
    void usesTodayAndTomorrowWording() {
        assertTrue(LotExpiryMessageComposer.compose(notice(0, "X"), "es").body().contains("hoy"));
        assertTrue(LotExpiryMessageComposer.compose(notice(1, "X"), "es").body().contains("mañana"));
    }

    @Test
    void fallsBackToGeneratedLotCodeWhenMissing() {
        var message = LotExpiryMessageComposer.compose(notice(2, null), "es");

        assertEquals("L-0007", message.data().get("lotCode"));
        assertTrue(message.body().contains("L-0007"));
    }

    @Test
    void composesEnglishWhenLanguageIsNotSpanish() {
        var message = LotExpiryMessageComposer.compose(notice(4, "X"), "en");

        assertEquals("Lot expiring soon", message.title());
        assertTrue(message.body().contains("in 4 days"));
    }
}
