package online.lifeasgame.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncExecutorConfig {

    @Bean(name = {"applicationTaskExecutor", "taskExecutor"})
    public ThreadPoolTaskExecutor asyncExecutor(TaskDecorator decorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setTaskDecorator(decorator);
        executor.initialize();
        return executor;
    }
}
