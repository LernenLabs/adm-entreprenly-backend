package online.entreprenly.platform.subscription.application.internal.queryservices;

import online.entreprenly.platform.subscription.application.queryservices.SubscriptionQueryService;
import online.entreprenly.platform.subscription.domain.model.aggregates.Payment;
import online.entreprenly.platform.subscription.domain.model.aggregates.Subscription;
import online.entreprenly.platform.subscription.domain.model.queries.GetActiveSubscriptionByUserIdQuery;
import online.entreprenly.platform.subscription.domain.model.queries.GetPaymentsBySubscriptionIdQuery;
import online.entreprenly.platform.subscription.domain.model.queries.GetSubscriptionByIdQuery;
import online.entreprenly.platform.subscription.domain.repositories.PaymentRepository;
import online.entreprenly.platform.subscription.domain.repositories.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

/**
 * Subscription query service implementation.
 */
@Service
public class SubscriptionQueryServiceImpl implements SubscriptionQueryService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;

    private final Clock clock;

    public SubscriptionQueryServiceImpl(SubscriptionRepository subscriptionRepository,
                                        PaymentRepository paymentRepository, Clock clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.clock = clock;
    }

    @Override
    public Optional<Subscription> handle(GetSubscriptionByIdQuery query) {
        return subscriptionRepository.findById(query.subscriptionId());
    }

    @Override
    public Optional<Subscription> handle(GetActiveSubscriptionByUserIdQuery query) {
        return subscriptionRepository.findActiveAt(query.userId(), clock.instant());
    }

    @Override
    public List<Payment> handle(GetPaymentsBySubscriptionIdQuery query) {
        return paymentRepository.findBySubscriptionId(query.subscriptionId());
    }
}
