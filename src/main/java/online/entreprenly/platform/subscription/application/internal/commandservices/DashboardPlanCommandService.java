package online.entreprenly.platform.subscription.application.internal.commandservices;

import online.entreprenly.platform.subscription.application.internal.eventhandlers.SubscriptionCatalogReadyEventHandler;
import online.entreprenly.platform.subscription.domain.model.aggregates.Subscription;
import online.entreprenly.platform.subscription.domain.model.aggregates.SubscriptionPlan;
import online.entreprenly.platform.subscription.domain.model.valueobjects.BillingPeriod;
import online.entreprenly.platform.subscription.domain.repositories.SubscriptionPlanRepository;
import online.entreprenly.platform.subscription.domain.repositories.SubscriptionRepository;
import online.entreprenly.platform.subscription.interfaces.events.SubscriptionPlanChangedIntegrationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class DashboardPlanCommandService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public DashboardPlanCommandService(SubscriptionPlanRepository subscriptionPlanRepository,
                                       SubscriptionRepository subscriptionRepository,
                                       ApplicationEventPublisher eventPublisher, Clock clock) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public void changePlan(Long userId, String targetCode, String targetStatus, BillingPeriod billingPeriod) {
        var freePlan = subscriptionPlanRepository.findByCode(SubscriptionCatalogReadyEventHandler.FREE_PLAN_CODE)
                .orElseThrow();
        var controlPlan = subscriptionPlanRepository.findByCode(SubscriptionCatalogReadyEventHandler.CONTROL_PLAN_CODE)
                .orElseThrow();
        subscriptionRepository.lockSubscriptionsByUserId(userId);
        var now = clock.instant();
        var activeSubscription = subscriptionRepository.findActiveAt(userId, now);

        if (SubscriptionCatalogReadyEventHandler.CONTROL_PLAN_CODE.equals(targetCode)) {
            var currentControl = activeSubscription
                    .filter(subscription -> controlPlan.getId().equals(subscription.getPlanId()));
            if (currentControl.isPresent()) {
                var subscription = currentControl.get();
                if ("scheduled-cancellation".equals(targetStatus)) {
                    subscription.scheduleCancellation(now);
                } else {
                    subscription.resumeActive();
                }
                subscriptionRepository.save(subscription);
                publishPlanChanged(userId, controlPlan);
                return;
            }
            if ("scheduled-cancellation".equals(targetStatus)) {
                return;
            }
            activeSubscription.ifPresent(subscription -> {
                subscription.cancel();
                subscriptionRepository.save(subscription);
            });
            var subscription = new Subscription(userId, controlPlan, now);
            subscription.activate(billingPeriod, now);
            subscriptionRepository.save(subscription);
            publishPlanChanged(userId, controlPlan);
            return;
        }

        if (SubscriptionCatalogReadyEventHandler.FREE_PLAN_CODE.equals(targetCode)) {
            var onFree = activeSubscription
                    .filter(subscription -> freePlan.getId().equals(subscription.getPlanId()))
                    .isPresent();
            activeSubscription
                    .filter(subscription -> !freePlan.getId().equals(subscription.getPlanId()))
                    .ifPresent(subscription -> {
                        subscription.cancel();
                        subscriptionRepository.save(subscription);
                    });
            if (!onFree) {
                var subscription = new Subscription(userId, freePlan, now);
                subscription.activateWithoutExpiration();
                subscriptionRepository.save(subscription);
            }
            publishPlanChanged(userId, freePlan);
        }
    }

    /**
     * Publishes the Subscription context's integration event so other bounded contexts (e.g.
     * Profile) can react to a plan change without coupling to the subscription domain.
     */
    private void publishPlanChanged(Long userId, SubscriptionPlan plan) {
        eventPublisher.publishEvent(new SubscriptionPlanChangedIntegrationEvent(userId, plan.getName()));
    }
}
