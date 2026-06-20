package com.devflow.copilot.common;

import org.springframework.http.HttpStatus;

public class TemplateRenderException extends BusinessException {

    public TemplateRenderException(String message) {
        super(4001, HttpStatus.BAD_REQUEST, message);
    }
}
