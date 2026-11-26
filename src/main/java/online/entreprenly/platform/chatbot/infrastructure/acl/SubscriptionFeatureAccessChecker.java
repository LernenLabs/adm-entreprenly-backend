package online.entreprenly.platform.chatbot.infrastructure.acl;

import online.entreprenly.platform.chatbot.application.internal.outboundservices.acl.SubscriptionAccessChecker;
import online.entreprenly.platform.iam.domain.repositories.UserRepository;
import online.entreprenly.platform.subscription.application.queryservices.SubscriptionPlanQueryService;
import online.entreprenly.platform.subscription.application.queryservices.SubscriptionQueryService;
import online.entreprenly.platform.subscription.domain.model.queries.GetActiveSubscriptionByUserIdQuery;
import online.entreprenly.platform.subscription.domain.model.queries.GetSubscriptionPlanByIdQuery;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionFeatureAccessChecker implements SubscriptionAccessChecker {

    private final UserRepository users;
    private final SubscriptionQueryService subscriptionQueryService;
    private final SubscriptionPlanQueryService subscriptionPlanQueryService;

    public SubscriptionFeatureAccessChecker(UserRepository users,
                                            SubscriptionQueryService subscriptionQueryService,
                                            SubscriptionPlanQueryService subscriptionPlanQueryService) {
        this.users = users;
        this.subscriptionQueryService = subscriptionQueryService;
        this.subscriptionPlanQueryService = subscriptionPlanQueryService;
    }

    @Override
    public boolean canUseChatbot(String ownerEmail) {
        if (ownerEmail == null || ownerEmail.isBlank()) {
            return false;
        }
        return users.findByOwnerEmail(ownerEmail.trim())
                .flatMap(user -> subscriptionQueryService.handle(new GetActiveSubscriptionByUserIdQuery(user.getId()))
                        .flatMap(subscription -> subscriptionPlanQueryService.handle(
                                new GetSubscriptionPlanByIdQuery(subscription.getPlanId()))))
                .filter(plan -> plan.isActive() && plan.hasFeature(CHATBOT_FEATURE))
                .isPresent();
    }
}