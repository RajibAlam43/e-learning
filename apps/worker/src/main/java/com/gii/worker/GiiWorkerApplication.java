package com.gii.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.gii.worker", "com.gii.common"})
@EntityScan(basePackages = {"com.gii.common.entity"})
@EnableJpaRepositories(basePackages = {"com.gii.common.repository"})
public class GiiWorkerApplication {

  public static void main(String[] args) {
    SpringApplication.run(GiiWorkerApplication.class, args);
  }
}
