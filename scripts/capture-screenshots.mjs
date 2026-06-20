import { spawn } from 'node:child_process'
import { mkdir } from 'node:fs/promises'
import net from 'node:net'
import path from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const rootDir = path.resolve(__dirname, '..')
const frontendDir = path.join(rootDir, 'frontend')
const imageDir = path.join(rootDir, 'docs', 'images')
const largeImageDir = path.join(imageDir, 'large')
const frontendPort = Number(process.env.DEVFLOW_FRONTEND_PORT || 5173)
const backendUrl = process.env.DEVFLOW_BACKEND_URL || 'http://127.0.0.1:8080'
const baseUrl = process.env.DEVFLOW_SCREENSHOT_URL || `http://127.0.0.1:${frontendPort}`
const forceRealBackend = process.env.DEVFLOW_SCREENSHOT_MOCK === '0'

const pages = [
  { route: '/', file: 'dashboard.png', title: '工作台概览' },
  { route: '/workbench', file: 'workbench.png', title: 'AI 工作台' },
  { route: '/logs', file: 'log-analyzer.png', title: '日志分析' },
  { route: '/prompts', file: 'prompt-templates.png', title: 'Prompt 模板' },
  { route: '/history', file: 'generation-history.png', title: '生成历史' },
]

async function importPlaywright() {
  const requireFromFrontend = createRequire(path.join(frontendDir, 'package.json'))
  try {
    return requireFromFrontend('playwright')
  } catch {
    throw new Error('缺少 Playwright。请先在 frontend 目录执行：npm install -D playwright')
  }
}

function isPortOpen(port) {
  return new Promise((resolve) => {
    const socket = net.createConnection(port, '127.0.0.1')
    socket.once('connect', () => {
      socket.destroy()
      resolve(true)
    })
    socket.once('error', () => resolve(false))
    socket.setTimeout(1000, () => {
      socket.destroy()
      resolve(false)
    })
  })
}

async function waitForUrl(url, timeoutMs = 60000) {
  const started = Date.now()
  while (Date.now() - started < timeoutMs) {
    try {
      const response = await fetch(url)
      if (response.ok || response.status < 500) return
    } catch {
      // keep waiting
    }
    await new Promise((resolve) => setTimeout(resolve, 800))
  }
  throw new Error(`等待服务超时：${url}`)
}

async function ensureBackend() {
  try {
    const response = await fetch(`${backendUrl}/api/dashboard/stats`)
    return response.ok
  } catch {
    return false
  }
}

async function ensureFrontend() {
  if (await isPortOpen(frontendPort)) return undefined

  const command = process.platform === 'win32' ? 'cmd.exe' : 'npm'
  const args = process.platform === 'win32' ? ['/d', '/s', '/c', 'npm run dev'] : ['run', 'dev']
  const child = spawn(command, args, {
    cwd: frontendDir,
    stdio: ['ignore', 'pipe', 'pipe'],
  })

  child.stdout.on('data', (chunk) => process.stdout.write(`[vite] ${chunk}`))
  child.stderr.on('data', (chunk) => process.stderr.write(`[vite] ${chunk}`))

  await waitForUrl(baseUrl)
  return child
}

function stopProcessTree(child) {
  if (!child || child.killed) return
  if (process.platform === 'win32') {
    spawn('taskkill', ['/pid', String(child.pid), '/T', '/F'], { stdio: 'ignore' })
  } else {
    child.kill('SIGTERM')
  }
}

async function preparePage(page, route) {
  await page.goto(`${baseUrl}${route}`, { waitUntil: 'networkidle' })
  await page.waitForSelector('.shell', { timeout: 30000 })
  await page.addStyleTag({
    content: `
      * { caret-color: transparent !important; }
      .el-message { display: none !important; }
    `,
  })

  if (route === '/logs') {
    const analyzeButton = page.getByRole('button', { name: '分析日志' })
    if (await analyzeButton.count()) {
      await analyzeButton.first().click()
      await page.waitForSelector('.diagnosis-body, .empty-state', { timeout: 30000 })
      await page.waitForTimeout(700)
    }
  } else {
    await page.waitForTimeout(700)
  }
}

const demoProjects = [
  {
    id: 1,
    projectName: 'DevFlow Copilot',
    techStack: 'Java 17 / Spring Boot 3 / MyBatis-Plus / Flyway / Vue 3 / TypeScript / Element Plus',
    readmeContent: 'AI Coding 工作流控制台',
    directoryStructure: 'backend/src/main/java, frontend/src/views, docs/images',
    currentRequirement: '围绕项目上下文、Prompt 模板、Artifact 生成、日志诊断与人工 Review 构建可追踪闭环。',
    codingRules: '不自动提交 Git；所有 AI 输出必须进入 READY_FOR_REVIEW → SAVED → CONFIRMED 状态机。',
    version: 1,
    createdAt: '2026-06-20T09:00:00',
    updatedAt: '2026-06-20T15:40:00',
  },
  {
    id: 2,
    projectName: 'power-plant-system',
    techStack: 'Java 17 / Spring Boot 3 / MySQL / Redis',
    readmeContent: '电厂运维后台示例项目',
    directoryStructure: 'src/main/java/com/power/plant',
    currentRequirement: '排查端口占用、Bean 初始化失败和慢查询日志。',
    codingRules: '先给修复步骤，再给可复制 Prompt。',
    version: 1,
    createdAt: '2026-06-20T09:10:00',
    updatedAt: '2026-06-20T14:10:00',
  },
  {
    id: 3,
    projectName: 'ai-jd-analyzer',
    techStack: 'Spring Boot / Vue 3 / Markdown-it',
    readmeContent: '简历投递分析工具',
    directoryStructure: 'backend, frontend, docs',
    currentRequirement: '生成 README、Commit Message 和投递复盘模块修改计划。',
    codingRules: '输出结构化 Markdown Artifact。',
    version: 1,
    createdAt: '2026-06-20T09:20:00',
    updatedAt: '2026-06-20T13:20:00',
  },
]

const demoPrompts = [
  prompt(1, 'requirement_split_default', '需求拆解标准模板', 'requirement-split', true, true, 3, 'projectName,techStack,requirement,context'),
  prompt(2, 'code_plan_spring_boot', 'Spring Boot 代码变更计划', 'code-plan', true, true, 2, 'projectName,techStack,requirement,codingRules'),
  prompt(3, 'readme_artifact_writer', 'README Artifact 生成模板', 'readme-generate', true, false, 2, 'projectName,requirement,context'),
  prompt(4, 'commit_message_cn', 'Commit Message 中文摘要模板', 'commit-message', true, true, 1, 'requirement,context'),
  prompt(5, 'fix_prompt_for_codex', 'Codex 修复 Prompt 模板', 'fix-prompt', true, true, 4, 'errorLog,projectName,techStack'),
  prompt(6, 'log_root_cause_rule', '日志根因诊断模板', 'log-analysis', false, false, 1, 'errorLog,projectName'),
]

const demoGenerations = [
  generation(16, 1, 'fix-prompt', 'DevFlow Workbench 修复 Prompt', 'READY_FOR_REVIEW', false, 134, 'local-rule', 'local-rule-mvp', 'Codex 修复 Prompt 模板', 4),
  generation(15, 2, 'log-analysis', 'power-plant-system 端口占用', 'READY_FOR_REVIEW', false, 91, 'local-rule', 'local-rule-mvp', '日志根因诊断模板', 1),
  generation(14, 3, 'commit-message', 'ai-jd-analyzer Commit Message', 'CONFIRMED', true, 76, 'local-rule', 'local-rule-mvp', 'Commit Message 中文摘要模板', 1),
  generation(13, 3, 'readme-generate', 'ai-jd-analyzer README 片段', 'CONFIRMED', true, 112, 'local-rule', 'local-rule-mvp', 'README Artifact 生成模板', 2),
  generation(12, 3, 'code-plan', 'ai-jd-analyzer 修改计划', 'SAVED', false, 236, 'local-rule', 'local-rule-mvp', 'Spring Boot 代码变更计划', 2),
  generation(11, 3, 'requirement-split', 'ai-jd-analyzer 投递复盘模块', 'READY_FOR_REVIEW', false, 188, 'local-rule', 'local-rule-mvp', '需求拆解标准模板', 3),
]

const demoLogs = [
  {
    id: 5,
    projectId: 2,
    rawLog: 'Web server failed to start. Port 8080 was already in use.',
    exceptionType: 'PortInUseException',
    possibleReason: '本机 8080 端口已被其他 Spring Boot 进程占用。',
    diagnoseSteps: '1. 执行 netstat -ano | findstr :8080\n2. 定位 PID 并确认进程用途\n3. 停止冲突进程或修改 server.port\n4. 重新启动服务并验证健康检查',
    fixPrompt: '请检查 Spring Boot 服务启动失败的 PortInUseException，给出 Windows 下定位 8080 端口占用、停止进程和调整 server.port 的修复步骤。',
    riskTips: '服务无法启动，接口不可访问；如果直接杀进程，需要确认不是业务依赖服务。',
    riskLevel: 'HIGH',
    createdAt: '2026-06-20T10:18:13',
  },
  {
    id: 4,
    projectId: 2,
    rawLog: 'BeanCreationException: Error creating bean with name alarmService',
    exceptionType: 'BeanCreationException',
    possibleReason: 'Mapper Bean 未注册或包扫描路径不完整。',
    diagnoseSteps: '1. 检查 @MapperScan\n2. 检查 mapper XML / interface 路径\n3. 验证测试 Profile 的配置',
    fixPrompt: '请基于 Spring Boot BeanCreationException 排查 Mapper Bean 未注入问题。',
    riskTips: '启动阶段失败，影响所有依赖该 Bean 的接口。',
    riskLevel: 'MEDIUM',
    createdAt: '2026-06-20T09:42:00',
  },
]

function prompt(id, key, name, type, enabled, isDefault, version, variables) {
  return {
    id,
    templateKey: key,
    templateName: name,
    templateType: type,
    templateContent: `你是 Java 后端 AI Coding 助手。请基于 {{projectName}}、{{techStack}} 和 {{requirement}} 输出可人工 Review 的 Markdown Artifact。`,
    variables,
    enabled,
    isDefault,
    version,
    createdAt: '2026-06-20T09:00:00',
    updatedAt: `2026-06-20T1${id}:20:00`,
  }
}

function generation(id, projectId, generationType, inputSummary, status, confirmed, costTimeMs, providerName, modelName, promptTemplateName, promptTemplateVersion) {
  return {
    id,
    projectId,
    generationType,
    inputSummary,
    inputContent: `${inputSummary}：请输出结构化 Artifact，并标注人工确认步骤。`,
    outputContent: `## ${inputSummary}\n\n- 目标：生成可审查的 AI Coding Artifact。\n- 处理方式：结合项目上下文、Prompt 模板和本地规则输出结果。\n- Review：结果必须保存后再标记已确认。\n\n### 建议动作\n\n1. 检查影响文件与接口边界。\n2. 复制 Artifact 给开发工具继续执行。\n3. 在生成历史中保留 Trace。`,
    status,
    confirmed,
    providerName,
    modelName,
    promptTemplateId: promptTemplateVersion,
    promptTemplateName,
    promptTemplateVersion,
    renderedPrompt: `项目：{{projectName}}\n任务：${inputSummary}\n要求：输出 Markdown Artifact，并进入人工 Review。`,
    promptTokens: 420,
    completionTokens: 680,
    totalTokens: 1100,
    costTimeMs,
    success: status !== 'FAILED',
    errorMessage: '',
    version: 1,
    createdAt: `2026-06-20T1${Math.max(0, id - 10)}:${String((id * 7) % 60).padStart(2, '0')}:00`,
    updatedAt: `2026-06-20T1${Math.max(0, id - 10)}:${String((id * 7 + 3) % 60).padStart(2, '0')}:00`,
  }
}

function apiPayload(data) {
  return {
    status: 200,
    contentType: 'application/json; charset=utf-8',
    body: JSON.stringify({ code: 0, message: 'ok', data }),
  }
}

async function routeMockApi(route) {
  const request = route.request()
  const url = new URL(request.url())
  const pathname = url.pathname
  if (!pathname.startsWith('/api/')) {
    return route.continue()
  }

  if (pathname === '/api/dashboard/stats') {
    return route.fulfill(apiPayload({
      projectCount: demoProjects.length,
      todayGenerationCount: demoGenerations.length,
      logAnalysisCount: demoLogs.length,
      promptTemplateCount: demoPrompts.length,
      recentGenerations: demoGenerations,
    }))
  }

  if (pathname === '/api/projects') return route.fulfill(apiPayload(demoProjects))
  if (pathname === '/api/prompts') return route.fulfill(apiPayload(demoPrompts))
  if (pathname === '/api/logs/history') return route.fulfill(apiPayload(demoLogs))
  if (pathname === '/api/generations') return route.fulfill(apiPayload(demoGenerations))
  if (pathname === '/api/logs/analyze') return route.fulfill(apiPayload({
    id: 6,
    recordId: 17,
    exceptionType: 'PortInUseException',
    possibleReason: 'Spring Boot 内嵌 Tomcat 启动时发现 8080 端口已经被占用，导致 WebServer 初始化失败。',
    diagnoseSteps: '1. 在 Windows 执行 netstat -ano | findstr :8080\n2. 用 tasklist | findstr <PID> 确认占用进程\n3. 停止冲突进程，或在 application.yml 修改 server.port\n4. 重新启动并检查 /api/dashboard/stats 是否可访问',
    fixPrompt: '请作为 Java 后端排障助手，分析 Spring Boot PortInUseException，输出 Windows 下定位端口占用、处理冲突进程、修改 server.port 和验证启动结果的步骤。',
    riskTips: '当前服务无法启动，前端 API 会全部失败；处理端口进程前要确认该进程不是数据库、网关或其他业务服务。',
    riskLevel: 'HIGH',
  }))

  return route.fulfill(apiPayload(null))
}

async function captureSet(browser, viewport, targetDir, suffix = '', useMockApi = false) {
  const page = await browser.newPage({
    viewport,
    deviceScaleFactor: 1,
  })
  if (useMockApi) {
    await page.route('**/api/**', routeMockApi)
  }

  for (const item of pages) {
    await preparePage(page, item.route)
    await page.evaluate(() => window.scrollTo(0, 0))
    await page.waitForTimeout(120)
    await page.screenshot({
      path: path.join(targetDir, item.file.replace('.png', `${suffix}.png`)),
      fullPage: false,
      animations: 'disabled',
    })
    console.log(`已截图：${path.relative(rootDir, path.join(targetDir, item.file.replace('.png', `${suffix}.png`)))} · ${item.title}`)
  }

  await page.close()
}

async function main() {
  const backendReady = await ensureBackend()
  if (!backendReady && forceRealBackend) {
    throw new Error(`后端未就绪：${backendUrl}。请先执行：cd backend && mvn spring-boot:run`)
  }
  if (!backendReady) {
    console.log('后端未就绪，截图脚本将使用内置 demo API 数据。')
  }
  const useMockApi = !backendReady
  const frontendProcess = await ensureFrontend()
  const { chromium } = await importPlaywright()

  await mkdir(imageDir, { recursive: true })
  await mkdir(largeImageDir, { recursive: true })

  const launchOptions = {
    channel: 'chrome',
    headless: true,
    args: ['--hide-scrollbars'],
  }

  let browser
  try {
    browser = await chromium.launch(launchOptions)
  } catch {
    browser = await chromium.launch({ headless: true, args: ['--hide-scrollbars'] })
  }

  try {
    await captureSet(browser, { width: 1440, height: 1100 }, imageDir, '', useMockApi)
    await captureSet(browser, { width: 1920, height: 1080 }, largeImageDir, '-1920', useMockApi)
  } finally {
    await browser.close()
    stopProcessTree(frontendProcess)
  }
}

main().catch((error) => {
  console.error(error.message)
  process.exit(1)
})
