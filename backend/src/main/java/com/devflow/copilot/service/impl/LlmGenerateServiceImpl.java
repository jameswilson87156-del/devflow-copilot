package com.devflow.copilot.service.impl;

import com.devflow.copilot.service.LlmGenerateService;
import com.devflow.copilot.service.provider.GenerationProviderRouter;
import com.devflow.copilot.service.provider.ProviderRequest;
import com.devflow.copilot.service.provider.ProviderResult;
import org.springframework.stereotype.Service;

@Service
public class LlmGenerateServiceImpl implements LlmGenerateService {

    private final GenerationProviderRouter providerRouter;

    public LlmGenerateServiceImpl(GenerationProviderRouter providerRouter) {
        this.providerRouter = providerRouter;
    }

    @Override
    public ProviderResult generate(ProviderRequest request) {
        return providerRouter.generate(request);
    }
}
