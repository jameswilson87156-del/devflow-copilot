package com.devflow.copilot.service.provider;

import com.devflow.copilot.common.LlmProviderException;
import com.devflow.copilot.common.ProviderFailureMetadata;
import com.devflow.copilot.common.ProviderFailureStage;
import com.devflow.copilot.common.ProviderErrorType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderErrorClassifierTest {

    @Test
    void classifiesOfflineTransportAndResponseFailuresWithoutUsingRawMessages() {
        assertThat(ProviderErrorClassifier.classify(new ResourceAccessException("offline", new SocketTimeoutException())))
                .isEqualTo(ProviderErrorType.TIMEOUT);
        assertThat(ProviderErrorClassifier.classify(new ResourceAccessException("offline", new ConnectException())))
                .isEqualTo(ProviderErrorType.CONNECTION_FAILED);
        assertThat(ProviderErrorClassifier.classify(HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8)))
                .isEqualTo(ProviderErrorType.AUTHENTICATION_FAILED);
        assertThat(ProviderErrorClassifier.classify(HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8)))
                .isEqualTo(ProviderErrorType.RATE_LIMITED);
        assertThat(ProviderErrorClassifier.classify(HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR, "", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8)))
                .isEqualTo(ProviderErrorType.UPSTREAM_SERVER_ERROR);
        assertThat(ProviderErrorClassifier.classify(new JsonProcessingException("offline") { }))
                .isEqualTo(ProviderErrorType.INVALID_RESPONSE);
    }

    @Test
    void providerExceptionKeepsCauseOutOfSafeMessagesToStringAndJson() throws Exception {
        String rawCause = "offline raw upstream content";
        LlmProviderException exception = new LlmProviderException(
                ProviderErrorType.UPSTREAM_SERVER_ERROR,
                ProviderFailureMetadata.atStage(ProviderFailureStage.HTTP_STATUS_RECEIVED),
                new RuntimeException(rawCause)
        );

        String serialized = new ObjectMapper().writeValueAsString(exception);
        assertThat(exception.getMessage()).isEqualTo(ProviderErrorType.UPSTREAM_SERVER_ERROR.safeMessage());
        assertThat(exception.toString()).doesNotContain(rawCause);
        assertThat(serialized).doesNotContain(rawCause, "cause", "stackTrace");
    }
}
