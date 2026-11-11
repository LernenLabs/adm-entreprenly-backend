package online.entreprenly.platform.subscription.infrastructure.scheduling;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class SubscriptionTimeConfiguration {
    @Bean
    Clock subscriptionClock() {
        return Clock.systemUTC();
    }
}
