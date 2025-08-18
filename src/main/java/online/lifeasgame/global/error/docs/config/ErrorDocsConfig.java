package online.lifeasgame.global.error.docs.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppErrorDocsProperties.class)
public class ErrorDocsConfig {}
