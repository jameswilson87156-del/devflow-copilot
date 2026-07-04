# Claude / Codex 交接记录

使用规则：每轮把新记录追加在“历史记录”顶部，不覆盖旧记录。没有证据时不要写“测试通过”。

## 2026-07-04 - Codex - Trace Evidence 目标图单页重做

- 本轮目标：只重做 Trace Evidence 页面，确认并使用 `.local/visual_targets/trace-evidence-target.png` 作为唯一视觉参考；未做 Dashboard、Workbench、Human Review，未根据公开参考图自由设计。
- 修改范围：重做 `frontend/src/views/AgentRunTraceView.vue`；为贴近目标图轻量调整 `frontend/src/router/index.ts`、`frontend/src/layouts/DevFlowLayout.vue`、`frontend/src/components/SidebarNav.vue`、`frontend/src/components/TopBar.vue`、`frontend/src/styles/theme.css`；更新 `scripts/capture-portfolio-screenshots.mjs` 的单页截图能力；刷新 `docs/images/trace-evidence.png` 和 `docs/metrics/metrics_snapshot.md`。
- 页面结果：Trace Evidence 现在包含左侧导航、当前工作空间、中文分组导航、选中的 `执行证据 Trace`、底部本地 Demo 模式卡片、顶部面包屑 / 搜索 / Demo Data / local-rule fallback / OpenAI-compatible 可选标签、页面标题区、当前执行摘要 5 个指标卡、左中右三栏主体和底部证据详情。
- 三栏结构：左栏为运行记录列表且当前项高亮；中栏为 `01 Prompt 渲染` 到 `07 复核已确认` 的执行时间线，展示状态、耗时、Provider / Model；右栏为步骤详情、风险与证据、复核结果和状态历史；底部证据详情默认展示 Raw JSON，并保留 Rendered Prompt / Fallback Reason / Tool I/O 标签。
- 截图：临时后端 `127.0.0.1:18081`、临时前端 `127.0.0.1:5176`，通过 `VITE_API_PROXY_TARGET=http://127.0.0.1:18081` 连接本地真实 API；执行 `node scripts/capture-portfolio-screenshots.mjs --only=trace-evidence` 生成 `docs/images/trace-evidence.png`，视口为 1440x900，不使用目标图、聊天截图或第三方截图替代。
- 数据边界：页面数据来自本地 H2 Demo、现有 Agent Run Trace / Generation Trace / Knowledge Reference / Human Review 接口和 local-rule 生成记录；页面明确展示 `Demo Data`、`local-rule fallback`、`OpenAI-compatible 可选` 和不连接真实 API Key；未改后端核心业务逻辑，未接真实 API Key，未把 local-rule 写成真实 LLM。
- 验证结果：`cd frontend && npm run build` 通过；`cd backend && mvn test` 通过，`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`；`node scripts/collect-portfolio-metrics.js --run-checks` 通过并刷新 metrics；`node scripts/capture-portfolio-screenshots.mjs --only=trace-evidence` 通过。前端 build 仍有既有 VueUse PURE 注释提示和 Element Plus / Markdown 大 chunk 警告。
- 待收尾：提交前还需执行 `git diff --check` 并确认 git diff 只包含本轮 Trace Evidence 相关文件。

## 2026-07-04 - Codex - Trace Evidence 冻结与 Dashboard / Workbench 第一阶段收口

- 本轮目标：把本地真实 `docs/images/trace-evidence.png` 作为第一阶段视觉基准，冻结 Trace Evidence 大结构，并把同一套深色企业级 AI DevTools 视觉语言收口到 Dashboard 与 Workbench；未继续重写 Knowledge Base / Prompt Templates，Human Review 只纳入既有第一阶段成果与截图。
- 设计决策：沿用 Trace Evidence 的深色背景、220px 左侧导航、顶部 command bar、Runtime Mint 克制高亮、弱边框、低阴影、中文企业级文案、等宽技术字段、状态小圆点和 Demo Data 边界说明。Dashboard 以产品总览和证据链为主，Workbench 以需求、上下文、Artifact、Trace、Tool Call、Human Review 三栏闭环为主，不采用普通 CRUD 后台或聊天页结构。
- Trace Evidence：新增 `docs/design_reviews/trace_evidence_review.md`，记录“阶段通过”；页面只统一 fallback 文案为 `local-rule fallback；本地演示模式不连接真实 API Key。`，未继续改大结构。
- Dashboard / Workbench：Dashboard 的待人工复核改为从现有 Agent Run Trace 实际状态派生，不再把总审核数冒充待复核；Metrics Snapshot 对齐当前 8 个页面、8 个真实路由、30 个 endpoint、20 个测试和 13 张截图。Workbench 修复 Knowledge References 长标题与 score 在 1440px 截图中的重叠。
- 截图脚本：`scripts/capture-portfolio-screenshots.mjs` 继续使用 1440x1000 viewport；新增 canonical 输出 `dashboard.png`、`workbench.png`、`trace-evidence.png`、`human-review.png`，同时刷新现有 README 兼容别名。脚本增加前端 `/api` proxy 数据校验，避免 API 预热成功但页面误连其他 8080 服务时生成空数据截图。
- 真实截图：后端临时启动在 `127.0.0.1:18081`，前端临时启动在 `127.0.0.1:5175`，并使用 `VITE_API_PROXY_TARGET=http://127.0.0.1:18081`。截图前通过真实 API 创建并确认 local-rule Demo Artifact；四张 canonical 截图已逐张检查，均来自当前项目本地真实路由，不是聊天截图、第三方截图或 AI concept 图。
- 验证结果：独立执行 `npm run build` 通过；独立执行 `mvn test` 通过，`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`；执行 `npm run screenshots:portfolio` 通过；执行 `node scripts/collect-portfolio-metrics.js --run-checks` 通过，内部 build / test 均通过；`git diff --check` 通过。
- 编码与边界：`TODO.md`、`HANDOFF.md`、`docs/metrics/metrics_snapshot.md`、`docs/design_reviews/trace_evidence_review.md` 的前三字节均为 UTF-8 BOM；未发现 `content_reference` 或替换字符；`.local/reference_screenshots/` 保持 ignored，未进入 Git；未提交真实 API Key，未声称真实 LLM、生产流量、向量数据库或完整多 Agent Runtime。
- 已知风险：Vite build 仍有既有 VueUse PURE 注释提示及 Element Plus / Markdown 大 chunk 警告；metrics 脚本的默认关键接口仍检查 8080，因此快照继续诚实标记该项“未采集”；Request Changes / 驳回仍是 disabled 预留控制，因为后端没有对应 API。

## 2026-07-04 - Codex - 第一阶段前端作品集化 UI 改造

- 做了什么：用户明确确认“这个前端方向，可以开工”后，按 `docs/frontend_reference_research.md`、`docs/frontend_moodboard.md`、`docs/frontend_reference_screenshot_index.md`、`docs/frontend_showcase_design.md` 和 `docs/metrics/metrics_snapshot.md` 执行第一阶段前端展示改造。未接真实 API Key，未复制第三方参考截图 / Logo / 文案，未把 local-rule 包装成真实 LLM。
- 全局视觉：`frontend/src/styles/theme.css` 切换为深色开发者工具风格、Runtime Mint 信号色、蓝黑 / 石墨黑背景、弱边框、低阴影、6px 圆角；App Shell 保持 220px Sidebar、52px Topbar、24px page padding、16px panel padding / grid gap。
- 导航：`frontend/src/router/index.ts` 重排为工作流、可观测性、知识与引用、治理与审核、配置；新增独立 `/reviews` Human Review 路由。Provider / Settings 保持 disabled，提示 `配置预留 / 当前通过环境变量配置`。
- Dashboard：`DashboardView.vue` 增加 Product Evidence Hero、本地 Demo / local-rule fallback / OpenAI-compatible 可选 / 本地 metrics_snapshot 数据来源声明；强化今日运行、成功率、平均耗时、待人工复核、工具调用、知识库命中、最近运行、Provider Health、Metrics Snapshot、执行证据等模块。
- Workbench：`WorkbenchView.vue` 强化 AI Coding 工作台信息架构：需求与上下文、Prompt 模板、Provider 状态、Knowledge References、Artifact Preview、Change Summary、Trace Summary、Tool Calls Timeline 和 Human Review 停点；`Request Changes` 作为 disabled 决策位，明确当前后端未提供 API。
- Trace Evidence：`AgentRunTraceView.vue` 重构为 Run list + Timeline / Steps / Spans + Step Inspector + Raw JSON / Rendered Prompt / Fallback Reason / Tool I/O tabs；Timeline 使用中文步骤 `01 Prompt 渲染` 至 `07 已确认 / 已驳回`，缺失字段显式标注未采集。
- Human Review：新增 `frontend/src/views/HumanReviewView.vue`，独立展示复核队列、风险标签、Artifact 预览、人工复核决策面板、复核原因、状态历史和为什么需要人工复核；通过按钮沿用现有 save/confirm 状态机，要求修改 / 驳回 / 重新生成保持 disabled 并说明后端未提供对应 API。
- Knowledge / Prompt：`KnowledgeBaseView.vue` 和 `PromptTemplatesView.vue` 仅做轻量视觉与链路文案对齐，分别突出 `Document -> Chunk -> Search -> Citation -> Generation Reference`、`Template -> Variables -> Render Preview -> Test Run -> Trace metadata`。
- 截图：`scripts/capture-portfolio-screenshots.mjs` viewport 改为 1440x1000，并将 Human Review 截图入口改为 `/reviews`。执行 `cd frontend && npm run screenshots:portfolio` 成功，生成 / 更新 `docs/images/dashboard-agentic.png`、`workbench-running.png`、`agent-run-trace.png`、`knowledge-base-rag.png`、`prompt-studio.png`、`human-review-trace-detail.png`，均来自本地真实运行页面。
- 指标：执行 `node scripts/collect-portfolio-metrics.js --run-checks` 成功并刷新 `docs/metrics/metrics_snapshot.md`；当前快照显示前端页面文件 8 个、真实 component route 8 个、redirect route 1 个、disabled nav item 3 个，`npm run build` 和 `mvn test` 均通过。
- 验证证据：`cd frontend && npm run build` 通过；`cd backend && mvn test` 通过，`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`；`git diff --check` 通过，仅 LF/CRLF 提示；敏感模式扫描未发现真实 Key 形态。
- 服务处理：为截图临时启动了后端 `18081` 和前端 `5175`，确认截图后已关闭本轮启动的进程；原本已存在的 `5174` 旧服务未改动。
- 保留边界：`.local/` 与 `.local/reference_screenshots/` 仍为 ignored；未把任何第三方参考截图放入 `docs/images` 或 README；README 本轮未修改。
- 下一步：建议用户先人工确认 `docs/images/` 最新截图，再决定是否进入第二阶段 README 截图展示、简历文案和页面细节 polish。

## 2026-07-04 - Codex - 公开前端参考截图采集与索引

- 做了什么：按用户新增要求，只做公开前端参考截图采集和索引，不改 Vue、CSS、README、截图脚本，不进入前端代码改造。先检查 `.gitignore`，确认原本未忽略 `.local/`，随后补充 `.local/` 和 `.local/reference_screenshots/`。
- 访问边界：只访问公开网页；未登录任何账号；未绕过权限、付费墙、验证码、反爬或访问限制；未下载/复用/提交第三方 Logo、图片素材或产品截图到 GitHub。
- 本地脚本：新增 `.local/capture-reference-screenshots.mjs` 作为 ignored 的本地 Playwright 采集脚本，只用于当前机器抓取公开参考截图。该脚本和所有截图都位于 `.local/`，不会纳入 Git。
- 实际采集：执行 `node .local/capture-reference-screenshots.mjs` 成功，输出到 `.local/reference_screenshots/`。本轮共采集 16 个目标页面，成功 16 个、失败 0 个、跳过 0 个。之所以有 `16_promptfoo_github.png`，是因为用户实际列出的目标来源共有 16 个。
- 已采集来源：Ant Design Pro Dashboard Analysis、Ant Design Pro Dashboard Monitor、Arco Design、Semi Design、Google Cloud Console、Google Cloud Monitoring Dashboards、Huawei Cloud Management Console Overview、Alibaba Cloud SLS Dashboard、Langfuse、LangSmith Observability、Helicone、Arize Phoenix GitHub、AgentOps GitHub、Dify GitHub、Open WebUI GitHub、Promptfoo GitHub。
- 新增索引：`docs/frontend_reference_screenshot_index.md`，记录采集时间、联网状态、来源名称、URL、本地截图文件名、访问结果、适合参考的 DevFlow 页面、可借鉴点、不能照搬点和第一阶段采用结论。文档已明确：这些截图仅用于内部视觉分析，不能复制到 `docs/images/` 或 README。
- 编码与 Git：`docs/frontend_reference_screenshot_index.md` 需要转为 UTF-8 with BOM；`.local/reference_screenshots/` 中的 PNG 和 `results.json` 不应 `git add`。本轮可提交改动应仅包含 `.gitignore`、`docs/frontend_reference_screenshot_index.md`、`TODO.md`、`HANDOFF.md`。
- 下一步：请先让用户确认这些公开参考截图是否用于更新前端视觉落地规格；确认前不要进入 UI 代码改造。

## 2026-07-04 - Codex - 作品集化只读审查、公开参考调研与指标采集地基

- 做了什么：按附件要求先做作品集化第一阶段，不直接改正式 UI 页面、不改后端业务逻辑、不新增生产依赖、不接真实 API Key。开始前已读取 `AGENTS.md`、`TODO.md`、`HANDOFF.md`、`docs/ARCHITECTURE.md`、附件请求；`docs/PRD.md` 和 `docs/DESIGN.md` 当前不存在，实际参考了根目录 `DESIGN.md`、`docs/frontend-design.md`、README、API 文档、路由、Controller、截图脚本、测试和 migration。
- 分支：从 `main` 创建并切换到 `feat/portfolio-showcase-v1`，开始修改前工作区无未提交 diff。
- 新增审查文档：`docs/portfolio_audit.md`，记录当前项目状态、已有优势、主要问题、展示风险、README 不一致点、前后端问题、可写入简历的数据、暂时不能写的数据和第一阶段建议优化方向。审查中特别记录 `docs/api.md` 的截图 demo backend `18090` 与截图脚本默认 `18081` 不一致。
- 新增参考调研：`docs/reference_research.md`，实际联网调研 OpenHands、Langfuse、Arize Phoenix、Dify、Flowise、Promptfoo、AgentOps。只记录可借鉴的信息架构、README 展示、Trace / Prompt / Review / Knowledge / Metrics 组织方式，明确不复制代码、样式、图片、Logo、商标或文案。
- 新增参考图索引：`docs/design_refs/README.md`。本目录当前无图片；项目已有参考图位于 `docs/design/references/`，且是 AI-generated concept images，只能作为布局/信息层级参考，不是 README 真实截图，也不是公开项目调研来源。
- 新增指标方案：`docs/metrics_plan.md`，按功能规模、工程质量、性能体验、AI 工作流效果 4 类定义指标，每项写明采集方式、命令、结果保存、是否可写简历、推荐表述和不能写的情况。
- 新增采集脚本：`scripts/collect-portfolio-metrics.js`，静态采集页面、路由、Controller endpoint、测试数量、migration、seed、截图、README 图片和基础敏感信息模式；传入 `--run-checks` 时执行 `npm run build` 和 `mvn test`，并统计 `frontend/dist/assets` bundle 文件。
- 新增指标快照：`docs/metrics/metrics_snapshot.md`，本轮采集到前端页面文件 7 个、真实 component route 7 个、redirect route 1 个、disabled nav item 4 个、后端 Controller 10 个、endpoint mapping 30 个、后端测试源码 `@Test` 20 个、Flyway migration 4 个、Prompt 模板 seed 6 条、Generation Record seed 6 条、Knowledge Document seed 2 条、Knowledge Chunk seed 4 条、`docs/images` PNG 11 张、README 图片引用 6 张。后端默认 `http://127.0.0.1:8080/api/dashboard/stats` 返回 HTTP 404，本轮未把接口耗时写成已采集。
- 新增改造计划：`docs/showcase_upgrade_plan.md`，定义后续 Dashboard、Workbench、Prompt Templates、Trace Evidence、Human Review 的作品集化信息架构、状态覆盖、视觉方向、组件需求、README 升级计划和分阶段实施建议。
- 编码处理：本轮新增中文文档按附件要求转为 UTF-8 with BOM；已抽查 `docs/portfolio_audit.md`、`docs/reference_research.md`、`docs/metrics/metrics_snapshot.md` 前三字节为 `239 187 191`。
- 验证证据：`node scripts/collect-portfolio-metrics.js --run-checks` 成功，内部执行 `npm run build` 通过、`mvn test` 通过；`backend/target/surefire-reports/*.txt` 显示 5 个测试类合计 `Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`。前端 build 产物已生成，最大 chunk 仍主要来自 `vendor-markdown` 和 `vendor-element-plus`，这是既有 bundle 体积优化项。
- 边界保持：未提交真实 API Key；未把 `local-rule` 写成真实 LLM 推理；未把 Knowledge Base 写成向量数据库；未把 Agent Run Trace 写成完整多 Agent Runtime；未运行 Docker；未把 demo seed / UI-only / mock 写成生产数据。
- 下一步：建议先升级 README 作品集指标小节，引用 `docs/metrics/metrics_snapshot.md`，并补充 dev / Docker / screenshot / production demo 端口用途表；之后再单独处理 Human Review / Trace Evidence 展示补强。

## 2026-06-26 - Codex - Production Demo Readiness / 部署前安全收口

- 做了什么：只做部署前安全收口，不继续改 UI 页面、不改后端核心业务逻辑、不新增接口或复杂功能。先审查 `README.md`、`docs/deployment-plan.md`、`docs/real-provider-verification.md`、`docs/design/references/README.md`、`docker-compose.yml`、后端配置、Vite 配置、`.gitignore`、截图脚本和当前 git status。
- 当前部署风险点：没有独立 `/api/health`，部署验收使用已有 `/api/dashboard/stats`；生产 profile 的 DB 默认值只能作示例，服务器上必须通过环境变量覆盖；前端生产访问依赖 Nginx `/api/` 反向代理，Vite proxy 只用于本地开发；公开 demo 应默认使用 `local-rule` 或启用 fallback，避免 API Key 和成本风险。
- 新增文档：`docs/deployment-production-demo.md`，覆盖项目定位、portfolio demo 边界、推荐 Nginx + Spring Boot 架构、服务器部署步骤、环境变量、安全说明和验收 checklist。明确不是 production SaaS、不自动改代码、不自动提交 Git、不实现完整多 Agent Runtime，Knowledge Base 仍是关键词检索 / RAG 引用。
- 新增示例：`docs/env.example` 只包含占位符；`docs/nginx/devflow-demo.conf.example` 使用 `devflow.example.com`、占位 `dist` 路径，并将 `/api/` 反代到 `127.0.0.1:8080`。没有真实域名、服务器 IP、证书路径或 API Key。
- README / 既有部署文档：README 新增 `Portfolio Demo 部署` 小节并链接 production demo 指南、env 示例和 Nginx 示例；`docs/deployment-plan.md` 修正过期的 `.env.example` 说明，改为引用 `docs/env.example`。
- 验证证据：`cd backend && mvn test` 成功，`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`；`cd frontend && npm run build` 成功；仍有既有 VueUse PURE 注释提示和 Element Plus / Markdown 大 chunk 警告。
- 下一步：建议提交本轮部署文档，然后将 `feat/frontend-design-system-foundation` 合并到 `main` 或创建 PR 后合并。部署服务器时只在服务器环境变量或 Secret Manager 配置真实 Provider Key，不写入仓库。

## 2026-06-26 - Codex - 真实浏览器截图与 README 收口

- 做了什么：只做前端中文版 UI 重设计收口，不继续大改页面、不改后端核心逻辑、不新增接口。复查 `scripts/capture-portfolio-screenshots.mjs`、README 截图区域、`docs/images/`、`docs/design/references/README.md` 和前端路由，确认截图脚本覆盖 Dashboard、Workbench、Agent Run Trace、Knowledge Base、Prompt Studio，并额外覆盖 Human Review Trace。
- 服务与截图：本机后端 `http://127.0.0.1:18081/api/dashboard/stats` 可访问，前端 `http://127.0.0.1:5174` 可访问。执行 `cd frontend && npm run screenshots:portfolio` 成功，通过真实后端 API 预热 demo Agent Workflow，再从真实浏览器页面输出截图到 `docs/images/`。
- 实际截图文件：`docs/images/dashboard-agentic.png`、`docs/images/workbench-running.png`、`docs/images/agent-run-trace.png`、`docs/images/knowledge-base-rag.png`、`docs/images/prompt-studio.png`、`docs/images/human-review-trace-detail.png`，均为 1440x1040 PNG。本轮没有使用 `docs/design/references/` 图片作为 README 主截图。
- README：顶部截图展示区补充 Prompt Studio 和 Human Review Trace；项目截图小节明确 `docs/images/` 是真实浏览器运行截图，`docs/design/references/` 是 AI-generated visual references，不是 runtime screenshots；核心功能文案补充 Prompt Studio、Tool Call、Generation Trace / Human Review 关联，并继续保持 Knowledge Base 关键词检索、非完整多 Agent Runtime、非自动改代码系统等边界。
- 截图脚本：本轮没有修改 `scripts/capture-portfolio-screenshots.mjs`。脚本现有等待选择器仍可进入 `/`、`/workbench`、`/agent-runs`、`/knowledge`、`/prompts` 并完成截图。
- 验证证据：`cd backend && mvn test` 成功，`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`；`cd frontend && npm run build` 成功；仍有既有 VueUse PURE 注释提示和 Element Plus / Markdown 大 chunk 警告。后续可做 bundle split / manual chunks 优化。
- 下一步：建议将 `feat/frontend-design-system-foundation` 合并到 `main`，或创建 PR 后合并。合并前继续检查不提交 API Key、`.env`、`node_modules`、`dist`、`target` 或日志文件。

## 2026-06-26 - Codex - Prompt Studio 高保真中文版重构

- 做了什么：只重构 Prompt Studio，不改后端、不新增接口、不改 Dashboard、Workbench、Agent Run Trace、Knowledge Base。基于 `docs/design/references/05-prompt-studio-ai-concept-cn.png` 的提示词工作室结构，把 `PromptTemplatesView.vue` 改成左 Prompt 模板列表、中 Prompt 编辑器与渲染预览、右测试运行预览与执行详情、底部可观测性的真实 Prompt 管理页。
- 页面内容：顶部展示 `Prompt 模板 -> 变量配置 -> 渲染预览 -> 测试运行 -> Trace / Human Review` 链路；左侧包含搜索、新建模板、类型筛选、状态筛选和模板列表；中间包含模板基础信息、System/User Prompt 派生预览、模板内容编辑、变量配置、原始 / 渲染 / JSON 预览、保存模板、保存版本和运行测试；右侧包含测试运行状态、Provider、Model、Token、Trace ID、原始响应、Human Review 和 Provider 配置提示；底部包含版本历史、测试结果、Prompt 校验、最近运行和变更记录。
- 保留真实功能：保留 `GET /api/prompts` 模板列表、`POST /api/prompts` 新建模板、`PUT /api/prompts/{id}` 保存模板并由后端递增当前版本、已有 `POST /api/ai/{type}` 试运行；`log-analysis` 仍可编辑保存，但因当前 `generateAi` 没有对应端点，页面禁用试运行而不是新增假接口。
- 使用真实接口：`GET /api/projects`、`GET /api/prompts`、`POST /api/prompts`、`PUT /api/prompts/{id}`、`GET /api/generations`、`POST /api/ai/requirement-split|code-plan|readme-generate|commit-message|fix-prompt`、`GET /api/generation-traces?generationRecordId={id}`、`GET /api/agent-runs?generationRecordId={id}`、`GET /api/agent-runs/{id}/trace`。
- 复用组件：`SectionCard`、`StatusBadge`、`ProviderBadge`、`CodeBlock`、`MetricCard`。本轮没有新增公共组件。
- fallback / 派生边界：后端没有独立版本历史表、PromptOps 评测后端、单独渲染预览 API、模板级使用次数 / 最近运行字段、独立 Prompt 校验记录；页面只展示当前版本记录、前端静态校验、从真实 Generation Record 派生的统计或空状态。`System Prompt` / `User Prompt` 没有独立字段，仅从 `templateContent` 标题片段安全推导，缺失时显示单字段说明。API Key 不在前端展示。
- 截图脚本：`scripts/capture-portfolio-screenshots.mjs` 的 Prompt Studio 路径仍是 `/prompts`，等待选择器 `.templates-page` 仍存在；本轮仅扩展试运行按钮匹配为 `运行测试|试运行|运行模板|Test`。未重新生成 `docs/images/` 作品集截图，建议下一轮统一截图并更新 README。
- 验证证据：`cd frontend && npm run build` 成功；仍有既有 VueUse PURE 注释提示和 Element Plus/Markdown 大 chunk 警告。
- 下一步：建议做统一截图任务，重新生成 Dashboard / Workbench / Knowledge Base / Prompt Studio 等真实运行截图，并更新 README 中的作品集截图引用。

## 2026-06-26 - Codex - Knowledge Base 高保真中文版重构

- 做了什么：只重构 Knowledge Base，不改后端、不新增接口、不改 Dashboard、Workbench、Agent Run Trace、Prompt Studio。基于 `docs/design/references/04-knowledge-base-ai-concept-cn.png` 的三栏知识管理结构，把 `KnowledgeBaseView.vue` 改成左文档列表、中详情与 Chunk、右检索结果与审核、底部可观测性的真实 RAG 引用管理页。
- 页面内容：顶部展示 `文档 -> Chunk -> 检索 -> 引用 -> 生成 -> Trace` 链路；左侧包含搜索、新增文档、来源筛选、状态筛选和文档列表；中间包含文档详情、Chunk 切片、命中结果、检索时间线、文档信息、访问控制和关键词检索；右侧包含检索结果详情、引用预览、生成使用情况、关联 Agent Run 和知识发布审核空状态；底部包含检索历史、引用历史、生成知识引用和索引日志。
- 保留真实功能：保留新增知识文档并后端切片、加载知识文档、加载文档 Chunk、关键词 / 简单相似度检索；文档选择后会刷新真实 Chunk 并用当前 query 重新检索。
- 使用真实接口：`GET /api/knowledge/documents`、`POST /api/knowledge/documents`、`GET /api/knowledge/documents/{id}/chunks`、`POST /api/knowledge/search`、`GET /api/knowledge/references?generationRecordId={id}`、`GET /api/generations`、`GET /api/agent-runs?generationRecordId={id}`。
- 复用组件：`SectionCard`、`StatusBadge`、`ProviderBadge`、`CodeBlock`、`MetricCard`。本轮没有新增公共组件。
- fallback / 派生边界：后端未提供文档发布状态、Chunk Token 数、检索日志持久化、索引耗时、知识发布审核人 / 意见、Trace ID 字段；页面从 `metadata status`、`chunkCount`、真实 generation references 和 Agent Run 记录安全派生，缺失时显示 `未记录`、`未关联 Trace` 或空状态。`embeddingModel / embeddingVector` 仅作为扩展点展示，没有写成向量数据库已启用。
- 截图脚本：`scripts/capture-portfolio-screenshots.mjs` 的 Knowledge Base 路径仍是 `/knowledge`，等待选择器 `.knowledge-page` 仍存在，本轮无需调整。未重新生成 `docs/images/` 作品集截图；后续建议统一截图。
- 验证证据：`cd frontend && npm run build` 成功；仍有既有 VueUse PURE 注释提示和 Element Plus/Markdown 大 chunk 警告。使用本机 Chrome 对 `http://127.0.0.1:5174/knowledge` 做过 DOM 烟测，确认标题、左侧、中间、右侧和底部区域存在；临时验证截图已删除，没有纳入本轮变更。
- 下一步：建议二选一：先做统一截图任务重新生成 Dashboard / Workbench / Knowledge Base 等真实运行截图，或单独做 Prompt Studio 高保真中文版重构；仍不要同时重构多个页面。

## 2026-06-26 - Codex - Workbench 高保真中文版重构

- 做了什么：只重构 Workbench，不改后端、不新增接口、不改 Dashboard、Agent Run Trace、Knowledge Base、Prompt Studio。基于 `docs/design/references/02-workbench-ai-concept-cn.png` 的三栏工作台结构，把 `WorkbenchView.vue` 改成左任务配置、中生成结果、右执行详情、底部追踪与日志的真实 AI Coding 工作台。
- 保留真实功能：保留 `运行工作流`、`保存记录`、`标记已审核 / 确认`；保留项目选择、Prompt 模板选择、知识检索 query、补充上下文输入和生成历史刷新；后端状态仍沿用 `READY_FOR_REVIEW -> SAVED -> CONFIRMED`。
- 使用真实接口：`GET /api/projects`、`GET /api/generations`、`GET /api/prompts`、`POST /api/ai/*`、`POST /api/generations/{id}/save`、`POST /api/generations/{id}/confirm`、`GET /api/generation-traces?generationRecordId={id}`、`GET /api/agent-runs?generationRecordId={id}`、`GET /api/agent-runs/{id}/trace`、`GET /api/knowledge/references?generationRecordId={id}`。
- 复用组件：`SectionCard`、`StatusBadge`、`ProviderBadge`、`CodeBlock`。本轮没有新增公共组件。
- fallback / 派生边界：任务标题、优先级、分支、上下文文件、关注区域、语言和约束是本页 UI-only 配置展示，后端未提供持久字段；diff、files_created、files_changed、tests_added、dependencies 只从响应 JSON 或文本安全派生，缺失时显示空状态；日志区只展示真实 `errorMessage`，无错误时显示空状态。
- 截图脚本：`scripts/capture-portfolio-screenshots.mjs` 的 Workbench 路径仍是 `/workbench`，等待选择器已更新为兼容新结果区 `.result-card .code-block`；本轮未重新生成 `docs/images/` 真实截图。
- 验证证据：`cd frontend && npm run build` 成功；仍有既有 VueUse PURE 注释提示和 Element Plus/Markdown 大 chunk 警告。待收尾执行 `git diff --check`、`git status --short` 和敏感文件/密钥检查。
- 下一步：建议先做统一截图任务重新生成 Dashboard / Workbench 真实运行截图，或单独重构 Knowledge Base；仍不要同时重构多个页面。

## 2026-06-26 - Codex - Dashboard 高保真中文版重构

- 做了什么：只重构 Dashboard，不改后端、不新增接口、不改 Workbench、Agent Run Trace、Knowledge Base、Prompt Studio。基于 `docs/design/references/01-dashboard-ai-concept-cn.png` 的信息层级，把 `DashboardView.vue` 改成高密度深色 DevFlow Copilot 仪表盘。
- 页面内容：新增 DevFlow Copilot Hero、Agentic Coding 工作流控制台副标题、Prompt / Provider / Trace / Knowledge / Human Review 说明、小型 Agent Workflow Overview、六项 KPI、最近智能体运行、启用中的 Prompt 模板、Provider 健康状态、最近人工审核、最新知识引用、最近活动时间线和高频工具。
- 复用组件：`MetricCard`、`SectionCard`、`StatusBadge`、`ProviderBadge`。本轮没有新增公共组件，也没有改其他页面。
- 使用真实接口：`GET /api/dashboard/stats`、`GET /api/prompts`、`GET /api/logs/history`、`GET /api/agent-runs`、`GET /api/agent-runs/{id}/trace`、`GET /api/knowledge/documents`、`GET /api/knowledge/references?generationRecordId={id}`。
- fallback / 派生边界：`dashboard/stats` 不直接提供工具调用总数、知识命中总数、Provider 健康明细或 Human Review 列表；Dashboard 只从现有 Trace / Knowledge 引用接口派生这些展示。缺失 provider/model/reviewer 时使用 `local-rule`、`local-rule`、`未分配` 作为安全展示 fallback；缺失 Tool Call 或 Knowledge 引用时显示 0 或空状态，不伪造成后端统计。
- 截图脚本：`scripts/capture-portfolio-screenshots.mjs` 的 Dashboard 路径仍是 `/`，等待选择器仍是 `.dashboard`，本轮没有破坏脚本选择器；未重新生成所有截图，建议下一轮或统一截图任务再跑作品集截图。
- 验证证据：`cd frontend && npm run build` 成功；仍有既有 VueUse PURE 注释提示和 Element Plus/Markdown 大 chunk 警告。待收尾执行 `git diff --check`、`git status --short` 和敏感文件/密钥检查。
- 下一步：建议单独做 Workbench 高保真重构，对齐 `docs/design/references/02-workbench-ai-concept-cn.png`，仍保持不改后端、不新增假接口。

## 2026-06-26 - Codex - 前端设计系统第一阶段地基

- 做了什么：只做前端基础设施，不改后端、不新增接口、不重构 5 个页面。先审查 router、layout、components、views、api、types、theme.css 和 5 张 `docs/design/references/` AI concept images，然后落地设计系统地基。
- 导航：`frontend/src/router/index.ts` 的导航文案统一为中文分组：工作流、可观测性、知识与引用、治理与审核、配置。已有真实路由继续保留；`工具调用`、`人工审核`、`Provider`、`设置` 作为 disabled 导航项，不创建假页面。
- Tokens：全局 design tokens 集中在 `frontend/src/styles/theme.css`，覆盖深色背景、卡片背景、蓝紫主色、成功/待审核/失败/运行中状态色、文本色、圆角、阴影、间距、字号、sidebar 宽度和 topbar 高度。
- 组件：新增 `SidebarNav.vue`、`TopBar.vue`、`MetricCard.vue`、`SectionCard.vue`、`StatusBadge.vue`、`ProviderBadge.vue`、`CodeBlock.vue`。本轮只把 `SidebarNav` / `TopBar` 接入 `DevFlowLayout`，把 `MetricCard` / `SectionCard` 轻量接入 `DashboardView`。
- 保留边界：未改 `WorkbenchView`、`AgentRunTraceView`、`KnowledgeBaseView`、`PromptTemplatesView` 的业务结构；未新增假接口；未把 `docs/design/references/` 当真实截图；未提交 API Key。
- 验证证据：`cd frontend && npm run build` 成功；仍有既有 VueUse PURE 注释提示和 Element Plus/Markdown 大 chunk 警告。`git diff --check` 退出码 0，仅有 LF/CRLF 提示；敏感内容扫描只命中文档占位符，没有真实 API Key。后端本轮未修改，未运行 `mvn test`。
- 下一步：建议下一轮只重构 Dashboard，对齐 `01-dashboard-ai-concept-cn.png` 的 Hero、KPI、运行、Prompt、Trace、Provider、Knowledge、Tool Call 和 Human Review 信息层级。

## 2026-06-26 - Codex - 前端 UI 中文版重构前审查

- 做了什么：按附件要求先审查，不直接大改。已阅读 `AGENTS.md`、`TODO.md`、`HANDOFF.md`、`README.md`、`docs/real-provider-verification.md`、`docs/resume-evidence.md`、`docs/api.md`、`docs/architecture.md`、根目录 `DESIGN.md`，并检查前端路由、API 封装、类型、布局、页面和截图脚本。
- 参考图：已查看 `docs/design/references/01-dashboard-ai-concept-cn.png` 至 `05-prompt-studio-ai-concept-cn.png`。该目录当前为未跟踪新增内容，且 `docs/design/references/README.md` 已声明这些图片是 AI-generated UI concept images，不是真实运行截图。
- 当前前端状态：真实接口基础完整，Dashboard 使用 stats/log/prompts，Workbench 使用生成、保存、确认接口，Prompt Studio 使用 prompt CRUD 和试运行生成接口，Agent Run Trace 使用真实 run/trace 接口，Knowledge Base 使用文档、chunk 和关键词检索接口。截图脚本 `scripts/capture-portfolio-screenshots.mjs` 会预热安全 demo 数据并输出真实浏览器截图到 `docs/images/`。
- 现有改动保护：审查开始前工作区已有 `frontend/src/views/AgentRunTraceView.vue` 大量未提交视觉改动，以及未跟踪的 `docs/design/` 参考图目录；本轮不回滚、不覆盖这些改动。
- 最小后续计划：先统一导航中文结构和全局设计 token/公共组件，再按 Dashboard -> Workbench -> Agent Run Trace -> Knowledge Base -> Prompt Studio 的顺序分阶段改造；每页继续使用真实接口，缺字段只做集中 fallback，并明确展示数据来源。
- 验证证据：`frontend` 目录执行 `npm run build` 成功；仍有既有 VueUse PURE 注释提示和 Element Plus/Markdown 大 chunk 警告。`git diff --check` 退出码 0，仅有 LF/CRLF 提示；敏感词扫描只命中文档占位符，没有真实 API Key。后端本轮未修改，未运行 `mvn test`。

## 2026-06-26 - Codex - 真实 OpenAI-compatible Provider 最小验证

- 做了什么：在不读取、不打印、不要求用户提供 API Key 的前提下，验证本机已运行后端 `http://127.0.0.1:8080`。`GET /api/dashboard/stats` 返回 HTTP 200；`POST /api/ai/requirement-split` 用最短 Prompt 完成一次真实 Provider 请求。
- Provider 结果：生成记录 `recordId=13`，`providerName=openai-compatible`，`modelName=gpt-5.5`，`costTimeMs=22937`，`status=READY_FOR_REVIEW`，`errorMessage=null`，`agentRunId=5`。
- Trace 闭环：`GET /api/generation-traces?generationRecordId=13` 返回 1 条 trace；`GET /api/agent-runs?generationRecordId=13` 返回 1 条 run；`GET /api/agent-runs/5/trace` 返回 5 steps、3 tool calls、1 human review；`GET /api/knowledge/references?generationRecordId=13` 返回 3 条引用。
- fallback 结论：未重启后端做无 Key 实测；通过代码和 `ProviderAndDiagnosisTest.routerFallsBackToLocalRuleWhenOpenAiConfigIsMissing` 确认缺 Key 且 `DEVFLOW_AI_FALLBACK_TO_LOCAL=true` 时会降级 `local-rule`。如后续要做无 Key 运行时验证，需要用户在同一个 PowerShell 里重新设置 `DEVFLOW_AI_*` 后再启动后端。
- 修改文件：`docs/real-provider-verification.md`、`HANDOFF.md`、`TODO.md`。
- 验证证据：`backend` 目录执行 `mvn test` 通过，20 tests；`frontend` 目录执行 `npm run build` 成功，仍有既有 VueUse PURE 注释提示和大 chunk 警告；`git diff --check` 退出码 0，仅有 CRLF 提示；API Key 扫描只命中文档占位符，没有真实 Key；`git status --short` 仅显示本轮 3 个文档修改。

## 当前待处理交接

### 更新时间

`2026-06-22（Asia/Shanghai）`

### 当前真实状态

- 当前工作分支：`resume-optimization-v1`。
- 最新已完成提交：
  - `0abd047 docs: add resume-oriented project summary`
  - `9babb4f docs: clarify InMemoryStore demo profile boundary`
  - `bce0368 feat: add ai task query endpoint`
  - `5908a7b docs: clarify resume README boundaries`
  - `d5a4486 docs: add project TODO roadmap`
  - `e38126c chore: avoid Docker compose port conflict`
- P0-1 已完成：根目录 `TODO.md` 已创建，AGENTS.md 启动流程中的 TODO 读取闭环已补齐。
- P0-2 已完成：`ai_task` 已补最小只读查询入口 `GET /api/tasks?projectId={projectId}`，不再只是表 / Entity / Mapper 空壳。
- P1-1 已完成：`InMemoryStore` 已补充 demo-only / `memory-demo` profile 边界 Javadoc；它仅用于 `memory-demo` 或无数据库轻量演示场景，不是默认 dev/test/prod 主流程持久化方案，默认主流程应使用 MyBatis-Plus + H2/MySQL。本次仅补注释，没有修改业务逻辑；未运行 `mvn test`，原因是只修改 Javadoc 注释，不改变运行行为。
- P1-2 已完成：`README.md` 已在“项目定位”之后、“技术栈”之前新增“简历版项目介绍”小节；本次仅新增 4 句话的简历投递版项目介绍，没有重写 README，没有修改 Java / Vue 业务代码。
- 后续每轮任务应先读 `AGENTS.md`、`HANDOFF.md`、`TODO.md`，每轮只做一个明确任务，校验通过后再 commit。
- README.md 已完成边界澄清：不再把 Docker Compose runtime 写成完整部署成功；不再笼统写 `API smoke test passed`；已压实 `local-rule` 是本地规则/模板生成，不是真实 LLM 推理；已压实日志分析是关键词规则引擎，不是 AI 自动推理；已压实 local-rule tokenUsage 是文本长度估算，不是真实 tokenizer。
- 项目仍适合作为大三 Java 后端实习简历项目基础，亮点应聚焦 Spring Boot 分层架构、MyBatis-Plus 持久化、Prompt 模板管理、生成记录状态流转、规则化日志诊断、Vue 3 控制台前端、后端测试和前端构建。

### Docker / Compose 当前状态

- WSL Ubuntu-22.04 中 Docker Engine 可用。
- Docker Engine：`29.1.3`。
- Docker Compose Plugin：`v5.1.4`。
- `docker compose config` 已通过。
- 后端宿主默认端口已改为 `18080`，避免和现有 `sub2api` 的 `8080` 冲突；容器内部端口仍为 `8080`。
- `docker compose up --build` 已尝试，但失败原因是 Docker Hub `registry-1.docker.io` 镜像元数据请求 `i/o timeout`。
- 已执行 `docker compose down`，不带 `-v`；不应写成 Docker Compose runtime 已完整部署成功。

### P0-2 只读复核结论

- P0-2 已解决。
- 实现符合 Controller -> Service -> Mapper 分层风格。
- 返回结构沿用 `ApiResponse<List<AiTask>>`。
- 参数校验与现有 `GlobalExceptionHandler` 风格一致：缺失 `projectId` 返回 HTTP 400，错误结构 `code=4000`。
- 测试覆盖正常查询、空结果、缺失 `projectId` 三类场景。
- 后端验证已通过：`mvn test`，`Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`。
- 未发现高风险。
- 低风险：当前排序使用 `orderByDesc(AiTask::getId)`，与部分历史查询使用 `createdAt` 倒序略有风格差异，但功能正确，可解释为 ID 自增近似创建顺序。
- 低风险：测试中 `projectId=2L` 依赖 seed 数据，当前与项目测试惯例一致。

### P1-1 InMemoryStore 边界同步结论

- P1-1 已完成：`InMemoryStore` 已补充 demo-only / `memory-demo` profile 边界 Javadoc。
- 完成提交：`9babb4f docs: clarify InMemoryStore demo profile boundary`。
- 完成边界：`InMemoryStore` 仅用于 `memory-demo` 或无数据库轻量演示场景。
- 持久化边界：它不是默认 dev/test/prod 主流程持久化方案；默认主流程应使用 MyBatis-Plus + H2/MySQL。
- 修改范围：仅修改 `InMemoryStore.java` 类头 Javadoc，没有修改字段、方法、注解或任何业务逻辑。
- 验证说明：未运行 `mvn test`，原因是本次只修改 Javadoc 注释，不改变运行行为。

### P1-2 README 简历版项目介绍同步结论

- P1-2 已完成：`README.md` 已新增“简历版项目介绍”小节。
- 完成提交：`0abd047 docs: add resume-oriented project summary`。
- 完成内容：只修改 `README.md`，在“项目定位”之后、“技术栈”之前新增 4 句话的简历投递版项目介绍。
- 能力表达：突出 Java 后端工程化能力，包括 Spring Boot 3、REST API、Controller -> Service -> Mapper 分层、MyBatis-Plus、Flyway、H2/MySQL Profile、统一响应、状态流转和后端测试；也突出 Prompt 模板、生成记录、人工确认、local-rule 演示生成链路与 OpenAI-compatible Provider 代码层适配。
- 边界表达：继续说明 `local-rule` 不是真实 LLM 推理，日志分析不是 AI 自动推理，tokenUsage 是估算，Docker runtime 未完整成功，`ai_task` 不是完整任务系统，`InMemoryStore` 不是默认主流程持久化。
- 修改范围：仅新增短小节，没有重写 README；没有修改 Java / Vue 业务代码。

### 下一轮建议任务

> 一次只处理一个任务。

**P1-3：只读审查 README.md、HANDOFF.md、TODO.md 和最近提交历史，判断 P1-1、P1-2 是否已闭环，并决定是否进入最终验收报告阶段。**

- 下一轮先只读审查，不直接修改文件。
- 审查重点：文档是否一致、是否还有夸大表述、是否适合进入最终验收。
- 不要继续扩功能。
- 不要运行 Docker。
- 不要把 Docker runtime 写成完整部署成功。

## Codex 修复结果

- `d5a4486 docs: add project TODO roadmap`：已创建根目录 `TODO.md`，P0-1 完成。
- `5908a7b docs: clarify resume README boundaries`：已修正 README 简历表述和验收状态边界。
- `bce0368 feat: add ai task query endpoint`：已新增 `GET /api/tasks?projectId={projectId}`、`AiTaskController`、`AiTaskService`、`AiTaskServiceImpl`，复用 `AiTaskMapper`，并补充 `ControllerAndMapperIntegrationTest` 覆盖；`mvn test` 通过，18 tests。
- `9babb4f docs: clarify InMemoryStore demo profile boundary`：已在 `InMemoryStore.java` 类头补充 demo-only / `memory-demo` profile 边界 Javadoc；仅用于无数据库或轻量演示场景，不是 dev/test/prod 默认持久化方案，默认主流程应使用 MyBatis-Plus + H2/MySQL；本次仅补注释，没有修改业务逻辑，未运行 `mvn test`。
- `0abd047 docs: add resume-oriented project summary`：已在 `README.md` 的“项目定位”之后、“技术栈”之前新增“简历版项目介绍”小节；仅新增 4 句话，没有重写 README，没有修改业务代码；继续压住 `local-rule`、日志规则、tokenUsage、Docker runtime、`ai_task`、`InMemoryStore` 等边界。
- 剩余 P0：当前无已知未闭环 P0；建议下一轮做 P1-3 只读审查 README.md、HANDOFF.md、TODO.md 和最近提交历史，判断是否进入最终验收报告阶段。
- 本次 HANDOFF 同步为纯文档状态更新，不代表业务代码变更。

## 历史记录

### 2026-06-26 — Codex — P1-5 Agentic Coding Workflow 控制台最小闭环

- 做了什么：将 DevFlow Copilot 从本地规则演示扩展为最小可运行的 Agentic Coding Workflow 控制台。新增 Generation Trace、Agent Run / Agent Step / Tool Call / Human Review、轻量 Knowledge Base / RAG 引用，并把生成链路接入这些记录。前端新增 Agent Run Trace 页面、Knowledge Base 页面，Prompt 模板页升级为 Prompt Studio 试运行，Dashboard 增加运行数、成功率、人工确认数和平均耗时指标。
- 修改文件：后端新增 Flyway V3/V4、实体、Mapper、Service、Controller 和 `AgenticWorkflowIntegrationTest`；前端修改 `domain.ts`、`devflow.ts`、路由、布局、Dashboard、Workbench、Prompt Studio，并新增 `AgentRunTraceView.vue`、`KnowledgeBaseView.vue`；文档更新 `README.md`、`docs/architecture.md`、`docs/api.md`、`docs/mvp-scope.md`、`DESIGN.md`、`TODO.md`，新增 `docs/resume-evidence.md`、`docs/deployment-plan.md`。
- 验证证据：已执行 `cd backend && mvn test`，结果 `Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`；已执行 `cd frontend && npm run build`，结果成功，仍有既有 `@vueuse/core` PURE 注释提示和大 chunk 警告。
- 真实边界：没有提交真实 API Key；OpenAI-compatible 仍只通过环境变量启用；默认仍可无 Key 用 `local-rule` 演示；Knowledge Base 当前是关键词/简单相似度检索，不是向量数据库；Agent Run Trace 是可解释的单次 workflow 记录，不是完整多 Agent Runtime；没有自动修改代码、自动提交 Git 或生产部署。
- 遗留问题：新增页面尚未生成静态截图；Docker runtime 仍不能写成已完整部署成功；真实 LLM 端到端调用仍未配置 Key 验证。
- 下一步：建议单独做“新增页面截图 / 演示 GIF”任务，补齐 Agent Run Trace、Prompt Studio、Knowledge Base 的作品集证据。

---

### YYYY-MM-DD HH:mm — Agent — 任务名称

- 做了什么：
- 修改文件：
- 验证证据：
- 遗留问题：
- 下一步：

---

### 2026-06-22 — Codex — P1-2 README 简历版项目介绍完成同步

- 做了什么：同步提交 `0abd047 docs: add resume-oriented project summary`，标记 P1-2 已完成；记录 README 已新增“简历版项目介绍”小节，并继续保持 local-rule、日志规则、tokenUsage、Docker runtime、ai_task、InMemoryStore 等边界。
- 修改文件：`HANDOFF.md`、`TODO.md`
- 验证证据：本轮为纯文档同步；已执行 `git diff -- HANDOFF.md TODO.md`、`git diff --check -- HANDOFF.md TODO.md`（退出码 0，仅 LF/CRLF 行尾提示）、`git status --short`（仅 `HANDOFF.md`、`TODO.md` 修改）。
- 遗留问题：无新增阻塞；下一轮不扩功能、不运行 Docker，先只读审查文档与提交历史是否一致。
- 下一步：P1-3 只读审查 README.md、HANDOFF.md、TODO.md 和最近提交历史，判断 P1-1、P1-2 是否已闭环，并决定是否进入最终验收报告阶段。

---

### 2026-06-21 — Codex — P1-1 InMemoryStore demo-only 边界说明完成同步

- 做了什么：同步提交 `9babb4f docs: clarify InMemoryStore demo profile boundary`，标记 P1-1 已完成，并记录 `InMemoryStore` 仅用于 `memory-demo` / 无数据库轻量演示场景，不是默认 dev/test/prod 主流程持久化方案。
- 修改文件：`HANDOFF.md`、`TODO.md`
- 验证证据：本轮为纯文档同步；已执行 `git diff -- HANDOFF.md TODO.md`、`git diff --check -- HANDOFF.md TODO.md`（退出码 0，仅 LF/CRLF 行尾提示）、`git status --short`（仅 `HANDOFF.md`、`TODO.md` 修改）。
- 遗留问题：无新增阻塞；`ai_task` 仍只是最小只读查询入口；`local-rule` 仍是本地规则/模板生成；日志分析仍是关键词规则引擎；Docker Compose runtime 仍未完整部署成功。
- 下一步：P1-2 只读审查 README.md 中“简历可写亮点”和“面试回答口径”，先判断是否需要补一段更适合大三 Java 后端实习简历的项目介绍，不直接修改 README。

---

### 2026-06-21 — Codex — P0-2 ai_task 最小查询入口完成复核

- 做了什么：记录提交 `bce0368 feat: add ai task query endpoint`，同步 P0-2 已完成，并补充只读复核结论。
- 修改文件：`HANDOFF.md`、`TODO.md`
- 验证证据：代码提交中已运行 `mvn test`，结果为 `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`。
- 遗留问题：`ai_task` 仅为最小只读查询入口，不是完整任务系统；无新增/更新/删除、调度、异步执行或 LLM 接入。
- 下一步：建议进行 P0 收口只读总审查，确认 README/HANDOFF/TODO/测试结果/提交历史一致后再进入 P1。

---

### 2026-06-21 — Codex — HANDOFF 最新状态同步

- 做了什么：同步最新提交 `d5a4486`、`5908a7b` 和 Docker Compose 当前真实状态；标记 P0-1 已完成；记录 README 边界澄清已完成；明确下一轮只处理 P0-2 ai_task 空壳问题。
- 修改文件：`HANDOFF.md`
- 验证证据：本轮仅文档同步；修改后应执行 `git diff -- HANDOFF.md`、`git diff --check -- HANDOFF.md`、`git status --short`。
- 遗留问题：`ai_task` 表 / 实体 / Mapper 仍无 Service / Controller 或明确扩展预留说明；`InMemoryStore` 仍建议后续补 demo-only Javadoc。
- 下一步：只处理 P0-2 ai_task 空壳问题，二选一实现最小接口或标注扩展预留。

---

### 2026-06-21 — Claude — 简历版只读审查

#### 审查对象

- 功能名称：项目整体简历可用性评估
- 审查文件范围：README.md、AGENTS.md、CLAUDE.md、HANDOFF.md（空模板）、docs/（全部 md 文件）、backend/src/main/java/（全部 Java 源码）、backend/src/test/（全部测试）、frontend/src/（App.vue、WorkbenchView.vue）、db/migration/V1__create_core_schema.sql、docs/images/（截图目录）
- 审查者：Claude（只读，未执行任何构建或测试命令）

#### 当前结论

项目整体完成度在同类大三实习项目中属于上游水平。后端分层清晰、状态机设计正确、15 个集成测试有据可查、截图文件实际存在、Provider 抽象和 Flyway 迁移均有真实代码落地。主要风险集中在三件事：ai_task 表空壳、InMemoryStore 遗留代码位置容易误导、local-rule 生成内容是模板 boilerplate 而非真实 LLM 输出——后两件需要在文档中说清楚，第一件需要补最小实现或说明。

关于 Git 仓库状态：旧验收记录（final-acceptance-report.md）曾显示 `fatal: not a git repository`，但当前项目已在 `resume-optimization-v1` 分支并已提交协作规则，此问题当前状态标记为**已解决**，不再列为 P0。后续建议保持真实小步提交，不伪造历史，不为凑 commit 数拆旧代码假提交。

#### 已确认可写进简历的真实能力

以下技术点均有对应源码，面试时可直接展示：

- Spring Boot 3 + Java 17 分层架构（controller / service / service.impl / provider / mapper / entity / dto）
- MyBatis-Plus 持久化 + Flyway 数据库迁移（V1 建表5张，FK 约束完整）
- H2（dev/test）/ MySQL（prod）Profile 双数据库支持
- 生成状态机 `GENERATING → READY_FOR_REVIEW → SAVED → CONFIRMED`，`canTransitionTo` / `allowedTargets` 逻辑正确，非法流转返回 HTTP 409
- Prompt 模板变量渲染（正则替换 `{{variable}}`）、缺填报 TemplateRenderException、模板版本记录进 generation_record
- OpenAI-compatible Provider（真实 RestClient HTTP 调用 `/v1/chat/completions`）+ local-rule 降级
- DTO 校验（`@Valid`）+ GlobalExceptionHandler 统一 ApiResponse 返回
- 15 个 JUnit 5 / MockMvc / `@SpringBootTest` 集成测试（`@Transactional` 回滚隔离）
- Vue 3 + TypeScript + Element Plus 前端，WorkbenchView 三栏布局，状态机按钮联动
- Docker Compose（backend / frontend / mysql 三服务）配置存在，`docker compose config` 已通过；runtime `up --build` 因 Docker Hub 超时未完整成功；GitHub Actions CI 配置文件存在

#### 不能夸大的能力

以下内容不得在简历或面试中声称为已实现：

- **不能写**"接入了真实大模型" —— 默认运行模式是 local-rule，生成内容是模板 boilerplate；只有配置了真实 API Key 并端到端验证后才可写
- **不能写**"AI 智能日志分析" —— 日志诊断是关键词规则引擎（8种异常类型硬编码），不是 LLM 推理
- **不能写**"毫秒级 latency 追踪" —— local-rule 的 costTimeMs 实测约为 0–1ms，不代表 LLM 网络延迟
- **不能写**"SSE 流式输出" —— 当前是同步请求，无 SSE
- **不能写**"Docker Compose 已验证部署" 或 "完整容器化部署成功" —— WSL Docker Engine 和 Compose Plugin 当前可用，`docker compose config` 已通过，`docker compose up --build` 已尝试但失败于 Docker Hub `registry-1.docker.io` 镜像元数据请求 `i/o timeout`，runtime smoke test 尚未完整完成

---

#### 当前 P0 问题

**P0-1：TODO.md 不存在（已完成）**

- 状态：已完成，提交 `d5a4486 docs: add project TODO roadmap`。
- 当前结果：根目录 `TODO.md` 已创建，记录已完成能力、不能夸大的能力、P0/P1/P2 待办和下一轮建议。
- AGENTS.md 启动流程闭环：后续每轮任务应先读 `AGENTS.md`、`HANDOFF.md`、`TODO.md`。
- 当前不再把 TODO 缺失列为 P0；剩余 P0 为 `ai_task` 空壳问题。

**P0-2：ai_task 表和 Mapper 无对应 Service / Controller（已完成）**

- 状态：已完成，提交 `bce0368 feat: add ai task query endpoint`。
- 完成内容：新增 `GET /api/tasks?projectId={projectId}`，新增 `AiTaskController`、`AiTaskService`、`AiTaskServiceImpl`，复用 `AiTaskMapper`。
- 行为边界：`projectId` 必填；缺失返回 HTTP 400 和统一错误结构 `code=4000`；返回 `ApiResponse<List<AiTask>>`；按 ID 倒序返回指定项目任务；不存在项目或无任务时返回空数组。
- 未做内容：没有新增、更新、删除接口；没有任务调度；没有异步执行；没有接入真实 LLM；没有改生成、日志分析、模板管理、历史记录主流程；没有改 `InMemoryStore`。
- 验证：`ControllerAndMapperIntegrationTest` 已覆盖正常查询、空结果、缺失 `projectId`；`mvn test` 通过，`Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`。

---

#### 当前 P1 问题

**P1-1：InMemoryStore 位置容易引起误解（已完成）**

- 状态：已完成，提交 `9babb4f docs: clarify InMemoryStore demo profile boundary`。
- 文件/位置：`backend/src/main/java/com/devflow/copilot/service/impl/InMemoryStore.java`。
- 完成内容：仅修改类头 Javadoc，明确 `InMemoryStore` 只用于 `memory-demo` profile 或无数据库轻量演示场景。
- 持久化边界：`InMemoryStore` 不是默认 dev/test/prod 主流程持久化方案；默认主流程应使用 MyBatis-Plus + H2/MySQL。
- 验证说明：未运行 `mvn test`，原因是本次只修改 Javadoc 注释，不改变运行行为。
- 验收：阅读代码不再产生“内存存储是默认生产持久化”的歧义。

**P1-2：README 简历版项目介绍（已完成）**

- 状态：已完成，提交 `0abd047 docs: add resume-oriented project summary`。
- 完成内容：`README.md` 已在“项目定位”之后、“技术栈”之前新增“简历版项目介绍”小节。
- 完成边界：仅新增 4 句话的短小节，没有重写 README，没有修改 Java / Vue 业务代码。
- 表达重点：突出 Spring Boot 3、REST API、分层设计、MyBatis-Plus、Flyway、H2/MySQL、统一响应、状态流转、后端测试，以及 Prompt 模板、生成记录、人工确认、local-rule 演示生成链路和 OpenAI-compatible Provider 代码层适配。
- 边界保持：没有把项目夸大为生产级大模型平台；继续说明 `local-rule` 不是真实 LLM 推理，日志分析不是 AI 自动推理，tokenUsage 是估算，Docker runtime 未完整成功，`ai_task` 不是完整任务系统，`InMemoryStore` 不是默认主流程持久化。

**P1-3：只读审查 README.md、HANDOFF.md、TODO.md 和最近提交历史**

- 状态：待处理。
- 目标：判断 P1-1、P1-2 是否已闭环，并决定是否进入最终验收报告阶段。
- 审查重点：文档是否一致、是否还有夸大表述、是否适合进入最终验收。
- 不可扩大：不要继续扩功能，不要运行 Docker，不要把 Docker runtime 写成完整部署成功。
- 验收：输出只读审查结论；如需进入最终验收报告阶段，应作为后续单独任务处理。

**已完成：README token 估算说明（原 P1-2）**

- 状态：已完成，提交 `5908a7b docs: clarify resume README boundaries`。
- 当前结果：README 已说明 local-rule 模式下 tokenUsage 是基于文本长度的估算值，不是真实 tokenizer；OpenAI-compatible 模式下只有 provider 返回 `usage` 字段时才可记录真实 usage。

**已完成：local-rule 生成内容性质说明（原 P1-3）**

- 状态：已完成，提交 `5908a7b docs: clarify resume README boundaries`。
- 当前结果：README 已明确 `local-rule` 是本地规则/模板生成，不是真实 LLM 推理；OpenAI-compatible Provider 是代码层适配，当前仓库不提交真实 API Key，也不把真实模型端到端调用写成已稳定验收。
- 同步结果：README 也已压实日志分析是关键词规则引擎，不是 AI 自动推理；已修正 Docker Compose runtime 状态和 `API smoke test passed` 的笼统表述。

**P1-4：HANDOFF.md 原为空白模板，无实际审查记录**

- 文件/位置：HANDOFF.md
- 证据：原文件仅包含占位符 `YYYY-MM-DD HH:mm`，无任何真实记录
- 影响：AGENTS.md 要求启动前读取 HANDOFF.md，空模板无法传递项目状态
- 建议：追加本次审查记录（即本条目）
- 验收：本条目追加完成即视为解决

---

#### 当前 P2 问题

**P2-1：OpenAI-compatible Provider 无真实端到端测试**

- 当前状态：只测试了缺 API Key 时的降级逻辑，未配置真实 Key 做端到端调用
- 建议：配置一个低成本兼容服务（如 DeepSeek、Groq），跑通真实调用，更新 README 说明已验证

**P2-2：前端无单元测试**

- 当前状态：只有 `npm run build` 通过，无 Vitest / Vue Test Utils 测试
- 建议：为 StatusTag 组件或 WorkbenchView 核心计算属性添加 1–2 个 Vitest 测试

**P2-3：Docker Compose runtime 未完整成功**

- 当前状态：WSL Ubuntu-22.04 Docker Engine `29.1.3` 可用，Docker Compose Plugin `v5.1.4` 可用；`docker compose config` 已通过；后端宿主默认端口已改为 `18080` 避让 `sub2api` 的 `8080`。
- 已尝试：`docker compose up --build` 已执行，但失败原因是 Docker Hub `registry-1.docker.io` 镜像元数据请求 `i/o timeout`；后端/前端容器未创建，runtime smoke test 未完整完成。
- 已清理：已执行 `docker compose down`，不带 `-v`。
- 建议：待 Docker Hub 网络恢复或基础镜像可用后，重新执行 runtime smoke test；未成功前不得写成 Docker Compose 已完整部署成功。

**P2-4：缺少动态演示材料**

- 当前状态：有静态截图10张，无 GIF 或视频演示
- 建议：录制 30 秒 GIF 展示 Workbench 生成→保存→确认流程，放入 README

---

#### 交给 Codex 的第一批任务

> 规则：每次只处理一个任务，完成后回写"Codex 修复结果"再开始下一个。

**任务1：创建或完善 TODO.md（已完成）**

- 目标：在项目根目录创建 `TODO.md`，记录已完成功能和当前待处理项
- 完成提交：`d5a4486 docs: add project TODO roadmap`
- 涉及文件：`TODO.md`（新建）
- 不能破坏：其他任何文件不做改动
- 不在本轮处理：其他 P0/P1 问题
- 验收方式：
  - [x] `TODO.md` 文件存在于项目根目录
  - [x] 文件内容列出"已完成"与"待处理"两个分区，内容与项目实际现状一致
  - [x] AGENTS.md 描述的"启动前读取 TODO.md"流程可正常执行

**任务2：处理 ai_task 空壳问题（已完成）**

- 目标：选择方案A或B —— A: 添加最简 `GET /api/tasks?projectId=` 返回空列表；B: 在 AiTaskMapper 头部 Javadoc 注释中说明"为后续扩展预留，当前无业务实现"
- 完成提交：`bce0368 feat: add ai task query endpoint`
- 涉及文件：方案A 涉及 `AiTaskMapper.java`、新建 `AiTaskService.java`、`AiTaskController.java`；方案B 仅涉及 `AiTaskMapper.java`
- 不能破坏：现有15个测试须全部通过，`mvn test` 结果不能回归
- 不在本轮处理：ai_task 的完整 CRUD
- 验收方式：
  - [x] 选择了明确的方案（A）并实施
  - [x] `GET /api/tasks?projectId=1` 返回 HTTP 200 和列表；未知项目返回 HTTP 200 空列表
  - [x] `mvn test` 通过，18 tests
  - [x] 面试时对"这个 Mapper 是干嘛的"有完整答案：它支撑最小只读任务查询入口，不是完整任务系统

**任务3：补充 README 中 token 估算和 local-rule 说明（已完成）**

- 目标：在 README.md 相关段落补充两句话：(1) local-rule token 是字符数估算；(2) local-rule 生成的是结构化模板内容，非 LLM 推理
- 完成提交：`5908a7b docs: clarify resume README boundaries`
- 涉及文件：`README.md`
- 不能破坏：README 整体结构和其他内容保持不变
- 不在本轮处理：其他文档修改
- 验收方式：
  - [x] README 中出现对 token 估算方式的准确说明
  - [x] README 中出现对 local-rule 内容性质的准确说明
  - [x] 不引入任何夸大或虚构的功能描述

**任务4：整理 InMemoryStore 的 demo-only 说明（已完成）**

- 目标：在 `InMemoryStore.java` 类头添加 Javadoc 注释，说明该类仅在 `memory-demo` profile 下激活，主存储已切换为 MyBatis-Plus + H2/MySQL。
- 完成提交：`9babb4f docs: clarify InMemoryStore demo profile boundary`。
- 涉及文件：`backend/src/main/java/com/devflow/copilot/service/impl/InMemoryStore.java`
- 完成边界：仅修改类头 Javadoc；`@Profile("memory-demo")` 注解未删除，类功能和业务逻辑未改变。
- 不在本轮处理：包结构调整
- 验收方式：
  - [x] InMemoryStore.java 类头有清晰的 Javadoc 注释
  - [x] 注释内容说明“演示/轻量 demo 用途，memory-demo profile 专用，主流程不加载”
  - [x] 说明默认主流程应使用 MyBatis-Plus + H2/MySQL
  - [x] 本次只改注释，不改变运行行为；未运行 `mvn test`

**任务5：补充 README 简历版项目介绍（已完成）**

- 目标：在 `README.md` 的“项目定位”之后、“技术栈”之前新增“简历版项目介绍”短小节。
- 完成提交：`0abd047 docs: add resume-oriented project summary`。
- 涉及文件：`README.md`。
- 完成边界：仅新增 4 句话的短小节，没有重写 README，没有修改 Java / Vue 业务代码，没有修改 `HANDOFF.md`、`TODO.md` 或 `InMemoryStore.java`。
- 验收方式：
  - [x] 小节标题为“简历版项目介绍”
  - [x] 覆盖 Java 后端工程化能力和 AI Coding 工具链能力
  - [x] 继续压住 local-rule、日志规则、tokenUsage、Docker runtime、ai_task、InMemoryStore 等边界

---

#### Codex 修复结果（已同步）

- P0-1 / 任务1：已完成，提交 `d5a4486 docs: add project TODO roadmap`。
- P0-2 / 任务2：已完成，提交 `bce0368 feat: add ai task query endpoint`。
- README 边界澄清 / 任务3：已完成，提交 `5908a7b docs: clarify resume README boundaries`。
- P1-1 / 任务4：已完成，提交 `9babb4f docs: clarify InMemoryStore demo profile boundary`；仅补充 `InMemoryStore.java` 类头 Javadoc，明确其仅用于 `memory-demo` / 无数据库轻量演示场景，不是默认 dev/test/prod 主流程持久化方案。
- P1-2 / 任务5：已完成，提交 `0abd047 docs: add resume-oriented project summary`；仅在 `README.md` 新增“简历版项目介绍”短小节，没有重写 README，并继续保持所有边界说明。
- Docker Compose 端口避让：已完成配置修改，提交 `e38126c chore: avoid Docker compose port conflict`；runtime `up --build` 未完整成功，原因是 Docker Hub 镜像元数据请求 `i/o timeout`。
- 当前剩余优先任务：P1-3 只读审查 README.md、HANDOFF.md、TODO.md 和最近提交历史，判断 P1-1、P1-2 是否已闭环，并决定是否进入最终验收报告阶段。
- 本条同步仅更新交接信息，不代表本轮修改业务代码。

---

## 2026-06-26 15:30 Codex 作品集证据收口

- 本轮目标：只做作品集证据收口、真实 Provider 验证准备、README 展示增强和简历证据文档更新，不扩展后端主能力。
- 已加载 Skill：`frontend-design`、`vercel-react-best-practices`、`emil-design-eng`。其中 Vercel Skill 的 React/Next.js 规则仅迁移使用通用部分，例如并行请求、bundle 拆分、按需加载、减少瀑布请求和动画性能。
- 截图结果：新增 `scripts/capture-portfolio-screenshots.mjs`，通过真实后端 API 预热一条安全 demo Agent Workflow，再用 Playwright 从真实浏览器页面生成 6 张截图到 `docs/images/`。
- 截图文件：`dashboard-agentic.png`、`workbench-running.png`、`agent-run-trace.png`、`knowledge-base-rag.png`、`prompt-studio.png`、`human-review-trace-detail.png`。
- README：顶部定位改为面向 AI Coding / VibeCoding 的 Agentic Coding Workflow 控制台，并展示 Dashboard、Workbench、Agent Run Trace、Knowledge Base 四张核心截图。
- Provider 文档：新增 `docs/real-provider-verification.md`，记录环境变量、启动命令、最小请求、成功字段、fallback 行为和 API Key 泄露检查。本轮 shell 中未检测到真实 Key，因此未执行外部模型调用。
- 简历证据：重写 `docs/resume-evidence.md`，补充 5 条后端架构感 bullet、5 条不能夸大的边界、10 个面试追问回答。
- 编码核对：使用 Node 按 UTF-8 读取并检查文件码位，确认现有中文文件本身是 UTF-8；PowerShell 普通 `Get-Content` 的乱码属于终端显示层问题。
- 待验证：本轮收尾需运行 `mvn test`、`npm run build`、`git diff --check`、截图存在性检查和敏感文件/密钥检查。
