package online.entreprenly.platform.subscription.domain.repositories;

import online.entreprenly.platform.subscription.domain.model.aggregates.Subscription;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Subscription repository port.
 */
public interface SubscriptionRepository {
    Optional<Subscription> findById(Long id);
    Optional<Subscription> findActiveAt(Long userId, Instant now);
    List<Long> findUsersWithScheduledCancellationDue(Long planId, Instant now);
    List<Subscription> lockSubscriptionsByUserId(Long userId);
    Optional<Subscription> lockById(Long id);
    Subscription save(Subscription subscription);
}
