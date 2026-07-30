package com.devflow.copilot.common;

public enum ProviderFailureStage {
    CONFIG_VALIDATION,
    REQUEST_BUILD,
    CONNECTION,
    HTTP_STATUS_RECEIVED,
    RESPONSE_DESERIALIZATION,
    CONTENT_EXTRACTION,
    UNKNOWN
}
