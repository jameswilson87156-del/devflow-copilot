package com.devflow.copilot.common;

public enum ProviderErrorType {
    PROTOCOL_UNSUPPORTED("Provider protocol is unsupported."),
    API_KEY_MISSING("Provider credential is not configured."),
    TIMEOUT("Provider request timed out."),
    AUTHENTICATION_FAILED("Provider authentication failed."),
    RATE_LIMITED("Provider rate limit reached."),
    UPSTREAM_SERVER_ERROR("Provider upstream service failed."),
    INVALID_RESPONSE("Provider returned an invalid response."),
    EMPTY_CONTENT("Provider returned empty content."),
    CONNECTION_FAILED("Provider connection failed."),
    UNKNOWN_PROVIDER_ERROR("Provider request failed.");

    private final String safeSummary;

    ProviderErrorType(String safeSummary) {
        this.safeSummary = safeSummary;
    }

    public String safeMessage() {
        return name() + ": " + safeSummary;
    }
}
