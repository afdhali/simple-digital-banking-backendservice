package tutorjava.digitalbanking.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.idempotency")
public record IdempotencyProperties(@NotNull Duration ttl) {
}
