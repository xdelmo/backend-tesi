package com.apexflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApexFlowApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApexFlowApplication.class, args);
	}

}
