package com.gii.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.gii.worker", "com.gii.common"})
@EntityScan(basePackages = {"com.gii.common.model"})
@EnableJpaRepositories(basePackages = {"com.gii.common.repository"})
public class ELearningWorkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ELearningWorkerApplication.class, args);
	}

}

