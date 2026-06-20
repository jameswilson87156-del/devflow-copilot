package com.devflow.copilot.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.devflow.copilot.common.BusinessException;
import com.devflow.copilot.entity.PromptTemplate;
import com.devflow.copilot.mapper.PromptTemplateMapper;
import com.devflow.copilot.service.PromptTemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PromptTemplateServiceImpl implements PromptTemplateService {

    private final PromptTemplateMapper mapper;

    public PromptTemplateServiceImpl(PromptTemplateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<PromptTemplate> list(String templateType) {
        return mapper.selectList(Wrappers.<PromptTemplate>lambdaQuery()
                .eq(templateType != null && !templateType.isBlank(), PromptTemplate::getTemplateType, templateType)
                .orderByDesc(PromptTemplate::getIsDefault)
                .orderByDesc(PromptTemplate::getUpdatedAt));
    }

    @Override
    @Transactional
    public PromptTemplate create(PromptTemplate template) {
        ensureKeyAvailable(template.getTemplateKey(), null);
        LocalDateTime now = LocalDateTime.now();
        template.setId(null);
        template.setEnabled(template.getEnabled() == null || template.getEnabled());
        template.setIsDefault(Boolean.TRUE.equals(template.getIsDefault()));
        template.setVersion(1);
        template.setCreatedAt(now);
        template.setUpdatedAt(now);
        if (template.getIsDefault()) {
            clearDefault(template.getTemplateType());
        }
        mapper.insert(template);
        return template;
    }

    @Override
    @Transactional
    public PromptTemplate update(Long id, PromptTemplate incoming) {
        PromptTemplate exists = getById(id);
        ensureKeyAvailable(incoming.getTemplateKey(), id);
        exists.setTemplateKey(incoming.getTemplateKey());
        exists.setTemplateName(incoming.getTemplateName());
        exists.setTemplateType(incoming.getTemplateType());
        exists.setTemplateContent(incoming.getTemplateContent());
        exists.setVariables(incoming.getVariables());
        exists.setEnabled(incoming.getEnabled() == null ? exists.getEnabled() : incoming.getEnabled());
        exists.setIsDefault(incoming.getIsDefault() == null ? exists.getIsDefault() : incoming.getIsDefault());
        exists.setVersion((exists.getVersion() == null ? 0 : exists.getVersion()) + 1);
        exists.setUpdatedAt(LocalDateTime.now());
        if (Boolean.TRUE.equals(exists.getIsDefault())) {
            clearDefault(exists.getTemplateType());
        }
        mapper.updateById(exists);
        return getById(id);
    }

    @Override
    public PromptTemplate getById(Long id) {
        PromptTemplate template = mapper.selectById(id);
        if (template == null) {
            throw new BusinessException(4042, HttpStatus.NOT_FOUND, "Prompt 模板不存在：" + id);
        }
        return template;
    }

    @Override
    public PromptTemplate findEnabledDefault(String generationType) {
        return mapper.selectOne(Wrappers.<PromptTemplate>lambdaQuery()
                .eq(PromptTemplate::getTemplateType, generationType)
                .eq(PromptTemplate::getEnabled, true)
                .orderByDesc(PromptTemplate::getIsDefault)
                .orderByDesc(PromptTemplate::getVersion)
                .last("LIMIT 1"));
    }

    private void ensureKeyAvailable(String templateKey, Long ignoredId) {
        Long count = mapper.selectCount(Wrappers.<PromptTemplate>lambdaQuery()
                .eq(PromptTemplate::getTemplateKey, templateKey)
                .ne(ignoredId != null, PromptTemplate::getId, ignoredId));
        if (count > 0) {
            throw new BusinessException(4092, HttpStatus.CONFLICT, "模板 Key 已存在：" + templateKey);
        }
    }

    private void clearDefault(String type) {
        PromptTemplate patch = new PromptTemplate();
        patch.setIsDefault(false);
        mapper.update(patch, Wrappers.<PromptTemplate>lambdaUpdate()
                .eq(PromptTemplate::getTemplateType, type)
                .eq(PromptTemplate::getIsDefault, true));
    }
}
