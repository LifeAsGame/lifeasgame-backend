package online.lifeasgame.platform.outbox;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.outbox")
public class OutboxProperties {

    private boolean enabled = true;

    @Min(1)
    private long fixedDelayMs = 500;

    @Min(1)
    private int batchSize = 50;

    @Min(1)
    private int maxAttempts = 10;

    @Min(0)
    private long retryDelayMs = 1_000;

    @Min(1)
    private long leaseDurationMs = 30_000;

    @NotBlank
    private String instanceId = "outbox-" + UUID.randomUUID();
}
