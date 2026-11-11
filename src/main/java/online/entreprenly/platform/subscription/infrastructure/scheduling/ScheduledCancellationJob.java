package online.entreprenly.platform.subscription.infrastructure.scheduling;

import online.entreprenly.platform.subscription.application.internal.commandservices.CompleteScheduledCancellationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
class ScheduledCancellationJob {
    private static final Logger log = LoggerFactory.getLogger(ScheduledCancellationJob.class);

    private final CompleteScheduledCancellationService cancellations;
    private final Clock clock;

    ScheduledCancellationJob(CompleteScheduledCancellationService cancellations, Clock clock) {
        this.cancellations = cancellations;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${subscriptions.cancellation.fixed-delay-ms:60000}",
            initialDelayString = "${subscriptions.cancellation.fixed-delay-ms:60000}")
    void completeDueCancellations() {
        var now = clock.instant();
        for (var userId : cancellations.findDueUsers(now)) {
            try {
                cancellations.completeForUser(userId, now);
            } catch (RuntimeException e) {
                log.error("Could not complete scheduled subscription cancellation for user {}", userId, e);
            }
        }
    }
}
