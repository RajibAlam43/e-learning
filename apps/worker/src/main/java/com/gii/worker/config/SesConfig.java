package com.gii.worker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.SesClientBuilder;

@Configuration
public class SesConfig {

  @Value("${aws.ses.region:ap-southeast-1}")
  private String sesRegion;

  @Value("${aws.ses.access-key-id:}")
  private String sesAccessKeyId;

  @Value("${aws.ses.secret-access-key:}")
  private String sesSecretAccessKey;

  @Bean
  public SesClient sesClient() {
    SesClientBuilder builder = SesClient.builder().region(Region.of(sesRegion));

    if (!sesAccessKeyId.isBlank() && !sesSecretAccessKey.isBlank()) {
      builder.credentialsProvider(
          StaticCredentialsProvider.create(
              AwsBasicCredentials.create(sesAccessKeyId, sesSecretAccessKey)));
    } else {
      builder.credentialsProvider(DefaultCredentialsProvider.builder().build());
    }

    return builder.build();
  }
}
