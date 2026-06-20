package com.devflow.copilot.service;

import com.devflow.copilot.service.provider.ProviderRequest;
import com.devflow.copilot.service.provider.ProviderResult;

public interface LlmGenerateService {

    ProviderResult generate(ProviderRequest request);
}
