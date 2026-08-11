package online.lifeasgame;

import online.lifeasgame.system.bootstrap.security.WebCorsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(WebCorsProperties.class)
public class LifeasgameApplication {

	public static void main(String[] args) {
		SpringApplication.run(LifeasgameApplication.class, args);
	}

}
