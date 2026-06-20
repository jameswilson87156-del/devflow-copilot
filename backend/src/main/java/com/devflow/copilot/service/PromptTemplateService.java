package com.devflow.copilot.service;

import com.devflow.copilot.entity.PromptTemplate;

import java.util.List;

public interface PromptTemplateService {

    List<PromptTemplate> list(String templateType);

    PromptTemplate create(PromptTemplate promptTemplate);

    PromptTemplate update(Long id, PromptTemplate promptTemplate);

    PromptTemplate getById(Long id);

    PromptTemplate findEnabledDefault(String generationType);
}
