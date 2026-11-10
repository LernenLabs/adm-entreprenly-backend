package online.entreprenly.platform.notification.infrastructure.scheduling;

import online.entreprenly.platform.notification.application.commandservices.LotExpiryNoticeCommandService;
import online.entreprenly.platform.notification.domain.model.commands.DispatchLotExpiryNoticesCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Daily job that pushes the notices for lots expiring within the next five days.
 *
 * <p>Configurable through {@code notifications.lot-expiry.*}. Each lot is notified at most once
 * per day, so running the job again (or restarting the app) does not repeat notices.</p>
 */
@Component
public class LotExpiryNoticeScheduler {

    private static final Logger log = LoggerFactory.getLogger(LotExpiryNoticeScheduler.class);

    private final LotExpiryNoticeCommandService lotExpiryNoticeCommandService;
    private final boolean enabled;

    public LotExpiryNoticeScheduler(LotExpiryNoticeCommandService lotExpiryNoticeCommandService,
                                    @Value("${notifications.lot-expiry.enabled:true}") boolean enabled) {
        this.lotExpiryNoticeCommandService = lotExpiryNoticeCommandService;
        this.enabled = enabled;
    }

    @Scheduled(cron = "${notifications.lot-expiry.cron:0 0 8 * * *}",
            zone = "${notifications.lot-expiry.zone:America/Lima}")
    public void dispatchDailyNotices() {
        if (!enabled) {
            return;
        }
        try {
            var summary = lotExpiryNoticeCommandService.handle(
                    new DispatchLotExpiryNoticesCommand(null, false, Instant.now()));
            log.info("[notification] lot expiry run: {}", summary);
        } catch (RuntimeException e) {
            log.error("[notification] lot expiry run failed", e);
        }
    }
}
