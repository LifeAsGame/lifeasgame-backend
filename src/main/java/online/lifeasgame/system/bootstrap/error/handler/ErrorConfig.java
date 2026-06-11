package online.lifeasgame.system.bootstrap.error.handler;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppErrorProperties.class)
public class ErrorConfig { }
