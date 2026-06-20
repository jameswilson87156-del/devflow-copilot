package com.devflow.copilot.service;

import com.devflow.copilot.entity.ProjectContext;

import java.util.List;

public interface ProjectContextService {

    ProjectContext create(ProjectContext projectContext);

    List<ProjectContext> list();

    ProjectContext getById(Long id);

    ProjectContext update(Long id, ProjectContext projectContext);

    void delete(Long id);
}
