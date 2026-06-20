package com.devflow.copilot.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.devflow.copilot.common.BusinessException;
import com.devflow.copilot.entity.ProjectContext;
import com.devflow.copilot.mapper.ProjectContextMapper;
import com.devflow.copilot.service.ProjectContextService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjectContextServiceImpl implements ProjectContextService {

    private final ProjectContextMapper mapper;

    public ProjectContextServiceImpl(ProjectContextMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ProjectContext create(ProjectContext projectContext) {
        LocalDateTime now = LocalDateTime.now();
        projectContext.setId(null);
        projectContext.setVersion(0);
        projectContext.setCreatedAt(now);
        projectContext.setUpdatedAt(now);
        mapper.insert(projectContext);
        return projectContext;
    }

    @Override
    public List<ProjectContext> list() {
        return mapper.selectList(Wrappers.<ProjectContext>lambdaQuery()
                .orderByDesc(ProjectContext::getUpdatedAt));
    }

    @Override
    public ProjectContext getById(Long id) {
        ProjectContext project = mapper.selectById(id);
        if (project == null) {
            throw new BusinessException(4041, HttpStatus.NOT_FOUND, "项目不存在：" + id);
        }
        return project;
    }

    @Override
    public ProjectContext update(Long id, ProjectContext incoming) {
        ProjectContext exists = getById(id);
        exists.setProjectName(incoming.getProjectName());
        exists.setTechStack(incoming.getTechStack());
        exists.setReadmeContent(incoming.getReadmeContent());
        exists.setDirectoryStructure(incoming.getDirectoryStructure());
        exists.setCurrentRequirement(incoming.getCurrentRequirement());
        exists.setCodingRules(incoming.getCodingRules());
        exists.setUpdatedAt(LocalDateTime.now());
        if (mapper.updateById(exists) != 1) {
            throw new BusinessException(4091, HttpStatus.CONFLICT, "项目已被其他操作更新，请刷新后重试");
        }
        return getById(id);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        mapper.deleteById(id);
    }
}
