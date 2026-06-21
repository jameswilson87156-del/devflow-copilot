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

## 2026-06-21 — WSL Docker Compose Plugin 安装记录

### 安装前审查

- 检查时间：`2026-06-21 22:12:28 +08:00`（Asia/Shanghai）。
- WSL 发行版：`Ubuntu 22.04.5 LTS`，codename `jammy`，架构 `amd64`。
- Docker Engine：客户端和服务端均为 `29.1.3`，Server 正常；检查时有 3 个运行容器、4 个镜像，Docker Root Dir 为 `/var/lib/docker`。
- Engine 包来源：Ubuntu 仓库的 `docker.io 29.1.3-0ubuntu3~22.04.2`，候选来自 `jammy-updates/universe` 与 `jammy-security/universe`，不是 Docker CE。
- 已有运行时包：`containerd 2.2.1-0ubuntu1~22.04.1`、`runc 1.3.4-0ubuntu1~22.04.1`。
- 系统原有旧版独立命令包：`docker-compose 1.29.2-1`；它不提供 `docker compose` CLI Plugin。
- 安装前 `/etc/apt/sources.list.d/` 为空，系统 apt sources 中不存在 `download.docker.com`。因此 Ubuntu 当前配置的软件源无法解析官方包名 `docker-compose-plugin`，此前出现 `E: Unable to locate package docker-compose-plugin`。

### Docker 官方 apt repository 配置

由于当前终端无法交互输入 sudo 密码，实际在同一个 `Ubuntu-22.04` 发行版内通过 WSL root 用户执行等价的官方 apt 命令，没有修改 Windows 权限或环境变量。

执行内容摘要：

```bash
apt-get update
apt-get install -y ca-certificates curl gnupg
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg
echo 'deb [arch=amd64 signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu jammy stable' \
  > /etc/apt/sources.list.d/docker.list
apt-get update
```

结果：

- Docker Release GPG key 获取成功，指纹为 `9DC8 5822 9FC7 DD38 854A E2D8 8D81 803C 0EBF CD88`。
- 新增 keyring：`/etc/apt/keyrings/docker.gpg`。
- 新增 apt source：`/etc/apt/sources.list.d/docker.list`。
- 官方仓库 `https://download.docker.com/linux/ubuntu jammy stable` 索引更新成功。
- `curl` 和 `gnupg` 已是最新版本；`ca-certificates` 从 `20240203~22.04.1` 升级到 `20260601~22.04.1`。
- 仓库配置后，`docker-compose-plugin` 候选版本为 `5.1.4-1~ubuntu.22.04~jammy`。

### 安装安全检查与 Plugin 安装

安装前运行：

```bash
apt-get -s install docker-compose-plugin
```

模拟结果为 `0 upgraded, 2 newly installed, 0 to remove`，仅计划新增：

- `docker-compose-plugin 5.1.4-1~ubuntu.22.04~jammy`
- 依赖 `docker-buildx-plugin 0.34.1-1~ubuntu.22.04~jammy`

模拟结果没有移除、替换或升级 `docker.io`、`containerd`、`runc`。确认无破坏性变更后执行：

```bash
apt-get install -y docker-compose-plugin
```

安装成功，实际新增上述两个 Plugin 包，占用约 105 MB；原有 `docker-compose 1.29.2-1` 包未删除。

### 安装后验证

版本检查：

```text
Docker Compose version v5.1.4
Docker version 29.1.3, build 29.1.3-0ubuntu3~22.04.2
```

`docker info` 继续正常显示 Server 信息，并识别以下 CLI Plugins：

- Compose：`v5.1.4`，路径 `/usr/libexec/docker/cli-plugins/docker-compose`
- Buildx：`v0.34.1`，路径 `/usr/libexec/docker/cli-plugins/docker-buildx`

现有 Docker Engine、3 个运行容器、镜像、volume 和 `/var/lib/docker` 数据没有被删除或重装；没有重启 WSL，也没有重启电脑。

审查过程有一项已如实记录的非破坏性异常：第一次执行包列表 grep 时，PowerShell/WSL 引号传递错误导致 `containerd` 被当作命令调用，并立即以 `chmod /var/lib/containerd: operation not permitted` 失败。随后改正引号并完成只读检查；没有产生包变更、服务替换或数据清理。

### Compose 配置验证

运行：

```bash
cd /mnt/d/workhome/ai-coding-workbench
docker compose config
```

结果：成功，退出码为 0。Compose 能完整解析：

- 三个服务：`mysql`、`backend`、`frontend`
- backend 对 MySQL healthcheck 的依赖关系
- 端口：MySQL `3306`、backend `8080`、frontend `5173`
- 默认网络 `ai-coding-workbench_default`
- 持久化卷 `ai-coding-workbench_devflow-mysql-data`

本轮没有运行 `docker compose up --build`，没有拉取项目镜像、构建镜像或创建本项目的容器/网络/volume；该动作仍需后续人工确认。

### 项目重新验证

Windows 后端：

```text
mvn test
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 14.001 s
```

Windows 前端：

```text
npm run build
vue-tsc --noEmit && vite build
1961 modules transformed
built in 9.10s
```

前端构建成功，仍有已知的 `@vueuse/core` PURE 注释提示和大于 500 kB 的 chunk 体积警告。本轮没有运行 `npm install`。

### 边界确认

- Docker Desktop：未安装，当前不需要。
- MySQL：未在 Windows 或 WSL 安装；项目本地默认使用 H2。Compose 中的 MySQL 仅为尚未启动的容器服务定义。
- Windows PATH：未修改。
- Docker Engine：未删除、未重装、未替换，仍使用 Ubuntu `docker.io 29.1.3`。
- 项目业务代码：未修改；未处理 `ai_task`、`InMemoryStore` 或 README 简历描述。
- 项目文件：本节仅更新 `docs/ENVIRONMENT_CHECK.md`；构建产物与缓存保持为 Git 忽略项。
