package com.devflow.copilot.service.provider;

public interface GenerationProvider {

    String key();

    ProviderResult generate(ProviderRequest request);
}
