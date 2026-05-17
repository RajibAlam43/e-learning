package com.gii.worker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gii.worker.config.RedisConfig;
import com.gii.worker.config.SqsConfig;
import com.gii.common.repository.collection.CollectionCourseRepository;
import com.gii.common.repository.collection.CollectionEnrollmentRepository;
import com.gii.common.repository.enrollment.EnrollmentRepository;
import com.gii.common.repository.order.OrderItemRepository;
import com.gii.common.repository.order.OrderRepository;
import com.gii.common.repository.order.PaymentEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

class SslcommerzValidationJobWiringTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withPropertyValues(
              "spring.cloud.aws.sqs.listener.auto-startup=false",
              "payments.sslcommerz.validation-api-url=https://sandbox.sslcommerz.com/validator/api/validationserverAPI.php",
              "payments.sslcommerz.store-id=test-store",
              "payments.sslcommerz.store-password=test-pass",
              "payments.sslcommerz.validation-timeout-ms=10000",
              "payments.sslcommerz.validation.jobs.queue=gii-test-sslcommerz-validation-queue")
          .withUserConfiguration(SqsConfig.class, TestWiringConfig.class, RedisConfig.class);

  @Test
  void contextShouldWireSslcommerzValidationJobServiceWithJacksonObjectMapper() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(ObjectMapper.class);
          assertThat(context).hasSingleBean(SslcommerzValidationJobService.class);
        });
  }

  @Configuration
  static class TestWiringConfig {
    @Bean
    WebClient.Builder webClientBuilder() {
      return WebClient.builder();
    }

    @Bean
    OrderRepository orderRepository() {
      return mock(OrderRepository.class);
    }

    @Bean
    OrderItemRepository orderItemRepository() {
      return mock(OrderItemRepository.class);
    }

    @Bean
    EnrollmentRepository enrollmentRepository() {
      return mock(EnrollmentRepository.class);
    }

    @Bean
    CollectionEnrollmentRepository collectionEnrollmentRepository() {
      return mock(CollectionEnrollmentRepository.class);
    }

    @Bean
    CollectionCourseRepository collectionCourseRepository() {
      return mock(CollectionCourseRepository.class);
    }

    @Bean
    PaymentEventRepository paymentEventRepository() {
      return mock(PaymentEventRepository.class);
    }

    @Bean
    SslcommerzValidationJobService sslcommerzValidationJobService(
        ObjectMapper objectMapper,
        WebClient.Builder webClientBuilder,
        SqsAsyncClient sqsAsyncClient,
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        EnrollmentRepository enrollmentRepository,
        CollectionEnrollmentRepository collectionEnrollmentRepository,
        CollectionCourseRepository collectionCourseRepository,
        PaymentEventRepository paymentEventRepository) {
      return new SslcommerzValidationJobService(
          objectMapper,
          webClientBuilder,
          sqsAsyncClient,
          orderRepository,
          orderItemRepository,
          enrollmentRepository,
          collectionEnrollmentRepository,
          collectionCourseRepository,
          paymentEventRepository);
    }
  }
}
