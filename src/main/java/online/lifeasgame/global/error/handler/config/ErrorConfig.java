package online.lifeasgame.global.error.handler.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppErrorProperties.class)
public class ErrorConfig { }
