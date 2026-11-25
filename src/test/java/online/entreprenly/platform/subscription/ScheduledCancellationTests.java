package online.entreprenly.platform.subscription;

import online.entreprenly.platform.profile.application.commandservices.ProfileCommandService;
import online.entreprenly.platform.profile.domain.model.commands.CreateProfileCommand;
import online.entreprenly.platform.profile.domain.repositories.ProfileRepository;
import online.entreprenly.platform.subscription.application.internal.commandservices.CompleteScheduledCancellationService;
import online.entreprenly.platform.subscription.application.internal.commandservices.DashboardPlanCommandService;
import online.entreprenly.platform.subscription.application.queryservices.SubscriptionQueryService;
import online.entreprenly.platform.subscription.domain.model.aggregates.Subscription;
import online.entreprenly.platform.subscription.domain.model.queries.GetActiveSubscriptionByUserIdQuery;
import online.entreprenly.platform.subscription.domain.model.valueobjects.BillingPeriod;
import online.entreprenly.platform.subscription.domain.model.valueobjects.SubscriptionStatus;
import online.entreprenly.platform.subscription.domain.repositories.SubscriptionPlanRepository;
import online.entreprenly.platform.subscription.domain.repositories.SubscriptionRepository;
import online.entreprenly.platform.subscription.interfaces.rest.SubscriptionsController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static online.entreprenly.platform.subscription.application.internal.eventhandlers.SubscriptionCatalogReadyEventHandler.CONTROL_PLAN_CODE;
import static online.entreprenly.platform.subscription.application.internal.eventhandlers.SubscriptionCatalogReadyEventHandler.FREE_PLAN_CODE;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.profiles.default=test",
        "spring.datasource.url=jdbc:h2:mem:scheduled_cancellations;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "subscriptions.cancellation.fixed-delay-ms=3600000"
})
class ScheduledCancellationTests {

    private static final Instant PERIOD_END = Instant.parse("2026-12-01T05:00:00Z");
    private static final Instant REQUESTED_AT = PERIOD_END.minus(1, ChronoUnit.DAYS);

    @TestConfiguration
    static class FixedTime {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(PERIOD_END, ZoneOffset.UTC);
        }
    }

    @Autowired private CompleteScheduledCancellationService cancellations;
    @Autowired private DashboardPlanCommandService dashboardPlans;
    @Autowired private SubscriptionRepository subscriptions;
    @Autowired private SubscriptionPlanRepository plans;
    @Autowired private SubscriptionQueryService queries;
    @Autowired private SubscriptionsController controller;
    @Autowired private ProfileCommandService profileCommands;
    @Autowired private ProfileRepository profiles;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void expiresAtTheExactBoundaryAndPersistsFreeOnce() throws Exception {
        var userId = 1001L;
        var freePlan = plans.findByCode(FREE_PLAN_CODE).orElseThrow();
        var control = createControl(userId, true);
        var justBefore = PERIOD_END.minusMillis(1);
        assertTrue(profileCommands.handle(new CreateProfileCommand(userId, "Ana", "Pérez",
                "Administrador", "Plan Control", null, null)).isSuccess());

        assertEquals(control.getId(), subscriptions.findActiveAt(userId, justBefore).orElseThrow().getId());
        assertFalse(cancellations.findDueUsers(justBefore).contains(userId));
        cancellations.completeForUser(userId, justBefore);
        assertEquals(SubscriptionStatus.ACTIVE, subscriptions.findById(control.getId()).orElseThrow().getStatus());

        assertTrue(queries.handle(new GetActiveSubscriptionByUserIdQuery(userId)).isEmpty());
        assertFalse(controller.validateAccess(userId, "chatbot").getBody().hasAccess());
        assertTrue(cancellations.findDueUsers(PERIOD_END).contains(userId));

        // A dashboard retry at the cutoff must not buy a new Control subscription.
        dashboardPlans.changePlan(userId, CONTROL_PLAN_CODE, "scheduled-cancellation", BillingPeriod.MONTHLY);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var first = executor.submit(() -> {
                start.await();
                cancellations.completeForUser(userId, PERIOD_END);
                return null;
            });
            var second = executor.submit(() -> {
                start.await();
                cancellations.completeForUser(userId, PERIOD_END);
                return null;
            });
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        }
        cancellations.completeForUser(userId, PERIOD_END.plusSeconds(60));

        var expired = subscriptions.findById(control.getId()).orElseThrow();
        assertEquals(SubscriptionStatus.CANCELLED, expired.getStatus());
        assertEquals(PERIOD_END, expired.getCurrentPeriodEnd());
        assertEquals(REQUESTED_AT, expired.getCancelledAt());
        var active = queries.handle(new GetActiveSubscriptionByUserIdQuery(userId)).orElseThrow();
        assertEquals(freePlan.getId(), active.getPlanId());
        assertNull(active.getCurrentPeriodEnd());
        assertEquals(1L, jdbc.queryForObject(
                "select count(*) from subscriptions where user_id = ? and plan_id = ?", Long.class,
                userId, freePlan.getId()));
        assertEquals("Plan Free", profiles.findByUserId(userId).orElseThrow().getPlan());
        assertFalse(controller.validateAccess(userId, "chatbot").getBody().hasAccess());
        assertTrue(controller.validateAccess(userId, "basic-inventory").getBody().hasAccess());
    }

    @Test
    void ignoresResumedControlAndFindsOlderFreeBehindAnExpiredRow() {
        var userId = 1002L;
        var freePlan = plans.findByCode(FREE_PLAN_CODE).orElseThrow();
        var free = new Subscription(userId, freePlan, PERIOD_END.minus(60, ChronoUnit.DAYS));
        free.activateWithoutExpiration();
        var savedFree = subscriptions.save(free);
        var control = createControl(userId, false);

        assertFalse(cancellations.findDueUsers(PERIOD_END).contains(userId));
        cancellations.completeForUser(userId, PERIOD_END);
        assertEquals(SubscriptionStatus.ACTIVE, subscriptions.findById(control.getId()).orElseThrow().getStatus());
        assertEquals(savedFree.getId(), queries.handle(new GetActiveSubscriptionByUserIdQuery(userId))
                .orElseThrow().getId());
    }

    private Subscription createControl(Long userId, boolean keepCancellation) {
        var controlPlan = plans.findByCode(CONTROL_PLAN_CODE).orElseThrow();
        var start = PERIOD_END.minus(30, ChronoUnit.DAYS);
        var control = new Subscription(userId, controlPlan, start);
        control.activate(BillingPeriod.MONTHLY, start);
        control.scheduleCancellation(REQUESTED_AT);
        if (!keepCancellation) {
            control.resumeActive();
        }
        return subscriptions.save(control);
    }
}
