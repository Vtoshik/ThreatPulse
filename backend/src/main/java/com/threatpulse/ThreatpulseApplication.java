package com.threatpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ThreatpulseApplication {

	public static void main(String[] args) {
		SpringApplication.run(ThreatpulseApplication.class, args);
	}

}
