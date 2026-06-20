package com.devflow.copilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LogAnalyzeRequest {

    @Positive(message = "projectId 必须为正整数")
    private Long projectId;
    @NotBlank(message = "日志内容不能为空")
    @Size(max = 50000, message = "日志内容不能超过 50000 个字符")
    private String rawLog;
}
