package online.entreprenly.platform.subscription.application.internal.commandservices;

import online.entreprenly.platform.subscription.domain.model.aggregates.Subscription;
import online.entreprenly.platform.subscription.domain.repositories.SubscriptionPlanRepository;
import online.entreprenly.platform.subscription.domain.repositories.SubscriptionRepository;
import online.entreprenly.platform.subscription.interfaces.events.SubscriptionPlanChangedIntegrationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static online.entreprenly.platform.subscription.application.internal.eventhandlers.SubscriptionCatalogReadyEventHandler.CONTROL_PLAN_CODE;
import static online.entreprenly.platform.subscription.application.internal.eventhandlers.SubscriptionCatalogReadyEventHandler.FREE_PLAN_CODE;

@Service
public class CompleteScheduledCancellationService {

    private final SubscriptionRepository subscriptions;
    private final SubscriptionPlanRepository plans;
    private final ApplicationEventPublisher events;

    public CompleteScheduledCancellationService(SubscriptionRepository subscriptions,
                                                SubscriptionPlanRepository plans,
                                                ApplicationEventPublisher events) {
        this.subscriptions = subscriptions;
        this.plans = plans;
        this.events = events;
    }

    public List<Long> findDueUsers(Instant now) {
        return plans.findByCode(CONTROL_PLAN_CODE)
                .map(plan -> subscriptions.findUsersWithScheduledCancellationDue(plan.getId(), now))
                .orElseGet(List::of);
    }

    @Transactional
    public void completeForUser(Long userId, Instant now) {
        var control = plans.findByCode(CONTROL_PLAN_CODE).orElseThrow();
        var completed = subscriptions.lockSubscriptionsByUserId(userId).stream()
                .filter(subscription -> control.getId().equals(subscription.getPlanId()))
                .filter(subscription -> subscription.completeScheduledCancellation(now))
                .toList();
        if (completed.isEmpty()) {
            return;
        }

        completed.forEach(subscriptions::save);

        var free = plans.findByCode(FREE_PLAN_CODE).orElseThrow();
        var active = subscriptions.findActiveAt(userId, now);
        if (active.isPresent() && !free.getId().equals(active.get().getPlanId())) {
            return;
        }

        if (active.isEmpty()) {
            var freeSubscription = new Subscription(userId, free, now);
            freeSubscription.activateWithoutExpiration();
            subscriptions.save(freeSubscription);
        }
        events.publishEvent(new SubscriptionPlanChangedIntegrationEvent(userId, free.getName()));
    }
}
