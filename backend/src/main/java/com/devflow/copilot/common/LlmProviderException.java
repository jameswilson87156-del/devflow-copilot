package com.devflow.copilot.common;

import org.springframework.http.HttpStatus;

public class LlmProviderException extends BusinessException {

    public LlmProviderException(String message) {
        super(5001, HttpStatus.BAD_GATEWAY, message);
    }

    public LlmProviderException(String message, Throwable cause) {
        super(5001, HttpStatus.BAD_GATEWAY, message);
        initCause(cause);
    }
}
