package online.lifeasgame.platform.outbox.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.platform.outbox.OutboxProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.outbox",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxRelayScheduler implements SmartLifecycle {

    private final OutboxRelayService relayService;
    private final OutboxProperties properties;

    private volatile boolean running;
    private ScheduledExecutorService executor;

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(
                    runnable,
                    "outbox-relay-" + properties.getInstanceId()
            );
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(
                this::relay,
                properties.getFixedDelayMs(),
                properties.getFixedDelayMs(),
                TimeUnit.MILLISECONDS
        );
        running = true;
    }

    private void relay() {
        try {
            relayService.relayBatch();
        } catch (RuntimeException exception) {
            log.error("Transactional outbox relay cycle failed", exception);
        }
    }

    @Override
    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
