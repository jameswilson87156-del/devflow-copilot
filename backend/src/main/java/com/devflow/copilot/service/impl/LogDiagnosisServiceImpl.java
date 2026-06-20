package com.devflow.copilot.service.impl;

import com.devflow.copilot.common.GenerationStatus;
import com.devflow.copilot.common.GenerationType;
import com.devflow.copilot.dto.LogAnalyzeRequest;
import com.devflow.copilot.dto.LogAnalyzeResponse;
import com.devflow.copilot.entity.GenerationRecord;
import com.devflow.copilot.entity.LogAnalysis;
import com.devflow.copilot.entity.ProjectContext;
import com.devflow.copilot.mapper.LogAnalysisMapper;
import com.devflow.copilot.service.GenerationRecordService;
import com.devflow.copilot.service.LogDiagnosisService;
import com.devflow.copilot.service.ProjectContextService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class LogDiagnosisServiceImpl implements LogDiagnosisService {

    private final LogAnalysisMapper logAnalysisMapper;
    private final ProjectContextService projectContextService;
    private final GenerationRecordService generationRecordService;

    public LogDiagnosisServiceImpl(
            LogAnalysisMapper logAnalysisMapper,
            ProjectContextService projectContextService,
            GenerationRecordService generationRecordService
    ) {
        this.logAnalysisMapper = logAnalysisMapper;
        this.projectContextService = projectContextService;
        this.generationRecordService = generationRecordService;
    }

    @Override
    public LogAnalyzeResponse analyze(LogAnalyzeRequest request) {
        ProjectContext project = resolveProject(request.getProjectId());
        String rawLog = request.getRawLog() == null ? "" : request.getRawLog();
        Diagnosis diagnosis = diagnose(rawLog);

        LogAnalysis logAnalysis = new LogAnalysis();
        logAnalysis.setProjectId(project.getId());
        logAnalysis.setRawLog(rawLog);
        logAnalysis.setExceptionType(diagnosis.exceptionType());
        logAnalysis.setPossibleReason(diagnosis.possibleReason());
        logAnalysis.setDiagnoseSteps(diagnosis.diagnoseSteps());
        logAnalysis.setFixPrompt(buildFixPrompt(project, rawLog, diagnosis));
        logAnalysis.setRiskTips(diagnosis.riskTips());
        logAnalysis.setRiskLevel(diagnosis.riskLevel());
        logAnalysis.setVersion(0);
        logAnalysis.setCreatedAt(java.time.LocalDateTime.now());
        logAnalysis.setUpdatedAt(logAnalysis.getCreatedAt());
        logAnalysisMapper.insert(logAnalysis);
        LogAnalysis savedLog = logAnalysis;

        GenerationRecord record = new GenerationRecord();
        record.setProjectId(project.getId());
        record.setGenerationType(GenerationType.LOG_ANALYSIS);
        record.setInputSummary(diagnosis.exceptionType());
        record.setInputContent(rawLog);
        record.setOutputContent(toMarkdown(savedLog));
        record.setStatus(GenerationStatus.READY_FOR_REVIEW);
        record.setConfirmed(false);
        record.setProviderName("local-rule");
        record.setModelName("local-rule-mvp");
        record.setCostTimeMs(80L);
        record.setPromptTokens(estimateTokens(rawLog));
        record.setCompletionTokens(estimateTokens(record.getOutputContent()));
        record.setTotalTokens(record.getPromptTokens() + record.getCompletionTokens());
        record.setSuccess(true);
        GenerationRecord savedRecord = generationRecordService.save(record);

        return LogAnalyzeResponse.builder()
                .id(savedLog.getId())
                .recordId(savedRecord.getId())
                .exceptionType(savedLog.getExceptionType())
                .possibleReason(savedLog.getPossibleReason())
                .diagnoseSteps(savedLog.getDiagnoseSteps())
                .fixPrompt(savedLog.getFixPrompt())
                .riskTips(savedLog.getRiskTips())
                .riskLevel(savedLog.getRiskLevel())
                .build();
    }

    @Override
    public List<LogAnalysis> history(Long projectId) {
        return logAnalysisMapper.selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers.<LogAnalysis>lambdaQuery()
                .eq(projectId != null, LogAnalysis::getProjectId, projectId)
                .orderByDesc(LogAnalysis::getCreatedAt));
    }

    private ProjectContext resolveProject(Long projectId) {
        if (projectId != null) {
            return projectContextService.getById(projectId);
        }
        return projectContextService.list().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Please create a project context first."));
    }

    private Diagnosis diagnose(String rawLog) {
        String text = rawLog.toLowerCase(Locale.ROOT);
        if (text.contains("nullpointerexception")) {
            return new Diagnosis(
                    "NullPointerException",
                    "对象为空但仍被调用，常见于请求参数缺失、Mapper 查询为空、依赖未初始化或 Optional 未处理。",
                    "1. 定位第一段业务代码堆栈。\n2. 检查空对象来源：请求体、数据库结果、配置项或 Bean 注入。\n3. 增加参数校验、空值分支或默认值。\n4. 补充单元测试覆盖空输入。",
                    "Medium",
                    "不要只在报错行加 if-null，应回到数据来源修复。"
            );
        }
        if (text.contains("sqlsyntaxerrorexception") || text.contains("bad sql grammar")) {
            return new Diagnosis(
                    "SQLSyntaxErrorException",
                    "SQL 语法错误、字段名不匹配、表不存在、关键字冲突或 MyBatis XML/注解 SQL 拼接错误。",
                    "1. 复制最终执行 SQL 到 MySQL 客户端验证。\n2. 检查表名、字段名、驼峰下划线映射。\n3. 检查动态 SQL 是否产生多余逗号或空 where。\n4. 对照 init.sql 或迁移脚本修正结构。",
                    "High",
                    "涉及数据库结构变更时，必须提供可回滚 SQL。"
            );
        }
        if (text.contains("communications link failure") || text.contains("connection refused")) {
            return new Diagnosis(
                    "Communications link failure",
                    "应用无法连接 MySQL，可能是服务未启动、端口错误、账号密码错误、网络阻断或连接 URL 参数不正确。",
                    "1. 确认 MySQL 服务和端口。\n2. 使用命令行客户端验证账号密码。\n3. 检查 application.yml 的 url、username、password。\n4. 检查 serverTimezone、SSL、字符集配置。",
                    "High",
                    "不要把真实数据库密码提交到 README 或仓库。"
            );
        }
        if (text.contains("beancreationexception")) {
            return new Diagnosis(
                    "BeanCreationException",
                    "Spring Bean 创建失败，常见于配置缺失、构造器依赖循环、Mapper 扫描失败或 Bean 初始化异常。",
                    "1. 向下查找 caused by 根因。\n2. 检查 @Service、@MapperScan、@Configuration 是否生效。\n3. 检查构造器注入是否循环依赖。\n4. 如果是配置属性，确认 profile 与环境变量。",
                    "High",
                    "不要只处理最外层 BeanCreationException，根因通常在 caused by。"
            );
        }
        if (text.contains("port") && (text.contains("already in use") || text.contains("端口") || text.contains("address already in use"))) {
            return new Diagnosis(
                    "Port already in use",
                    "启动端口已被其他进程占用。",
                    "1. 使用 netstat 或 Get-NetTCPConnection 找到占用进程。\n2. 结束进程或修改 server.port。\n3. 前后端代理端口要同步调整。\n4. 重新启动并验证健康接口。",
                    "Low",
                    "修改端口后，前端代理和 README 启动说明需要同步。"
            );
        }
        if (text.contains("classnotfoundexception") || text.contains("noclassdeffounderror")) {
            return new Diagnosis(
                    "ClassNotFoundException",
                    "运行期缺少类，通常是 Maven 依赖未引入、scope 设置错误、版本冲突或打包遗漏。",
                    "1. 确认 pom.xml 依赖和版本。\n2. 执行 mvn dependency:tree 查看冲突。\n3. 检查依赖 scope 是否误设为 provided/test。\n4. 重新 clean package。",
                    "Medium",
                    "不要随意升级大版本依赖，先确认兼容矩阵。"
            );
        }
        if (text.contains("nosuchbeandefinitionexception")) {
            return new Diagnosis(
                    "NoSuchBeanDefinitionException",
                    "Spring 容器找不到目标 Bean，可能是注解缺失、包扫描范围错误、条件装配未满足或接口实现未注册。",
                    "1. 检查实现类是否有 @Service/@Component。\n2. 检查启动类包路径是否覆盖实现类。\n3. 检查 @ConditionalOnProperty 等条件。\n4. 如果有多个实现，确认 @Qualifier 或 @Primary。",
                    "Medium",
                    "优先修复装配边界，不建议用 new 手动创建 Spring Bean。"
            );
        }
        if (text.contains("could not resolve dependencies") || text.contains("dependencyresolutionexception") || text.contains("failed to collect dependencies")) {
            return new Diagnosis(
                    "Maven dependency error",
                    "Maven 依赖解析失败，可能是仓库不可达、版本不存在、镜像配置错误或依赖冲突。",
                    "1. 检查 pom.xml 中依赖坐标是否存在。\n2. 切换或修复 Maven settings.xml 镜像。\n3. 执行 mvn -U clean package 刷新依赖。\n4. 使用 dependency:tree 定位冲突。",
                    "Medium",
                    "不要删除整个本地 Maven 仓库，优先清理单个异常依赖目录。"
            );
        }
        return new Diagnosis(
                "Unknown Java/Spring Boot exception",
                "当前日志未命中内置规则，需要进一步查看 caused by、业务堆栈和启动阶段。",
                "1. 找到第一段 caused by。\n2. 区分启动失败、请求失败、数据库失败还是依赖失败。\n3. 复制业务包名附近 20 行堆栈。\n4. 结合最近一次代码变更缩小范围。",
                "Medium",
                "日志不足时不要直接修改代码，应先补充上下文。"
        );
    }

    private String buildFixPrompt(ProjectContext project, String rawLog, Diagnosis diagnosis) {
        return """
                你是资深 Java/Spring Boot 工程师，请基于以下上下文分析并修复问题。

                项目：%s
                技术栈：%s
                异常类型：%s
                可能原因：%s

                日志：
                %s

                要求：
                1. 先指出根因，不要直接大范围重构。
                2. 给出最小修改文件和修改步骤。
                3. 保留人工确认，所有修复建议等待开发者审查。
                4. 修复后执行 mvn clean package，并说明验证结果。
                """.formatted(project.getProjectName(), project.getTechStack(), diagnosis.exceptionType(), diagnosis.possibleReason(), rawLog);
    }

    private String toMarkdown(LogAnalysis log) {
        return """
                ## 异常类型

                %s

                ## 可能原因

                %s

                ## 排查步骤

                %s

                ## 修复 Prompt

                ```text
                %s
                ```

                ## 风险提示

                - 风险等级：%s
                - %s
                """.formatted(log.getExceptionType(), log.getPossibleReason(), log.getDiagnoseSteps(),
                log.getFixPrompt(), log.getRiskLevel(), log.getRiskTips());
    }

    private int estimateTokens(String text) {
        return text == null || text.isBlank() ? 0 : Math.max(1, (int) Math.ceil(text.length() / 3.5));
    }

    private record Diagnosis(
            String exceptionType,
            String possibleReason,
            String diagnoseSteps,
            String riskLevel,
            String riskTips
    ) {
    }
}
