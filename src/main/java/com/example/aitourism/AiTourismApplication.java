package com.example.aitourism;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiTourismApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiTourismApplication.class, args);
	}

}
