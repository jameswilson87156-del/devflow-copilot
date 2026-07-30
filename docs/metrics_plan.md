# DevFlow Copilot 作品集数据指标计划

计划日期：2026-07-04

原则：所有指标必须可重复采集、可回到命令或文件证据；采集不到就写“当前未采集 / 待补充”。不得为简历或 README 编造数据。

默认采集命令：

```bash
node scripts/collect-portfolio-metrics.js --run-checks
```

默认输出：

```text
docs/metrics/metrics_snapshot.md
```

## 1. 功能规模类

| 指标名称 | 如何采集 | 命令 | 结果保存 | 是否可写简历 | 推荐表述 | 不能写的情况 |
| --- | --- | --- | --- | --- | --- | --- |
| 核心页面数量 | 扫描 `frontend/src/views/*.vue` | `node scripts/collect-portfolio-metrics.js` | `docs/metrics/metrics_snapshot.md` | 是 | 实现 7 个 Vue 页面，覆盖 Dashboard、Workbench、Trace、Knowledge、Prompt 等视图。 | 页面只是空壳或不可运行时不能写。 |
| 前端路由数量 | 扫描 `frontend/src/router/index.ts` 中真实 component route | 同上 | 同上 | 是 | 配置 7 个真实前端路由和 1 个文档重定向。 | disabled nav item 不能算独立页面。 |
| 后端接口数量 | 扫描 Controller 中 `@GetMapping` / `@PostMapping` 等方法注解 | 同上 | 同上 | 是 | 后端约 30 个 REST endpoint，覆盖生成、Trace、知识库、模板和审核。 | 未扫描或接口无法编译时不能写。 |
| Prompt 模板 seed 数量 | 解析 Flyway seed 中 `prompt_template` 插入行数 | 同上 | 同上 | 是 | 内置 6 个 Prompt 模板 seed，覆盖需求拆解、代码计划、README、commit、修复和日志分析。 | 如果 parser 失败，只能写“待人工核对”。 |
| demo 生成记录 seed 数量 | 解析 `generation_record` seed 插入行数 | 同上 | 同上 | 谨慎 | 使用本地 demo generation records 支撑历史和截图演示。 | 不能写成真实线上运行记录。 |
| 截图数量 | 扫描 `docs/images/*.png` | 同上 | 同上 | 是 | 准备真实浏览器截图并通过脚本复现。 | AI concept image 不能算真实截图。 |
| README 展示图数量 | 扫描 README 中 `docs/images` 图片引用 | 同上 | 同上 | 是 | README 顶部展示 6 张本地真实页面截图。 | 图片不存在或不是本地运行截图时不能写。 |

## 2. 工程质量类

| 指标名称 | 如何采集 | 命令 | 结果保存 | 是否可写简历 | 推荐表述 | 不能写的情况 |
| --- | --- | --- | --- | --- | --- | --- |
| 自动化测试数量 | 扫描 `backend/src/test/java` 中 `@Test` | `node scripts/collect-portfolio-metrics.js` | `docs/metrics/metrics_snapshot.md` | 是 | 不在计划文档固化数量；以最新快照为准。 | 测试未运行时只能写“测试源码数量”，不能写“测试通过”。 |
| 后端测试是否通过 | 执行 `cd backend && mvn test` | `node scripts/collect-portfolio-metrics.js --run-checks` | 同上 | 是 | 最近一次 `mvn test` 通过。 | 命令失败或未执行时不能写通过。 |
| 前端构建是否通过 | 执行 `cd frontend && npm run build` | 同上 | 同上 | 是 | 最近一次 `npm run build` 通过。 | 命令失败或未执行时不能写通过。 |
| Flyway migration 数量 | 扫描 `backend/src/main/resources/db/migration/V*.sql` | `node scripts/collect-portfolio-metrics.js` | 同上 | 是 | 使用 4 个 Flyway migration 管理 schema 和 demo seed。 | 手动 SQL 未纳入 migration 时不能写。 |
| CI 配置 | 扫描 `.github/workflows/*.yml` | 同上 | 同上 | 是 | 配置 GitHub Actions CI 运行后端 verify 和前端 build。 | CI 未实际运行时不能写“云端 CI 已通过”。 |
| 敏感信息检查 | 扫描常见 API key/token 模式 | 同上 | 同上 | 是 | 指标脚本执行基础密钥模式扫描，未发现真实 Key 模式。 | 仅模式扫描，不能当作完整安全审计。 |

## 3. 性能体验类

| 指标名称 | 如何采集 | 命令 | 结果保存 | 是否可写简历 | 推荐表述 | 不能写的情况 |
| --- | --- | --- | --- | --- | --- | --- |
| 前端 bundle 大小 | `npm run build` 后统计 `frontend/dist/assets` 文件大小 | `node scripts/collect-portfolio-metrics.js --run-checks` | `docs/metrics/metrics_snapshot.md` | 可以写工程记录 | 记录本地 build 后 JS/CSS asset 体积，用于后续 bundle split 优化。 | 未执行 build 或 dist 不存在时不能写具体体积。 |
| 关键接口响应耗时 | 访问 `DEVFLOW_METRICS_API_URL` 或默认本地 `/api/dashboard/stats` | `node scripts/collect-portfolio-metrics.js` | 同上 | 谨慎 | 本机 demo 环境下 Dashboard stats 响应耗时为 x ms。 | 服务未启动、网络失败或只跑一次时不能写成稳定性能指标。 |
| 页面加载性能 | 后续用 Playwright 或 Lighthouse 采集 | 待补充 | `docs/metrics/` | 暂不写 | 当前未采集，待补充。 | 没有 Lighthouse/Playwright trace 时不能写分数。 |
| 截图脚本耗时 | 后续运行 `npm run screenshots:portfolio` 并记录 | 待补充 | `docs/metrics/` | 可作为演示材料 | 截图脚本可生成 x 张图。 | 前后端未启动或截图失败时不能写。 |

## 4. AI 工作流效果类

| 指标名称 | 如何采集 | 命令 | 结果保存 | 是否可写简历 | 推荐表述 | 不能写的情况 |
| --- | --- | --- | --- | --- | --- | --- |
| Prompt -> Provider -> Trace -> Review 闭环 | 检查实体、表、接口和测试 | `node scripts/collect-portfolio-metrics.js` + `mvn test` | `docs/metrics/metrics_snapshot.md` | 是 | 实现 Prompt 渲染、Provider 调用、Generation Trace、Agent Run Trace、Human Review 的可解释闭环。 | 测试未通过时不能写“已验证通过”。 |
| Provider fallback 可演示 | 依赖测试与配置 | `cd backend && mvn test` | 同上 | 是 | 支持 OpenAI-compatible Provider 缺 Key/失败时降级到 local-rule 并记录原因。 | 不能写成真实模型稳定调用。 |
| Trace 是否可回放 | 检查 `GET /api/agent-runs/{id}/trace` 和前端页面 | 指标脚本静态扫描 + 本地服务手工验证 | 同上 | 是 | Agent Run Trace 可展示 steps、tool calls、human reviews。 | 服务未启动时不能写“本轮已手工验收页面”。 |
| Tool Call 类型数量 | 扫描 seed / service 中 tool name | `node scripts/collect-portfolio-metrics.js` | 同上 | 可以写 | 当前 demo 中记录 prompt-template-render、keyword-knowledge-search、generation-provider 等工具调用。 | 不应写成真实外部工具执行平台。 |
| Human Review 状态数量 | 扫描枚举/代码/seed 中 review status | 同上 | 同上 | 可以写 | 记录 PENDING、SAVED、CONFIRMED、REJECTED 等审核状态。 | 前端没有独立审核页时不要写成完整审核系统。 |

## 简历表达红线

- 不写真实用户数、商业收益、线上请求量、生产 SLA。
- 不写未实测的性能提升百分比。
- 不写“接入真实大模型”，除非有不泄露 Key 的端到端验证记录。
- 不写“向量数据库 / 生产级 RAG”，除非实现并验证。
- 不写“自动改代码 / 自动提交 Git”，当前项目明确不做。
