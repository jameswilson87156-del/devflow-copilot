package com.devflow.copilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project_context")
public class ProjectContext {

    @TableId(type = IdType.AUTO)
    private Long id;
    @NotBlank(message = "项目名称不能为空")
    @Size(max = 128, message = "项目名称不能超过 128 个字符")
    private String projectName;
    @Size(max = 512, message = "技术栈不能超过 512 个字符")
    private String techStack;
    private String readmeContent;
    private String directoryStructure;
    private String currentRequirement;
    private String codingRules;
    @Version
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
