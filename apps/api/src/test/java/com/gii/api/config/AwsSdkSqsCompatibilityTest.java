package com.gii.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;

class AwsSdkSqsCompatibilityTest {

  @Test
  void realSqsClientCanExecuteRequestWithoutBinaryLinkageErrors() {
    assertTimeoutPreemptively(
        Duration.ofSeconds(3),
        () -> {
          try (SqsAsyncClient client = newClient()) {
            CompletableFuture<GetQueueUrlResponse> response =
                assertDoesNotThrow(
                    () ->
                        client.getQueueUrl(
                            GetQueueUrlRequest.builder().queueName("compatibility-test").build()));

            Throwable failure = response.handle((ignored, error) -> error).join();
            assertThat(rootCause(failure)).isNotInstanceOf(LinkageError.class);
          }
        });
  }

  private SqsAsyncClient newClient() {
    ClientOverrideConfiguration overrideConfiguration =
        ClientOverrideConfiguration.builder()
            .apiCallTimeout(Duration.ofSeconds(1))
            .apiCallAttemptTimeout(Duration.ofMillis(500))
            .retryPolicy(RetryPolicy.none())
            .build();

    return SqsAsyncClient.builder()
        .endpointOverride(URI.create("http://127.0.0.1:1"))
        .region(Region.AP_SOUTHEAST_1)
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
        .overrideConfiguration(overrideConfiguration)
        .build();
  }

  private Throwable rootCause(Throwable failure) {
    Throwable current = failure;
    while (current.getCause() != null) {
      current = current.getCause();
    }
    return current;
  }
}
