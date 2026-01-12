package com.team.cops_and_robbers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CopsAndRobbersApplication {

	public static void main(String[] args) {
		SpringApplication.run(CopsAndRobbersApplication.class, args);
	}

}
