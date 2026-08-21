package com.gii.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;

class AwsSdkSqsCompatibilityTest {

  @Test
  void realSqsClientCanExecuteRequestWithoutBinaryLinkageErrors() {
    try (SqsAsyncClient client = newClient()) {
      GetQueueUrlResponse response =
          assertDoesNotThrow(
              () ->
                  client
                      .getQueueUrl(
                          GetQueueUrlRequest.builder().queueName("compatibility-test").build())
                      .join());

      assertThat(response.queueUrl()).isEqualTo("http://localhost/compatibility-test");
    }
  }

  private SqsAsyncClient newClient() {
    return SqsAsyncClient.builder()
        .endpointOverride(URI.create("http://localhost"))
        .region(Region.AP_SOUTHEAST_1)
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
        .httpClient(new InMemorySqsHttpClient())
        .build();
  }

  private static final class InMemorySqsHttpClient implements SdkAsyncHttpClient {

    private static final byte[] GET_QUEUE_URL_RESPONSE =
        "{\"QueueUrl\":\"http://localhost/compatibility-test\"}".getBytes(StandardCharsets.UTF_8);

    @Override
    public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
      request
          .responseHandler()
          .onHeaders(
              SdkHttpResponse.builder()
                  .statusCode(200)
                  .putHeader("Content-Type", "application/x-amz-json-1.0")
                  .build());
      request
          .responseHandler()
          .onStream(SdkPublisher.fromIterable(List.of(ByteBuffer.wrap(GET_QUEUE_URL_RESPONSE))));
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public void close() {}
  }
}
