package online.lifeasgame;

import org.springframework.boot.SpringApplication;

public class TestLifeasgameApplication {

	public static void main(String[] args) {
		SpringApplication.from(LifeasgameApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
