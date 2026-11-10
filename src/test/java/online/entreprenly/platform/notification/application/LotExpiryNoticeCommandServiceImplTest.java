package online.entreprenly.platform.notification.application;

import online.entreprenly.platform.inventory.interfaces.acl.ExpiringLot;
import online.entreprenly.platform.notification.application.internal.commandservices.LotExpiryNoticeCommandServiceImpl;
import online.entreprenly.platform.notification.domain.model.aggregates.DeviceToken;
import online.entreprenly.platform.notification.domain.model.commands.DispatchLotExpiryNoticesCommand;
import online.entreprenly.platform.notification.domain.model.valueobjects.PushResult;
import online.entreprenly.platform.notification.support.InMemoryDeviceTokenRepository;
import online.entreprenly.platform.notification.support.InMemoryLotExpiryNoticeLog;
import online.entreprenly.platform.notification.support.RecordingPushNotificationSender;
import online.entreprenly.platform.notification.support.StubInventoryContextFacade;
import online.entreprenly.platform.notification.support.StubProfileContextFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LotExpiryNoticeCommandServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-10T13:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);
    private static final String OWNER = "owner@shop.com";

    private StubInventoryContextFacade inventory;
    private StubProfileContextFacade profiles;
    private InMemoryDeviceTokenRepository devices;
    private InMemoryLotExpiryNoticeLog log;
    private RecordingPushNotificationSender sender;
    private LotExpiryNoticeCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        inventory = new StubInventoryContextFacade();
        profiles = new StubProfileContextFacade();
        devices = new InMemoryDeviceTokenRepository();
        log = new InMemoryLotExpiryNoticeLog();
        sender = new RecordingPushNotificationSender();
        service = new LotExpiryNoticeCommandServiceImpl(inventory, profiles, devices, log, sender);
    }

    private static ExpiringLot lot(String owner, long id, int daysAhead) {
        return new ExpiringLot(owner, "unit", id, "LOT-" + id, 10L + id, "Producto " + id,
                TODAY.plusDays(daysAhead), 5);
    }

    private void device(String owner, String token) {
        devices.save(new DeviceToken(owner, token, DeviceToken.PLATFORM_ANDROID));
    }

    private DispatchLotExpiryNoticesCommand run(String owner, boolean force) {
        return new DispatchLotExpiryNoticesCommand(owner, force, NOW);
    }

    @Test
    void sendsNoticeWithProductDaysLeftAndExpiryDate() {
        inventory.lots.add(lot(OWNER, 1, 3));
        device(OWNER, "tok-1");

        var summary = service.handle(run(null, false));

        assertEquals(1, summary.lotsNotified());
        assertEquals(1, sender.sent().size());
        var message = sender.sent().get(0).message();
        assertEquals("tok-1", sender.sent().get(0).token());
        assertEquals("Producto 1", message.data().get("productName"));
        assertEquals("3", message.data().get("daysLeft"));
        assertEquals(TODAY.plusDays(3).toString(), message.data().get("expiryDate"));
        assertEquals("1", message.data().get("lotId"));
    }

    @Test
    void asksInventoryForTodayThroughFiveDaysAhead() {
        service.handle(run(null, false));

        assertEquals(TODAY, inventory.requestedFrom);
        assertEquals(TODAY.plusDays(5), inventory.requestedTo);
    }

    @Test
    void ignoresLotsBeyondFiveDays() {
        inventory.lots.add(lot(OWNER, 1, 6));
        device(OWNER, "tok-1");

        var summary = service.handle(run(null, false));

        assertEquals(0, summary.lotsInWindow());
        assertTrue(sender.sent().isEmpty());
    }

    @Test
    void doesNotSendWhenOwnerTurnedStockAlertsOff() {
        inventory.lots.add(lot(OWNER, 1, 2));
        device(OWNER, "tok-1");
        profiles.set(OWNER, false, "es");

        var summary = service.handle(run(null, false));

        assertEquals(1, summary.skippedByPreference());
        assertTrue(sender.sent().isEmpty());
    }

    @Test
    void doesNotSendWhenOwnerHasNoRegisteredDevice() {
        inventory.lots.add(lot(OWNER, 1, 2));

        var summary = service.handle(run(null, false));

        assertEquals(1, summary.skippedWithoutDevice());
        assertTrue(sender.sent().isEmpty());
    }

    @Test
    void notifiesEachLotOnlyOncePerDayUnlessForced() {
        inventory.lots.add(lot(OWNER, 1, 2));
        device(OWNER, "tok-1");

        service.handle(run(null, false));
        var second = service.handle(run(null, false));
        var forced = service.handle(run(null, true));

        assertEquals(1, second.skippedAlreadySent());
        assertEquals(2, sender.sent().size());
        assertEquals(1, forced.lotsNotified());
    }

    @Test
    void removesTokensRejectedAsInvalid() {
        inventory.lots.add(lot(OWNER, 1, 2));
        device(OWNER, "dead");
        device(OWNER, "alive");
        sender.respondWith("dead", PushResult.INVALID_TOKEN);

        var summary = service.handle(run(null, false));

        assertEquals(1, summary.invalidTokensRemoved());
        assertEquals(1, summary.deliveries());
        assertEquals(1, devices.all().size());
        assertEquals("alive", devices.all().get(0).getToken());
    }

    @Test
    void retriesOnNextRunWhenPushIsNotConfigured() {
        inventory.lots.add(lot(OWNER, 1, 2));
        device(OWNER, "tok-1");
        sender.respondWithByDefault(PushResult.NOT_CONFIGURED);

        var first = service.handle(run(null, false));
        sender.respondWithByDefault(PushResult.SENT);
        var second = service.handle(run(null, false));

        assertEquals(0, first.lotsNotified());
        assertEquals(1, second.lotsNotified());
    }

    @Test
    void canBeLimitedToOneOwner() {
        inventory.lots.add(lot(OWNER, 1, 2));
        inventory.lots.add(lot("other@shop.com", 2, 2));
        device(OWNER, "tok-1");
        device("other@shop.com", "tok-2");

        var summary = service.handle(run(OWNER, false));

        assertEquals(1, summary.lotsInWindow());
        assertEquals(1, sender.sent().size());
        assertEquals("tok-1", sender.sent().get(0).token());
    }

    @Test
    void writesEnglishMessageForEnglishAccounts() {
        inventory.lots.add(lot(OWNER, 1, 2));
        device(OWNER, "tok-1");
        profiles.set(OWNER, true, "en");

        service.handle(run(null, false));

        assertEquals("Lot expiring soon", sender.sent().get(0).message().title());
    }
}
