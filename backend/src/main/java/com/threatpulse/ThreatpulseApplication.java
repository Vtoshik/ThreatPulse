package com.threatpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.OffsetDateTime;
import java.util.Optional;

@SpringBootApplication
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
public class ThreatpulseApplication {

	public static void main(String[] args) {
		SpringApplication.run(ThreatpulseApplication.class, args);
	}

	@Bean
	public DateTimeProvider offsetDateTimeProvider() {
		return () -> Optional.of(OffsetDateTime.now());
	}
}
