package online.entreprenly.platform.subscription.infrastructure.persistence.jpa.adapters;

import online.entreprenly.platform.subscription.domain.model.aggregates.Subscription;
import online.entreprenly.platform.subscription.domain.model.valueobjects.SubscriptionStatus;
import online.entreprenly.platform.subscription.domain.repositories.SubscriptionRepository;
import online.entreprenly.platform.subscription.infrastructure.persistence.jpa.assemblers.SubscriptionPersistenceAssembler;
import online.entreprenly.platform.subscription.infrastructure.persistence.jpa.repositories.SubscriptionPersistenceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository adapter for subscriptions.
 */
@Repository
public class SubscriptionRepositoryImpl implements SubscriptionRepository {

    private final SubscriptionPersistenceRepository persistenceRepository;

    public SubscriptionRepositoryImpl(SubscriptionPersistenceRepository persistenceRepository) {
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public Optional<Subscription> findById(Long id) {
        return persistenceRepository.findById(id).map(SubscriptionPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<Subscription> findActiveAt(Long userId, Instant now) {
        return persistenceRepository.findActiveAt(userId, SubscriptionStatus.ACTIVE, now, PageRequest.of(0, 1))
                .stream().findFirst()
                .map(SubscriptionPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Long> findUsersWithScheduledCancellationDue(Long planId, Instant now) {
        return persistenceRepository.findUsersWithScheduledCancellationDue(planId, SubscriptionStatus.ACTIVE, now);
    }

    @Override
    public List<Subscription> lockSubscriptionsByUserId(Long userId) {
        return persistenceRepository.findAllByUserIdOrderByIdAsc(userId).stream()
                .map(SubscriptionPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Optional<Subscription> lockById(Long id) {
        return persistenceRepository.lockById(id).map(SubscriptionPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Subscription save(Subscription subscription) {
        var saved = persistenceRepository.save(SubscriptionPersistenceAssembler.toPersistenceFromDomain(subscription));
        return SubscriptionPersistenceAssembler.toDomainFromPersistence(saved);
    }
}
