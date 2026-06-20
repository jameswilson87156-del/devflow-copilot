# Acceptance Checklist

## Build

- [x] 后端构建通过：`mvn clean package`
- [x] 前端构建通过：`npm run build`
- [x] Vite 仅有 chunk size warning，不影响运行

## Pages

- [x] 封板验收地址：`http://127.0.0.1:5174`
- [x] 封板后端地址：`http://127.0.0.1:18090`
- [x] Dashboard 可访问，第一屏呈现 AI Coding 工具感，不是普通后台首页
- [x] Workbench 三栏稳定：Project Context / Task Composer / AI Result Panel
- [x] Workbench 可跑通：Run Workflow → Save Record → Mark Confirmed
- [x] Log Analyzer 可用，示例按钮可填充日志
- [x] Generation History 可查看记录，支持项目和类型筛选
- [x] Prompt Templates 卡片式展示，支持编辑和启用状态切换

## Demo Data

- [x] `ai-jd-analyzer` 投递复盘需求拆解可用
- [x] `power-plant-system` 端口占用日志可用
- [x] `power-plant-system` BeanCreationException 日志可用
- [x] `DevFlow Copilot` Workbench 优化修复 Prompt 可用

## Documentation

- [x] README 截图已补齐
- [x] `docs/api.md` JSON 示例为 UTF-8 正常中文
- [x] `docs/interview-guide.md` 可用于面试讲解
- [x] `docs/demo-script.md` 可用于录屏演示
- [x] `docs/architecture.md` 包含 Mermaid 架构和流程图

## MVP Boundary

- [x] 不接真实 LLM API
- [x] 不做登录权限
- [x] 不做 GitHub OAuth
- [x] 不做 IDE 插件
- [x] 不自动改代码
- [x] 不自动提交 Git
- [x] 所有 AI 输出都需要人工确认
