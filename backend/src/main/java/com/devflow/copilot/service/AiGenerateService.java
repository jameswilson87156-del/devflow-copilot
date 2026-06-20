package com.devflow.copilot.service;

import com.devflow.copilot.dto.AiGenerateRequest;
import com.devflow.copilot.dto.AiGenerateResponse;

public interface AiGenerateService {

    AiGenerateResponse generate(String generationType, AiGenerateRequest request);
}
