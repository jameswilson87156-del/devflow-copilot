# DevFlow Copilot 开发环境检查报告

## 1. 检查概况

- 检查时间：`2026-06-21 21:52:44 +08:00`（Asia/Shanghai）
- 项目路径：`D:\workhome\ai-coding-workbench`
- 当前分支：`resume-optimization-v1`
- 审计开始时 Git 状态：`nothing to commit, working tree clean`
- 最近提交：
  - `dbd986b docs: add resume review handoff`
  - `68dd3b8 add claude codex collaboration rules`
  - `5a62fb8 feat: polish AI coding command center presentation`
  - `b08a955 feat: complete DevFlow Copilot interview-ready MVP`
- Maven 工程入口：根目录不存在 `pom.xml`，实际入口为 `backend/pom.xml`，因此后端命令必须在 `backend` 目录运行。

旧报告中“当前目录不是 Git 仓库”的记录已经过时。本次以真实 Git 现场为准：项目是 Git 仓库、目标分支正确、审计开始时工作区干净。

## 2. 环境需求与判断依据

| 工具或环境 | 项目要求 | 判断依据 |
| --- | --- | --- |
| Git | 必需，用于版本管理与本轮报告提交；项目未声明最低版本 | 实际存在 `.git`；`AGENTS.md` 要求检查状态和 diff；`.github/workflows/ci.yml` 使用 `actions/checkout@v4` |
| Java | Java 17 | `README.md`“技术栈/快速启动”；`backend/pom.xml` 的 `<java.version>17</java.version>`；CI 的 `java-version: "17"`；`backend/Dockerfile` 使用 Temurin 17 |
| Maven | 必需；项目未声明最低 Maven 版本 | `README.md` 使用 `mvn spring-boot:run`、`mvn test`；CI 在 `backend` 运行 `mvn -B verify`；`backend/Dockerfile` 使用 Maven 3.9.9 构建镜像 |
| Node.js | 本地要求 Node.js 18+；CI 使用 Node 20 | `README.md`“前端启动”写明 Node.js 18+；CI 与 `frontend/Dockerfile` 使用 Node 20；lockfile 中 Vite/Rollup/Playwright 的 engine 约束支持当前 Node 版本 |
| npm | 必需；应使用已有 `frontend/package-lock.json` | `frontend/package.json` 定义 `dev`、`build`、`screenshots`；CI 使用 `npm ci` 与 `npm run build`；lockfile 已被 Git 跟踪 |
| Docker CLI / Compose | 容器化启动和三服务联调时需要；本地后端测试和前端构建不需要 | `README.md` 的 `docker compose up --build`；`docker-compose.yml`；后端/前端 Dockerfile；CI 当前不执行 Compose |
| MySQL | `prod`/Compose 需要；本地 `dev` 和测试当前不需要 | `docker-compose.yml` 使用 `mysql:8.4`；`application-prod.yml` 使用 MySQL；`application-dev.yml` 与 `application-test.yml` 使用 H2 |
| H2 | 本地 dev/test 需要，但由 Maven 依赖管理，无需单独安装 | `backend/pom.xml` 的 H2 runtime dependency；`application-dev.yml` 使用文件型 H2；`application-test.yml` 使用内存 H2 |
| Nginx | 只在前端生产容器内需要，无需本机安装 | `frontend/Dockerfile` 的运行阶段使用 `nginx:1.27-alpine` |
| Playwright 浏览器 | 仅重新生成截图时需要，本轮测试/构建不需要额外下载浏览器 | `frontend/package.json` 的 `playwright` devDependency；`screenshots` 脚本；`README.md` 的截图说明 |
| LLM API Key | 默认不需要 | `README.md` 与 `application.yml` 默认 `DEVFLOW_AI_PROVIDER=local-rule`；真实 OpenAI-compatible 调用才需要 Key |

## 3. 工具检查结果

状态含义使用本任务约定的 `ok`、`missing`、`version_mismatch`、`not_required_now`、`manual_confirm_required`。

| 工具 | 检查命令 | 当前版本或结果 | 状态 | 是否满足当前任务 |
| --- | --- | --- | --- | --- |
| Git | `git --version` | `git version 2.54.0.windows.1`；路径 `D:\Git\cmd\git.exe` | `ok` | 是 |
| Java | `java -version` | Microsoft OpenJDK `17.0.19` LTS；路径 `C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot\bin\java.exe` | `ok` | 是，主版本为 17 |
| Maven | `mvn -v` | Apache Maven `3.9.11`；Maven home `C:\Users\wangxun\.local\tools\apache-maven-3.9.11`；使用 Java 17.0.19 | `ok` | 是；主命令成功，未使用备用路径 |
| Node.js | `node -v` | `v24.16.0`；路径 `C:\Program Files\nodejs\node.exe` | `ok` | 是；满足 README 的 18+ 及当前依赖 engine 范围。CI 固定 Node 20，属于版本差异而非不满足 |
| npm | `npm -v` | `11.13.0`；路径 `C:\Program Files\nodejs\npm.ps1` | `ok` | 是 |
| Docker CLI | `docker --version` | 命令不存在 | `manual_confirm_required` | 不影响本地测试/构建；容器验证不满足，安装 Docker Desktop 前需人工确认 |
| Docker Compose | `docker compose version` | 因 Docker 命令不存在而不可用 | `manual_confirm_required` | 不影响本地测试/构建；Compose 联调不满足 |
| MySQL CLI/本机服务 | `mysql --version` | 命令不存在 | `not_required_now` | 是；dev/test 使用 H2。若以后安装本机 MySQL 服务，必须人工确认 |
| H2 | 由 `mvn test` 运行时确认 | H2 `2.2.224`（Maven 依赖） | `ok` | 是，无需独立安装 |
| 本机 Nginx | 未执行版本命令 | 仅由 Docker 镜像提供 | `not_required_now` | 是 |
| Playwright 浏览器 | 本轮未下载浏览器 | 仅截图任务需要 | `not_required_now` | 是 |

Maven 的首选命令 `mvn -v` 已成功，因此未执行备用的 `C:\Users\wangxun.local\tools\apache-maven-3.9.11\bin\mvn.cmd`。实际 Maven home 是 `C:\Users\wangxun\.local\tools\apache-maven-3.9.11`，与题目中备用路径的用户名目录写法不同，但不影响使用。

## 4. 测试与构建验证

### 后端测试

运行目录与命令：

```powershell
Set-Location D:\workhome\ai-coding-workbench\backend
mvn test
```

实际结果：

```text
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 13.527 s
```

测试使用 `test` profile、内存 H2，并实际执行两条 Flyway migration。测试日志出现非阻塞警告：当前 H2 2.2.224 高于该 Flyway 版本明确测试到的 H2 2.2.220；本轮测试仍全部通过，未修改依赖或业务代码。

### 前端依赖与构建

运行目录与命令：

```powershell
Set-Location D:\workhome\ai-coding-workbench\frontend
npm install
npm run build
```

实际结果：

- `npm install`：依赖已是最新，审计 117 个包，`0 vulnerabilities`。
- `frontend/package-lock.json` 在执行前已经存在且被 Git 跟踪；执行前后 SHA-256 均为 `D5032E21788609F4D664FB1A5EF084A4F660084F99E7364A918597C44A3E49E6`，没有新增或改写 lockfile。
- `npm run build`：`vue-tsc --noEmit && vite build` 成功，1961 个模块完成转换，Vite `6.4.3`，构建耗时 `7.77s`。
- 构建输出包含非阻塞提示：Rollup 移除了 `@vueuse/core` 中位置无法解释的 `/* #__PURE__ */` 注释；主 JS chunk 为 `2,211.75 kB`（gzip `733.36 kB`），超过 500 kB 警告阈值。

测试/构建生成的 `backend/target/`、`frontend/dist/`、已有 `frontend/node_modules/` 以及 `backend/data/` 均被 `.gitignore` 忽略，没有加入 Git。

### 未执行的验证

- 未运行 `docker compose up --build`：Docker CLI/Compose 缺失，且 Docker Desktop 安装属于必须人工确认的系统级动作。
- 未连接本机 MySQL：本地 dev/test 使用 H2，当前任务不需要；也未安装 MySQL。
- 未调用真实 LLM：默认 local-rule 模式不需要 API Key，本轮目标仅为环境与构建验证。

## 5. D 盘安装规划（仅规划，未执行）

### Docker Desktop / Docker Compose

- 当前结论：缺失，但仅容器化演示和 Compose 联调需要。
- 建议目录：`D:\DevTools\Docker` 可用于保存经官方来源获取的安装包、配置说明和可迁移数据；不要放入项目目录。
- 安装性质：Docker Desktop 不是纯 ZIP 便携工具，可能涉及 Windows 服务、虚拟化、WSL 2、系统组件和重启，无法保证所有组件都只落在 D 盘。
- 状态：`manual_confirm_required`。只有用户确认安装方式、系统组件影响和数据目录后再执行。
- PATH：当前不修改。若安装器未自动提供 CLI，再单独给出 PATH 方案并等待确认。

### MySQL

- 当前结论：本机 MySQL CLI/服务缺失，但 `dev`/`test` 不需要；容器场景优先使用 `docker-compose.yml` 中固定的 `mysql:8.4`，避免额外维护本机服务。
- 若以后明确需要原生 MySQL：优先考虑官方 ZIP 包解压到 `D:\DevTools\MySQL`，配置文件和数据目录也放在 D 盘；不要放入项目目录。
- 安装性质：初始化数据目录、注册 Windows 服务、开放 3306 端口、设置密码均属于有系统影响的动作。
- 状态：当前 `not_required_now`；一旦决定安装或注册服务，转为 `manual_confirm_required`。
- PATH：只写入后续方案，不在本轮执行。

### 已满足工具

Java 17、Maven、Node.js/npm 和 Git 已满足项目要求，本轮不重复安装，也不因其现有位置不在 `D:\DevTools` 而迁移。若未来希望统一工具目录，可另开任务评估以下便携布局，但当前没有必要：

```text
D:\DevTools\Java
D:\DevTools\Maven
D:\DevTools\Node
```

## 6. 需要人工确认的动作

1. 安装 Docker Desktop，以及启用 WSL 2/Hyper-V、系统服务或重启等相关动作。
2. 安装原生 MySQL、初始化数据、注册 Windows 服务或占用 3306 端口。
3. 对系统或用户 PATH 做任何新增、删除或重排。
4. 下载或安装新的大型软件、安装包、容器运行时或数据库服务。
5. 配置真实 LLM API Key 并进行可能产生费用的外部调用。

本轮没有执行上述任何动作。

## 7. 后续运行命令

### 本地开发（推荐，当前环境可用）

后端：

```powershell
Set-Location D:\workhome\ai-coding-workbench\backend
mvn spring-boot:run
```

前端（新检出或需要严格按 lockfile 重装时优先使用 `npm ci`）：

```powershell
Set-Location D:\workhome\ai-coding-workbench\frontend
npm ci
npm run dev
```

### 重复验证

```powershell
Set-Location D:\workhome\ai-coding-workbench\backend
mvn test

Set-Location D:\workhome\ai-coding-workbench\frontend
npm run build
```

### 容器化运行（仅在人工确认并安装 Docker 后）

```powershell
Set-Location D:\workhome\ai-coding-workbench
docker compose up --build
```

## 8. 变更边界声明

本轮只新增环境审计报告 `docs/ENVIRONMENT_CHECK.md`。没有修改 Java/Vue 业务代码，没有处理 `ai_task`，没有修改 `InMemoryStore`，没有修改 README 简历描述，没有新增接口，没有安装任何软件，没有修改系统环境变量。检查产生的 `target`、`dist`、`node_modules` 和 `data` 均保持为 Git 忽略项；没有提交 `.env`、日志、缓存、安装包或构建产物。
