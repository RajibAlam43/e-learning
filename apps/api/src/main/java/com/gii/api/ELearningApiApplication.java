package com.gii.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.gii.api", "com.gii.common"})
@EntityScan(basePackages = {"com.gii.common.model"})
@EnableJpaRepositories(basePackages = {"com.gii.common.repository"})
public class ELearningApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ELearningApiApplication.class, args);
	}

}