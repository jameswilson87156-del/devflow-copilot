package com.devflow.copilot.controller;

import com.devflow.copilot.common.ApiResponse;
import com.devflow.copilot.entity.PromptTemplate;
import com.devflow.copilot.service.PromptTemplateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/prompts")
public class PromptTemplateController {

    private final PromptTemplateService promptTemplateService;

    public PromptTemplateController(PromptTemplateService promptTemplateService) {
        this.promptTemplateService = promptTemplateService;
    }

    @GetMapping
    public ApiResponse<List<PromptTemplate>> list(@RequestParam(required = false) String templateType) {
        return ApiResponse.ok(promptTemplateService.list(templateType));
    }

    @PostMapping
    public ApiResponse<PromptTemplate> create(@Valid @RequestBody PromptTemplate promptTemplate) {
        return ApiResponse.ok(promptTemplateService.create(promptTemplate));
    }

    @PutMapping("/{id}")
    public ApiResponse<PromptTemplate> update(@PathVariable Long id, @Valid @RequestBody PromptTemplate promptTemplate) {
        return ApiResponse.ok(promptTemplateService.update(id, promptTemplate));
    }
}
