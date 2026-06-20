package com.devflow.copilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prompt_template")
public class PromptTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;
    @NotBlank(message = "模板 Key 不能为空")
    @Size(max = 128, message = "模板 Key 不能超过 128 个字符")
    private String templateKey;
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 128, message = "模板名称不能超过 128 个字符")
    private String templateName;
    @NotBlank(message = "模板类型不能为空")
    private String templateType;
    @NotBlank(message = "模板内容不能为空")
    @Size(max = 20000, message = "模板内容不能超过 20000 个字符")
    private String templateContent;
    private String variables;
    private Boolean enabled;
    private Boolean isDefault;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
