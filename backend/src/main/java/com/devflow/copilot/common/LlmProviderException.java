package com.devflow.copilot.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpStatus;

@JsonIgnoreProperties({"cause", "stackTrace", "suppressed", "localizedMessage"})
public class LlmProviderException extends BusinessException {

    private final ProviderErrorType errorType;
    private final ProviderFailureMetadata failureMetadata;

    public LlmProviderException(ProviderErrorType errorType) {
        this(errorType, ProviderFailureMetadata.unknown(), null);
    }

    public LlmProviderException(ProviderErrorType errorType, Throwable cause) {
        this(errorType, ProviderFailureMetadata.unknown(), cause);
    }

    public LlmProviderException(ProviderErrorType errorType, ProviderFailureMetadata failureMetadata) {
        this(errorType, failureMetadata, null);
    }

    public LlmProviderException(ProviderErrorType errorType, ProviderFailureMetadata failureMetadata, Throwable cause) {
        super(5001, HttpStatus.BAD_GATEWAY, errorType.safeMessage());
        this.errorType = errorType;
        this.failureMetadata = failureMetadata == null ? ProviderFailureMetadata.unknown() : failureMetadata;
        if (cause != null) {
            initCause(cause);
        }
    }

    public ProviderErrorType getErrorType() {
        return errorType;
    }

    public ProviderFailureMetadata getFailureMetadata() {
        return failureMetadata;
    }

    @Override
    public String getMessage() {
        return errorType.safeMessage();
    }

    @Override
    @JsonIgnore
    public synchronized Throwable getCause() {
        return super.getCause();
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + getMessage();
    }
}
