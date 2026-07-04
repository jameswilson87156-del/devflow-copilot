# DevFlow Copilot 作品集指标快照

采集时间：2026-07-04T04:05:29.149Z
采集命令：`node scripts/collect-portfolio-metrics.js --run-checks`
当前分支：`feat/portfolio-showcase-v1`

## 功能规模

- 前端页面文件：7 个
- 前端真实 component route：7 个；redirect route：1 个；disabled nav item：4 个
- 后端 Controller：10 个
- 后端 endpoint mapping：30 个
- Flyway migration：4 个
- SQL seed insert statement：6 条
- Prompt 模板 seed：6 条
- Generation Record seed：6 条
- Knowledge Document seed：2 条；Knowledge Chunk seed：4 条
- docs/images 截图文件：11 张
- README 顶部 / 正文图片引用：6 张

## 工程质量

- 后端测试文件：5 个
- 后端测试源码中的 `@Test`：20 个
- GitHub Actions workflow：1 个
- 前端 build 脚本：`vue-tsc --noEmit && vite build`
- `npm run build`：通过，耗时 39921ms
- `mvn test`：通过，耗时 43765ms

## 性能体验

- Node.js 版本：v24.16.0
- 前端 bundle 统计：已采集
- JS assets：18 个，2.2 MB
- CSS assets：14 个，461 KB
- 关键接口响应：当前未采集 / 默认接口不可用（HTTP 404）
- 关键接口 URL：`http://127.0.0.1:8080/api/dashboard/stats`
- 关键接口单次耗时：当前未采集

## AI 工作流证据

- Prompt -> Provider -> Result -> Generation Trace -> Agent Run Trace -> Tool Call -> Human Review 的闭环有实体、表、接口和测试覆盖。
- 当前默认 Provider 是 `local-rule`，不代表真实 LLM 推理。
- OpenAI-compatible Provider 为代码层适配，真实调用必须通过环境变量配置 Key。
- Knowledge Base 当前是关键词 / 简单相似度检索，不是向量数据库。

## 敏感信息扫描

- 基础模式扫描未发现真实 API Key / token / secret 形态。注意：这不是完整安全审计。

## 可写入简历的数据

- 7 个 Vue 页面文件、7 个真实前端页面路由。
- 30 个后端 endpoint mapping，覆盖 AI 生成、Trace、Knowledge Base、Prompt、History、Review 等模块。
- 20 个后端自动化测试源码；若本快照显示 `mvn test` 通过，可写最近一次本地测试通过。
- 11 张本地截图文件，其中 README 引用 6 张真实页面截图。

## 暂时不能写的数据

- 不能写真实线上用户、商业收益、生产请求量或 SLA。
- 不能写未采集的 Lighthouse 分数或首屏性能分数。
- 不能把 local-rule 写成真实 LLM 推理。
- 不能把 demo seed / mock / UI-only 字段写成生产数据。
- 不能把 Docker Compose runtime 写成已完整部署成功，除非重新完成 `docker compose up --build` 和 smoke test。
