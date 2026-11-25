package online.entreprenly.platform.subscription.infrastructure.persistence.jpa.repositories;

import online.entreprenly.platform.subscription.domain.model.valueobjects.SubscriptionStatus;
import online.entreprenly.platform.subscription.infrastructure.persistence.jpa.entities.SubscriptionPersistenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for subscription persistence entities.
 */
@Repository
public interface SubscriptionPersistenceRepository extends JpaRepository<SubscriptionPersistenceEntity, Long> {
    @Query("""
            select s from SubscriptionPersistenceEntity s
            where s.userId = :userId and s.status = :status
              and (s.currentPeriodEnd is null or s.currentPeriodEnd > :now)
            order by s.startedAt desc, s.id desc
            """)
    List<SubscriptionPersistenceEntity> findActiveAt(@Param("userId") Long userId,
            @Param("status") SubscriptionStatus status, @Param("now") Instant now,
            Pageable pageable);

    @Query("""
            select distinct s.userId from SubscriptionPersistenceEntity s
            where s.planId = :planId and s.status = :status
              and s.cancelledAt is not null and s.currentPeriodEnd <= :now
            """)
    List<Long> findUsersWithScheduledCancellationDue(@Param("planId") Long planId,
            @Param("status") SubscriptionStatus status, @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<SubscriptionPersistenceEntity> findAllByUserIdOrderByIdAsc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SubscriptionPersistenceEntity s where s.id = :id")
    Optional<SubscriptionPersistenceEntity> lockById(@Param("id") Long id);
}
