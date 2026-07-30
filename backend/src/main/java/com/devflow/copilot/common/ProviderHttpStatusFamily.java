package com.devflow.copilot.common;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ProviderHttpStatusFamily {
    FOUR_XX("4XX"),
    FIVE_XX("5XX"),
    UNKNOWN("UNKNOWN");

    private final String value;

    ProviderHttpStatusFamily(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
