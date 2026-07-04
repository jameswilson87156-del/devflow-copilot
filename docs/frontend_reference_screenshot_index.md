# 前端公开参考截图索引

采集时间：2026-07-04

## 采集说明

- 联网状态：成功
- 采集方式：本地 Playwright 自动化访问公开页面并截图
- 登录状态：未登录任何账号
- 访问边界：未绕过权限、付费墙、验证码、反爬或访问限制
- 本地截图目录：`.local/reference_screenshots/`
- Git 处理规则：`.local/` 与 `.local/reference_screenshots/` 已加入 `.gitignore`，这些第三方参考截图不应进入 Git
- README 边界：这些截图仅用于内部视觉分析，不能复制到 `docs/images/`，也不能用于 README 主展示
- 编号说明：用户给出的示例文件名到 `15_promptfoo_github.png`，但实际目标来源共有 16 个，因此本轮补充生成了 `16_promptfoo_github.png`

## 采集结果总表

| 文件 | 来源名称 | URL | 访问结果 | 适合参考的 DevFlow 页面 | 可借鉴点 | 不能照搬点 | 第一阶段采用 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `01_ant_design_pro_analysis.png` | Ant Design Pro Dashboard Analysis | `https://preview.pro.ant.design/dashboard/analysis` | 成功 | Dashboard | 企业级中后台 dashboard 信息层级、指标卡与图表混排、顶部筛选区 | Ant Design Pro 品牌、完整页面布局、图表样式与文案 | 是 |
| `02_ant_design_pro_monitor.png` | Ant Design Pro Dashboard Monitor | `https://preview.pro.ant.design/dashboard/monitor` | 成功 | Dashboard / Trace Evidence | 监控页的多图表编排、状态看板、趋势与分栏结构 | 页面整体拷贝、品牌元素、具体监控卡片模板 | 部分 |
| `03_arco_design_home.png` | Arco Design | `https://arco.design/` | 成功 | 整体设计系统 / 导航 | 组件展示层级、留白控制、企业级设计系统表达 | 品牌、首页视觉、组件官网排版 | 部分 |
| `04_semi_design_home.png` | Semi Design | `https://semi.design/en-US/` | 成功 | 整体设计系统 / 导航 | 设计系统官网的模块组织、组件与文档入口组织 | 官网首页布局、品牌、文案与插图 | 部分 |
| `05_google_cloud_console_overview.png` | Google Cloud Console Product Page | `https://cloud.google.com/cloud-console` | 成功 | Dashboard / README 定位 | 云控制台产品价值表达、功能说明块、企业级控制台叙事 | Google Cloud 品牌、营销区块、配图与按钮文案 | 部分 |
| `06_google_cloud_monitoring_dashboards.png` | Google Cloud Monitoring Dashboards Docs | `https://docs.cloud.google.com/monitoring/dashboards` | 成功 | Dashboard / Log Analyzer / Trace Evidence | monitoring dashboard 文档结构、观测页面命名与术语 | 文档正文、截图、Google 品牌样式 | 是 |
| `07_huawei_cloud_console_overview.png` | Huawei Cloud Management Console Overview | `https://support.huaweicloud.com/intl/en-us/qs-consolehome/consolehome_qs_0002.html` | 成功 | Dashboard / Navigation | 控制台总览区块划分、总览/资源/运维等导航思路 | 华为云品牌、页面截图、原文说明与图示 | 部分 |
| `08_aliyun_sls_dashboard.png` | Alibaba Cloud SLS Dashboard Docs | `https://www.alibabacloud.com/help/en/sls/dashboard-overview` | 成功 | Dashboard / Log Analyzer | 日志服务 dashboard 概念、图表用于运维状态总览的组织方式 | 阿里云文档截图、品牌、产品术语直接照搬 | 是 |
| `09_langfuse_home_or_github.png` | Langfuse | `https://langfuse.com/` | 成功 | Trace Evidence / Dashboard / Prompt Templates | LLM observability、trace、prompt management 的模块分区 | 品牌、产品图、截图和功能宣称 | 是 |
| `10_langsmith_observability.png` | LangSmith Observability Docs | `https://docs.langchain.com/langsmith/observability` | 成功 | Trace Evidence / Human Review | observability、feedback、trace debugging 的结构和术语 | LangChain / LangSmith 品牌、文案、截图 | 是 |
| `11_helicone_home.png` | Helicone | `https://www.helicone.ai/` | 成功 | Prompt Templates / Dashboard | prompt / request / metrics 的分组表达 | 品牌、营销文案、官网视觉素材 | 部分 |
| `12_phoenix_github.png` | Arize Phoenix GitHub | `https://github.com/Arize-ai/phoenix` | 成功 | Trace Evidence / Prompt Templates | Tracing、evaluation、datasets、prompt management 的能力边界划分 | README 截图、Logo、功能承诺 | 部分 |
| `13_agentops_github.png` | AgentOps GitHub | `https://github.com/AgentOps-AI/agentops` | 成功 | Trace Evidence / Human Review | session drilldown、event breakdown、agent telemetry 结构 | README 图、品牌、SDK 能力照搬 | 是 |
| `14_dify_github.png` | Dify GitHub | `https://github.com/langgenius/dify` | 成功 | Knowledge Base / Workbench / Prompt Templates | Workflow、knowledge、model provider 的模块分区 | 产品图、workflow canvas、品牌与完整布局 | 是 |
| `15_openwebui_github.png` | Open WebUI GitHub | `https://github.com/open-webui/open-webui` | 成功 | Knowledge Base / Provider / Tool grouping | models、knowledge、tools 的平台导航分组 | 平台截图、品牌、未实现能力 | 部分 |
| `16_promptfoo_github.png` | Promptfoo GitHub | `https://github.com/promptfoo/promptfoo` | 成功 | Prompt Templates / README metrics | PromptOps、eval evidence、CLI + UI 双证据展示 | eval 矩阵截图、README 图、red teaming 文案 | 是 |

## 结果摘要

- 成功：16
- 失败：0
- 跳过：0
- 结论：本轮公开参考截图采集全部成功，可用于内部视觉分析和页面级参考绑定

## 使用边界

- 这些截图只用于内部视觉分析、信息架构对比和 moodboard 讨论。
- 不下载原站素材，不复用第三方 Logo、图片素材、产品截图到仓库。
- 不把 `.local/reference_screenshots/` 中的第三方截图复制到 `docs/images/` 或 README。
- DevFlow 最终展示图仍必须来自本地真实运行页面截图。
