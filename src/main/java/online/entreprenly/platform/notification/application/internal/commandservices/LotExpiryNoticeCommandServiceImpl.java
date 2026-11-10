package online.entreprenly.platform.notification.application.internal.commandservices;

import online.entreprenly.platform.inventory.interfaces.acl.ExpiringLot;
import online.entreprenly.platform.inventory.interfaces.acl.InventoryContextFacade;
import online.entreprenly.platform.notification.application.commandservices.LotExpiryNoticeCommandService;
import online.entreprenly.platform.notification.application.internal.outboundservices.PushNotificationSender;
import online.entreprenly.platform.notification.domain.model.aggregates.DeviceToken;
import online.entreprenly.platform.notification.domain.model.commands.DispatchLotExpiryNoticesCommand;
import online.entreprenly.platform.notification.domain.model.valueobjects.LotExpiryDispatchSummary;
import online.entreprenly.platform.notification.domain.model.valueobjects.LotExpiryNotice;
import online.entreprenly.platform.notification.domain.model.valueobjects.PushResult;
import online.entreprenly.platform.notification.domain.repositories.DeviceTokenRepository;
import online.entreprenly.platform.notification.domain.repositories.LotExpiryNoticeLog;
import online.entreprenly.platform.notification.domain.services.LotExpiryMessageComposer;
import online.entreprenly.platform.profile.interfaces.acl.NotificationPreferences;
import online.entreprenly.platform.profile.interfaces.acl.ProfileContextFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Delivers push notices for lots that expire within the next five days.
 *
 * <p>The window mirrors the inventory alert rule: from today up to five days ahead, counted on UTC
 * dates. Lots without stock are ignored (they show as out of stock instead). Each lot is notified at
 * most once per day, only to owners who keep stock alerts on, and only to their registered devices.
 * A lot counts as notified only when at least one device accepted the message, so a failed or
 * unconfigured run is retried on the next one.</p>
 */
@Service
public class LotExpiryNoticeCommandServiceImpl implements LotExpiryNoticeCommandService {

    /** Number of days ahead (after today) that still trigger a notice. */
    static final int EXPIRING_SOON_DAYS = 5;

    private static final Logger log = LoggerFactory.getLogger(LotExpiryNoticeCommandServiceImpl.class);

    private final InventoryContextFacade inventoryContextFacade;
    private final ProfileContextFacade profileContextFacade;
    private final DeviceTokenRepository deviceTokenRepository;
    private final LotExpiryNoticeLog noticeLog;
    private final PushNotificationSender pushNotificationSender;

    public LotExpiryNoticeCommandServiceImpl(InventoryContextFacade inventoryContextFacade,
                                             ProfileContextFacade profileContextFacade,
                                             DeviceTokenRepository deviceTokenRepository,
                                             LotExpiryNoticeLog noticeLog,
                                             PushNotificationSender pushNotificationSender) {
        this.inventoryContextFacade = inventoryContextFacade;
        this.profileContextFacade = profileContextFacade;
        this.deviceTokenRepository = deviceTokenRepository;
        this.noticeLog = noticeLog;
        this.pushNotificationSender = pushNotificationSender;
    }

    @Override
    public LotExpiryDispatchSummary handle(DispatchLotExpiryNoticesCommand command) {
        LocalDate today = LocalDate.ofInstant(command.now(), ZoneOffset.UTC);
        List<ExpiringLot> lots = inventoryContextFacade.fetchLotsExpiringBetween(
                today, today.plusDays(EXPIRING_SOON_DAYS));

        Map<String, NotificationPreferences> preferencesByOwner = new HashMap<>();
        Map<String, List<DeviceToken>> devicesByOwner = new HashMap<>();
        int inWindow = 0;
        int notified = 0;
        int deliveries = 0;
        int skippedByPreference = 0;
        int skippedWithoutDevice = 0;
        int skippedAlreadySent = 0;
        int invalidTokensRemoved = 0;

        for (ExpiringLot lot : lots) {
            String owner = lot.ownerEmail();
            if (command.ownerEmail() != null && !command.ownerEmail().equals(owner)) {
                continue;
            }
            inWindow++;

            NotificationPreferences preferences = preferencesByOwner.computeIfAbsent(
                    owner, profileContextFacade::fetchNotificationPreferences);
            if (!preferences.stockAlerts()) {
                skippedByPreference++;
                continue;
            }
            if (!command.force() && noticeLog.wasSent(lot.lotType(), lot.lotId(), today)) {
                skippedAlreadySent++;
                continue;
            }
            List<DeviceToken> devices = devicesByOwner.computeIfAbsent(
                    owner, key -> new ArrayList<>(deviceTokenRepository.findAllByOwnerEmail(key)));
            if (devices.isEmpty()) {
                skippedWithoutDevice++;
                continue;
            }

            long daysLeft = ChronoUnit.DAYS.between(today, lot.expiryDate());
            var notice = new LotExpiryNotice(owner, lot.lotType(), lot.lotId(), lot.lotCode(), lot.productId(),
                    lot.productName(), lot.expiryDate(), daysLeft);
            var message = LotExpiryMessageComposer.compose(notice, preferences.language());

            boolean delivered = false;
            for (DeviceToken device : List.copyOf(devices)) {
                PushResult result = pushNotificationSender.send(device.getToken(), message);
                switch (result) {
                    case SENT -> {
                        deliveries++;
                        delivered = true;
                    }
                    case INVALID_TOKEN -> {
                        deviceTokenRepository.deleteByToken(device.getToken());
                        devices.remove(device);
                        invalidTokensRemoved++;
                    }
                    case NOT_CONFIGURED, FAILED -> {
                        // Nothing was delivered; the lot is retried on the next run.
                    }
                }
            }
            if (delivered) {
                noticeLog.markSent(owner, lot.lotType(), lot.lotId(), today);
                notified++;
            }
        }

        var summary = new LotExpiryDispatchSummary(inWindow, notified, deliveries, skippedByPreference,
                skippedWithoutDevice, skippedAlreadySent, invalidTokensRemoved);
        log.info("[push] lot expiry notices for {}: {}", today, summary);
        return summary;
    }
}
