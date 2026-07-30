package com.gii.api.config;

import java.net.URI;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
public class SqsConfig {

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }

  @Bean
  @Profile("local")
  public SqsAsyncClient sqsAsyncClientLocal() {
    return SqsAsyncClient.builder()
        .endpointOverride(URI.create("http://elasticmq:9324"))
        .region(Region.AP_SOUTHEAST_1)
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create("foo", "bar")))
        .build();
  }

  @Bean
  @Profile("!local")
  public SqsAsyncClient sqsAsyncClient() {
    return SqsAsyncClient.builder()
        .region(Region.AP_SOUTHEAST_1)
        .credentialsProvider(DefaultCredentialsProvider.builder().build())
        .build();
  }
}
