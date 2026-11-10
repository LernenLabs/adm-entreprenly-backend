package online.entreprenly.platform.notification.infrastructure.scheduling;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's {@code @Scheduled} support for the notification jobs.
 */
@Configuration
@EnableScheduling
public class SchedulingConfiguration {
}
