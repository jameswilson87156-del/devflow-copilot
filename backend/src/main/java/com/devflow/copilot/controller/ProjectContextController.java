package com.devflow.copilot.controller;

import com.devflow.copilot.common.ApiResponse;
import com.devflow.copilot.entity.ProjectContext;
import com.devflow.copilot.service.ProjectContextService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectContextController {

    private final ProjectContextService projectContextService;

    public ProjectContextController(ProjectContextService projectContextService) {
        this.projectContextService = projectContextService;
    }

    @PostMapping
    public ApiResponse<ProjectContext> create(@Valid @RequestBody ProjectContext projectContext) {
        return ApiResponse.ok(projectContextService.create(projectContext));
    }

    @GetMapping
    public ApiResponse<List<ProjectContext>> list() {
        return ApiResponse.ok(projectContextService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectContext> get(@PathVariable Long id) {
        return ApiResponse.ok(projectContextService.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProjectContext> update(@PathVariable Long id, @Valid @RequestBody ProjectContext projectContext) {
        return ApiResponse.ok(projectContextService.update(id, projectContext));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        projectContextService.delete(id);
        return ApiResponse.ok(null);
    }
}
