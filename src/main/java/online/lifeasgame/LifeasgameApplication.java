package online.lifeasgame;

import online.lifeasgame.system.bootstrap.security.WebCorsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import online.lifeasgame.demo.application.DemoProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({WebCorsProperties.class, DemoProperties.class})
public class LifeasgameApplication {

	public static void main(String[] args) {
		SpringApplication.run(LifeasgameApplication.class, args);
	}

}
