import { spawn } from 'node:child_process'
import { copyFile, mkdir, readFile } from 'node:fs/promises'
import { createRequire } from 'node:module'
import net from 'node:net'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const rootDir = path.resolve(__dirname, '..')
const frontendDir = path.join(rootDir, 'frontend')
const canonicalDir = path.join(rootDir, 'docs', 'images')
const candidateDir = path.join(canonicalDir, 'candidates', 'evidence-workspace-system')
const beforeFile = path.join(candidateDir, 'before-trace-desktop.png')
const baseUrl = process.env.DEVFLOW_EVIDENCE_SCREENSHOT_URL || 'http://127.0.0.1:5178'
const frontendPort = Number(new URL(baseUrl).port || 5178)

const projects = [{
  id: 1,
  projectName: 'DevFlow Copilot',
  techStack: 'Java 17 / Spring Boot 3 / Vue 3 / TypeScript',
  readmeContent: 'AI Coding Workflow Evidence Workspace',
  directoryStructure: 'backend, frontend, docs',
  currentRequirement: '只展示真实 AgentStep、Tool Call、Generation Trace 与 Human Review。',
  codingRules: '不自动修改代码或 Git。',
  version: 1,
  createdAt: '2026-07-29T09:00:00',
  updatedAt: '2026-07-29T14:32:00',
}]

const runs = [
  run(42, '解释 local-rule fallback 并整理 Trace Evidence 重构计划', 'READY_FOR_REVIEW', 'local-rule', '14:32'),
  run(41, '校验 Knowledge References 注入边界', 'CONFIRMED', 'local-rule', '14:18'),
  run(40, '对比 Tool Call 与 Prompt 输出', 'FAILED', 'openai-compatible', '13:57'),
  run(39, '复核 README 的 Provider 选择', 'SAVED', 'local-rule', '13:41'),
]

const generationRecord = {
  id: 2042,
  projectId: 1,
  generationType: 'requirement-split',
  inputSummary: '解释 Provider fallback 与 Trace Evidence 页面重构边界。',
  inputContent: '为 DevFlow Copilot 输出一份可复核的 Trace Evidence 重构计划，保留现有路由与 API。',
  outputContent: [
    '保留 /agent-runs 路由与 API contract。',
    'Evidence Stream 只展示真实 AgentStep。',
    'Tool Call 只按 stepId 关联。',
    'Request Changes / Reject 没有 API，保持 disabled。',
  ].join('\n'),
  status: 'READY_FOR_REVIEW',
  confirmed: false,
  providerName: 'local-rule',
  modelName: 'local-rule-mvp',
  promptTemplateId: 4,
  promptTemplateName: '需求拆解标准模板',
  promptTemplateVersion: 4,
  renderedPrompt: '只生成 review-only Artifact；不得自动修改代码、提交 Git 或部署。',
  promptTokens: 312,
  completionTokens: 553,
  totalTokens: 865,
  costTimeMs: 175,
  success: true,
  errorMessage: 'OpenAI-compatible 预检未发现 DEVFLOW_AI_API_KEY；按显式配置降级至 local-rule。未发出真实模型请求。',
  version: 1,
  createdAt: '2026-07-29T14:32:08',
  updatedAt: '2026-07-29T14:32:08',
}

const steps = [
  step(4201, 1, 'TASK_DECOMPOSITION', '任务接收', 'SUCCESS', '接收 requirement-split 请求，并绑定 DevFlow Copilot 项目上下文。', 4, '14:32:08.004'),
  step(4202, 2, 'PROMPT_RENDER', 'Prompt 模板渲染', 'SUCCESS', '使用需求拆解标准模板 v4，合并项目边界与 Knowledge 上下文。', 11, '14:32:08.015'),
  step(4203, 3, 'KNOWLEDGE_RETRIEVAL', 'Knowledge Base 检索', 'SUCCESS', '关键词 / 简单相似度检索命中 2 个 chunk，并注入 Prompt 上下文。', 17, '14:32:08.032'),
  step(4204, 4, 'PROVIDER_ROUTE', 'Provider 路由与 fallback', 'FALLBACK', 'OpenAI-compatible 缺少 API Key，按明确配置降级到 local-rule；没有发出真实模型请求。', 9, '14:32:08.041'),
  step(4205, 5, 'LLM_GENERATION', 'Provider 生成 Artifact', 'SUCCESS', 'local-rule-mvp 返回结构化 Artifact，Generation Record 进入 READY_FOR_REVIEW。', 134, '14:32:08.175'),
  step(4206, 6, 'HUMAN_REVIEW', '人工复核', 'PENDING', '生成结果等待开发者先保存，再执行最终确认。', 0, '14:32:08.175', false),
]

const toolCalls = [
  tool(5001, 4202, 'prompt-template-render', 'requirement-split / templateId=4 / projectId=1', '渲染 1,046 字符；未包含 API Key。', 11),
  tool(5002, 4203, 'keyword-knowledge-search', 'local-rule provider trace human review', '命中 2 个 Knowledge Reference。', 17),
  tool(5003, 4204, 'generation-provider', 'openai-compatible / fallbackToLocal=true', 'local-rule / local-rule-mvp；保留明确 fallback reason。', 9),
  tool(5004, undefined, 'run-audit-export', 'runId=42', 'Run-level evidence；接口未返回可靠 stepId。', 3),
]

const trace = {
  run: runs[0],
  steps,
  toolCalls,
  humanReviews: [{
    id: 6001,
    runId: 42,
    generationRecordId: 2042,
    reviewStatus: 'PENDING',
    reviewer: 'manual',
    comment: '等待开发者保存或确认 Artifact。Request Changes 与 Reject 当前没有 API。',
    createdAt: '2026-07-29T14:32:08',
    updatedAt: '2026-07-29T14:32:08',
  }],
}

const generationTraces = [{
  id: 7001,
  generationRecordId: 2042,
  promptVersion: 4,
  inputVariables: '{"projectName":"DevFlow Copilot","generationType":"requirement-split"}',
  renderedPromptSummary: '生成 review-only Trace Evidence 重构计划。',
  providerName: 'local-rule',
  modelName: 'local-rule-mvp',
  status: 'SUCCESS',
  latencyMs: 134,
  errorMessage: generationRecord.errorMessage,
  createdAt: '2026-07-29T14:32:08',
}]

const references = [
  {
    documentId: 8,
    documentTitle: 'DevFlow Copilot 项目边界',
    chunkId: 801,
    chunkIndex: 0,
    score: 0.94,
    citationLabel: '项目边界#0',
    snippet: '默认使用 local-rule 本地规则生成；只有配置 API Key 后才调用真实 OpenAI-compatible Provider。',
  },
  {
    documentId: 9,
    documentTitle: 'Agent Workflow Boundary',
    chunkId: 901,
    chunkIndex: 1,
    score: 0.82,
    citationLabel: 'Workflow Boundary#1',
    snippet: 'Agent Run Trace 是包含 steps、tool calls 与 human review 的可解释审计记录，不是完整自主多 Agent Runtime。',
  },
]

function run(id, title, status, providerName, time) {
  return {
    id,
    projectId: 1,
    generationRecordId: id === 42 ? 2042 : 2000 + id,
    title,
    goal: '生成 review-only Artifact',
    status,
    providerName,
    modelName: providerName === 'local-rule' ? 'local-rule-mvp' : undefined,
    latencyMs: id === 42 ? 175 : undefined,
    startedAt: `2026-07-29T${time}:08`,
    completedAt: status === 'READY_FOR_REVIEW' ? undefined : `2026-07-29T${time}:09`,
    createdAt: `2026-07-29T${time}:08`,
    updatedAt: `2026-07-29T${time}:09`,
  }
}

function step(id, stepOrder, stepType, stepName, status, summary, latencyMs, clock, completed = true) {
  const base = `2026-07-29T${clock.slice(0, 8)}`
  return {
    id,
    runId: 42,
    stepOrder,
    stepType,
    stepName,
    status,
    summary,
    latencyMs,
    startedAt: base,
    completedAt: completed ? base : undefined,
  }
}

function tool(id, stepId, toolName, inputSummary, outputSummary, latencyMs) {
  return {
    id,
    runId: 42,
    stepId,
    toolName,
    inputSummary,
    outputSummary,
    status: toolName === 'generation-provider' ? 'FALLBACK' : 'SUCCESS',
    latencyMs,
    createdAt: '2026-07-29T14:32:08',
  }
}

function payload(data) {
  return {
    status: 200,
    contentType: 'application/json; charset=utf-8',
    body: JSON.stringify({ code: 0, message: 'ok', data }),
  }
}

function traceForScenario(scenario) {
  if (scenario === 'short-run') return { ...trace, steps: steps.slice(0, 2) }
  if (scenario === 'long-run') {
    const additional = Array.from({ length: 8 }, (_, index) => ({
      ...steps[index % steps.length],
      id: 4300 + index,
      stepOrder: 7 + index,
      stepName: `长 Run 验证步骤 ${index + 7}`,
      summary: `这是长 Run 的第 ${index + 7} 个真实 DTO 形状 QA Step，用于验证 Evidence Stream 独立滚动。`,
    }))
    return { ...trace, steps: [...steps, ...additional] }
  }
  if (scenario === 'no-tool') return { ...trace, toolCalls: [] }
  if (scenario === 'multiple-tools') {
    return {
      ...trace,
      toolCalls: [
        ...toolCalls,
        tool(5005, 4201, 'project-context-bind', 'projectId=1', '已绑定项目上下文。', 2),
        tool(5006, 4201, 'requirement-validator', 'generationType=requirement-split', '字段校验通过。', 1),
      ],
    }
  }
  if (scenario === 'no-review') return { ...trace, humanReviews: [] }
  if (scenario === 'confirmed-review' || scenario === 'rejected-review') {
    const reviewStatus = scenario === 'confirmed-review' ? 'CONFIRMED' : 'REJECTED'
    return {
      ...trace,
      humanReviews: trace.humanReviews.map((item) => ({
        ...item,
        reviewStatus,
        comment: reviewStatus === 'CONFIRMED' ? '已由人工确认。' : '人工复核已驳回该 Artifact。',
      })),
    }
  }
  if (scenario === 'failed-step') {
    return {
      ...trace,
      steps: steps.map((item, index) => index === 2
        ? { ...item, status: 'FAILED', summary: 'Knowledge 查询失败；后端返回明确 FAILED 状态。' }
        : item),
    }
  }
  if (scenario === 'long-chinese') {
    return {
      ...trace,
      steps: steps.map((item, index) => index === 0
        ? { ...item, summary: '长中文验收：'.concat('当任务名称、证据摘要与状态说明同时超过两到三行时，页面仍需保持完整来源、键盘焦点与可读行高。'.repeat(8)) }
        : item),
    }
  }
  return trace
}

function recordForScenario(scenario) {
  if (scenario === 'confirmed-review') {
    return { ...generationRecord, status: 'CONFIRMED', confirmed: true }
  }
  if (scenario === 'rejected-review') {
    return { ...generationRecord, status: 'REJECTED', confirmed: false }
  }
  if (scenario === 'long-json') {
    return {
      ...generationRecord,
      outputContent: JSON.stringify({
        title: 'Long Normalized Snapshot QA Fixture',
        payload: 'long-json-value-'.repeat(600),
      }),
    }
  }
  return generationRecord
}

async function mockApi(route, scenario = 'normal') {
  const request = route.request()
  const url = new URL(request.url())
  const pathname = url.pathname
  if (!pathname.startsWith('/api/')) return route.continue()

  if (pathname === '/api/projects') return route.fulfill(payload(projects))
  if (pathname === '/api/agent-runs') return route.fulfill(payload(scenario === 'empty' ? [] : runs))
  if (pathname === '/api/generations') {
    return route.fulfill(payload(scenario === 'missing-generation' ? [] : [recordForScenario(scenario)]))
  }
  if (pathname === '/api/agent-runs/42/trace') {
    if (scenario === 'loading') await new Promise((resolve) => setTimeout(resolve, 1200))
    if (scenario === 'error') {
      return route.fulfill({
        status: 500,
        contentType: 'application/json; charset=utf-8',
        body: JSON.stringify({ code: 5000, message: 'QA fixture trace detail error', data: null }),
      })
    }
    return route.fulfill(payload(traceForScenario(scenario)))
  }
  if (pathname === '/api/generation-traces') {
    return route.fulfill(payload(scenario === 'missing-generation' ? [] : generationTraces))
  }
  if (pathname === '/api/knowledge/references') {
    return route.fulfill(payload(scenario === 'no-knowledge' ? [] : references))
  }
  if (pathname === '/api/generations/2042/save') {
    return route.fulfill(payload({ ...generationRecord, status: 'SAVED' }))
  }
  if (pathname === '/api/generations/2042/confirm') {
    return route.fulfill(payload({ ...generationRecord, status: 'CONFIRMED', confirmed: true }))
  }
  return route.fulfill(payload(null))
}

async function importPlaywright() {
  const requireFromFrontend = createRequire(path.join(frontendDir, 'package.json'))
  return requireFromFrontend('playwright')
}

function isPortOpen(port) {
  return new Promise((resolve) => {
    const socket = net.createConnection(port, '127.0.0.1')
    socket.once('connect', () => {
      socket.destroy()
      resolve(true)
    })
    socket.once('error', () => resolve(false))
    socket.setTimeout(800, () => {
      socket.destroy()
      resolve(false)
    })
  })
}

async function waitForFrontend() {
  const started = Date.now()
  while (Date.now() - started < 45000) {
    try {
      const response = await fetch(baseUrl)
      if (response.ok) return
    } catch {
      // keep waiting
    }
    await new Promise((resolve) => setTimeout(resolve, 500))
  }
  throw new Error(`Vite 未在 ${baseUrl} 就绪`)
}

async function ensureFrontend() {
  if (await isPortOpen(frontendPort)) return undefined
  const child = process.platform === 'win32'
    ? spawn('cmd.exe', ['/d', '/s', '/c', `npm.cmd run dev -- --host 127.0.0.1 --port ${frontendPort}`], {
        cwd: frontendDir,
        stdio: ['ignore', 'pipe', 'pipe'],
      })
    : spawn('npm', ['run', 'dev', '--', '--host', '127.0.0.1', '--port', String(frontendPort)], {
        cwd: frontendDir,
        stdio: ['ignore', 'pipe', 'pipe'],
      })
  child.stdout.on('data', (chunk) => process.stdout.write(`[vite] ${chunk}`))
  child.stderr.on('data', (chunk) => process.stderr.write(`[vite] ${chunk}`))
  await waitForFrontend()
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

async function capturePage(browser, viewport, fileName, openMobileInspector = false) {
  const page = await browser.newPage({ viewport, deviceScaleFactor: 1 })
  await page.route('**/api/**', (route) => mockApi(route, 'normal'))
  await page.goto(`${baseUrl}/agent-runs?generationRecordId=2042&qaFixture=1`, { waitUntil: 'networkidle' })
  await page.waitForSelector('.evidence-event-row', { timeout: 30000 })
  await page.addStyleTag({
    content: `
      * { caret-color: transparent !important; }
      .el-message { display: none !important; }
    `,
  })
  if (openMobileInspector) {
    await page.evaluate(() => {
      document.querySelector('.mobile-inspector-trigger')?.click()
    })
    await page.waitForSelector('.inspector-pane.open')
  }
  await page.waitForTimeout(250)
  await assertNoPageOverflow(page, fileName)
  await page.screenshot({
    path: path.join(candidateDir, fileName),
    fullPage: false,
    animations: 'disabled',
  })
  await page.close()
  console.log(path.relative(rootDir, path.join(candidateDir, fileName)))
}

async function assertNoPageOverflow(page, label) {
  const overflow = await page.evaluate(() => ({
    innerWidth: window.innerWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }))
  if (overflow.scrollWidth > overflow.innerWidth) {
    throw new Error(`${label} 存在页面横向溢出：${overflow.scrollWidth} > ${overflow.innerWidth}`)
  }
}

async function openScenario(browser, scenario, viewport = { width: 1280, height: 800 }) {
  const page = await browser.newPage({ viewport, deviceScaleFactor: 1 })
  await page.route('**/api/**', (route) => mockApi(route, scenario))
  await page.goto(`${baseUrl}/agent-runs?generationRecordId=2042&qaFixture=1`, { waitUntil: 'domcontentloaded' })
  return page
}

async function verifyStateMatrix(browser) {
  const checks = []

  async function verify(name, assertion, viewport) {
    const page = await openScenario(browser, name, viewport)
    try {
      await assertion(page)
      await assertNoPageOverflow(page, name)
      checks.push(name)
    } finally {
      await page.close()
    }
  }

  await verify('normal', async (page) => {
    await page.waitForSelector('.evidence-event-row')
    if (await page.locator('.evidence-event-row').count() !== 6) throw new Error('Normal 应包含 6 个 fixture steps')
  })
  await verify('loading', async (page) => {
    await page.waitForSelector('.workspace-loading')
  })
  await verify('empty', async (page) => {
    await page.waitForSelector('.workspace-empty-page')
  })
  await verify('error', async (page) => {
    await page.waitForSelector('.workspace-error')
  })
  await verify('short-run', async (page) => {
    await page.waitForSelector('.evidence-event-row')
    if (await page.locator('.evidence-event-row').count() !== 2) throw new Error('Short Run 应只显示 2 个 AgentStep')
  })
  await verify('long-run', async (page) => {
    await page.waitForSelector('.evidence-event-row')
    if (await page.locator('.evidence-event-row').count() !== 14) throw new Error('Long Run fixture 数量不正确')
  })
  await verify('no-tool', async (page) => {
    await page.waitForSelector('.evidence-event-row')
    if (await page.locator('.event-tool-chip').count()) throw new Error('Zero Tool 行不应显示 Tool 文案')
  })
  await verify('multiple-tools', async (page) => {
    await page.waitForSelector('.evidence-event-row')
    if (!(await page.locator('.event-tool-chip').first().innerText()).includes('Tool')) throw new Error('有 Tool 时未显示紧凑数量标签')
    await page.getByRole('tab', { name: /工具调用/ }).click()
    if (await page.locator('.tool-call-card').count() < 3) throw new Error('Multiple Tools 未显示多条精确 stepId 关联')
  })
  await verify('no-review', async (page) => {
    await page.waitForSelector('.evidence-event-row')
    if (!(await page.locator('.run-context-facts').innerText()).includes('未进入人工复核')) {
      throw new Error('No Review 被错误表示')
    }
  })
  await verify('confirmed-review', async (page) => {
    await page.waitForSelector('.evidence-event-row')
    if (!(await page.locator('.run-context-facts').innerText()).includes('复核已确认')) {
      throw new Error('Confirmed Review 映射错误')
    }
  })
  await verify('rejected-review', async (page) => {
    await page.waitForSelector('.evidence-event-row')
    if (!(await page.locator('.run-context-facts').innerText()).includes('复核已驳回')) {
      throw new Error('Rejected Review 映射错误')
    }
  })
  await verify('failed-step', async (page) => {
    await page.waitForSelector('.evidence-event-row[data-tone="error"]')
  })
  await verify('missing-generation', async (page) => {
    await page.waitForSelector('.derived-attention')
    if (!(await page.locator('.derived-attention').innerText()).includes('缺少 Generation Record')) {
      throw new Error('Missing Generation 未明确标记 Derived Attention')
    }
  })
  await verify('no-knowledge', async (page) => {
    await page.waitForSelector('.derived-attention')
    const attention = await page.locator('.derived-attention').innerText()
    if (!attention.includes('证据缺口')) throw new Error('No Knowledge 未明确标记')
    return
    if (!(await page.locator('.derived-attention').innerText()).includes('未返回 Knowledge Reference')) {
      throw new Error('No Knowledge 未明确标记')
    }
  })
  await verify('long-chinese', async (page) => {
    await page.waitForSelector('.evidence-event-row')
    const summary = await page.locator('.event-summary').first().textContent()
    if (!summary || summary.length < 300) throw new Error('Long Chinese fixture 未完整进入 View Model')
  })
  await verify('long-json', async (page) => {
    await page.waitForSelector('.evidence-event-row')
    await page.getByRole('tab', { name: /数据快照/ }).click()
    const scrollable = await page.locator('.snapshot-section pre').evaluate((element) => {
      return element.scrollWidth > element.clientWidth && element.scrollHeight > element.clientHeight
    })
    if (!scrollable) throw new Error('Long JSON 未保持 Inspector 内部滚动')
  })

  const interactionPage = await openScenario(browser, 'normal')
  try {
    await interactionPage.waitForSelector('.evidence-event-row')
    const first = interactionPage.locator('.evidence-event-row').first()
    await first.focus()
    await interactionPage.keyboard.press('ArrowDown')
    const secondSelected = await interactionPage.locator('.evidence-event-row').nth(1).getAttribute('aria-selected')
    const activeId = await interactionPage.evaluate(() => document.activeElement?.id)
    if (secondSelected !== 'true' || activeId !== 'evidence-event-step-4202') {
      throw new Error('Keyboard Focus / ArrowDown selection 失败')
    }
    checks.push('keyboard-focus')
  } finally {
    await interactionPage.close()
  }

  const desktopPage = await openScenario(browser, 'normal', { width: 1440, height: 900 })
  try {
    await desktopPage.waitForSelector('.inspector-pane.open')
    const role = await desktopPage.locator('.inspector-pane').getAttribute('role')
    const modal = await desktopPage.locator('.inspector-pane').getAttribute('aria-modal')
    if (role !== 'complementary' || modal) throw new Error('Desktop Inspector 不应为 modal dialog')
    await desktopPage.keyboard.press('Escape')
    if (!(await desktopPage.locator('.inspector-pane').evaluate((node) => node.classList.contains('open')))) {
      throw new Error('Desktop Escape 不应关闭常驻 Inspector')
    }
    checks.push('desktop-inspector-semantics')
  } finally {
    await desktopPage.close()
  }

  const mobilePage = await openScenario(browser, 'normal', { width: 390, height: 844 })
  try {
    await mobilePage.waitForSelector('.mobile-inspector-trigger')
    if (await mobilePage.locator('.inspector-pane.open').count()) throw new Error('Mobile Inspector 应默认关闭')
    await mobilePage.evaluate(() => document.querySelector('.mobile-inspector-trigger')?.click())
    await mobilePage.waitForSelector('.inspector-pane.open')
    await mobilePage.locator('.inspector-close').click()
    await mobilePage.waitForFunction(() => !document.querySelector('.inspector-pane')?.classList.contains('open'))
    checks.push('inspector-closed')
    await assertNoPageOverflow(mobilePage, 'mobile-inspector-closed')
  } finally {
    await mobilePage.close()
  }

  const drawerPage = await openScenario(browser, 'normal', { width: 1024, height: 768 })
  try {
    const trigger = drawerPage.locator('.mobile-inspector-trigger')
    await trigger.click()
    await drawerPage.waitForSelector('.inspector-pane.open')
    const dialogFocused = await drawerPage.evaluate(() => document.activeElement?.getAttribute('role') === 'dialog')
    if (!dialogFocused) throw new Error('Inspector 打开后未获得焦点')
    const shellInert = await drawerPage.evaluate(() => Boolean(document.querySelector('.sidebar')?.hasAttribute('inert') && document.querySelector('.topbar')?.hasAttribute('inert') && document.querySelector('.run-context-bar')?.hasAttribute('inert') && document.querySelector('.evidence-stream-pane')?.hasAttribute('inert')))
    if (!shellInert) throw new Error('Drawer 打开时背景未完整 inert')
    await drawerPage.keyboard.press('Tab')
    await drawerPage.keyboard.press('Shift+Tab')
    const focusInside = await drawerPage.evaluate(() => Boolean(document.activeElement?.closest('.inspector-pane')))
    if (!focusInside) throw new Error('Drawer Focus Trap 失败')
    await drawerPage.getByRole('tab', { name: /上下文/ }).focus()
    await drawerPage.keyboard.press('ArrowRight')
    if ((await drawerPage.getByRole('tab', { name: /证据/ }).getAttribute('aria-selected')) !== 'true') throw new Error('Tabs ArrowRight 失败')
    await drawerPage.keyboard.press('End')
    if ((await drawerPage.getByRole('tab', { name: /人工复核/ }).getAttribute('aria-selected')) !== 'true') throw new Error('Tabs End 失败')
    await drawerPage.keyboard.press('Home')
    if ((await drawerPage.getByRole('tab', { name: /上下文/ }).getAttribute('aria-selected')) !== 'true') throw new Error('Tabs Home 失败')
    await drawerPage.keyboard.press('Escape')
    await drawerPage.waitForFunction(() => !document.querySelector('.inspector-pane')?.classList.contains('open'))
    const focusRestored = await drawerPage.evaluate(() => document.activeElement?.classList.contains('mobile-inspector-trigger'))
    if (!focusRestored) throw new Error('Inspector 关闭后未恢复触发焦点')
    checks.push('escape-focus-restore')
  } finally {
    await drawerPage.close()
  }

  const mobileSelectorPage = await openScenario(browser, 'normal', { width: 390, height: 844 })
  try {
    await mobileSelectorPage.waitForSelector('.mobile-run-selector')
    if (await mobileSelectorPage.locator('.run-ledger-pane').isVisible()) throw new Error('Mobile Run Ledger 应隐藏')
    if (!(await mobileSelectorPage.locator('.mobile-run-selector').innerText()).includes('run')) throw new Error('Mobile Run Selector 未显示当前 Run 上下文')
    checks.push('mobile-run-selector')
  } finally {
    await mobileSelectorPage.close()
  }

  const reducedPage = await browser.newPage({
    viewport: { width: 1280, height: 800 },
    reducedMotion: 'reduce',
  })
  await reducedPage.route('**/api/**', (route) => mockApi(route, 'normal'))
  try {
    await reducedPage.goto(`${baseUrl}/agent-runs?generationRecordId=2042&qaFixture=1`, { waitUntil: 'domcontentloaded' })
    await reducedPage.waitForSelector('.evidence-event-row')
    const motion = await reducedPage.evaluate(() => getComputedStyle(document.documentElement).getPropertyValue('--motion-standard').trim())
    if (motion !== '0ms') throw new Error(`Reduced Motion token 未归零：${motion}`)
    checks.push('reduced-motion')
  } finally {
    await reducedPage.close()
  }

  console.log(`State matrix verified (${checks.length}): ${checks.join(', ')}`)
}

async function createComparison(browser) {
  const before = await readFile(beforeFile)
  const after = await readFile(path.join(candidateDir, 'after-trace-1440x900.png'))
  const page = await browser.newPage({ viewport: { width: 1600, height: 620 }, deviceScaleFactor: 1 })
  await page.setContent(`
    <style>
      * { box-sizing: border-box; }
      body { margin: 0; padding: 18px; background: #0a0f13; color: #edf3f1; font-family: "Microsoft YaHei UI", sans-serif; }
      header { height: 46px; display: flex; justify-content: space-between; align-items: flex-start; }
      h1 { margin: 0; font-size: 20px; font-weight: 650; }
      p { margin: 5px 0 0; color: #7e8d90; font-size: 11px; }
      main { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
      figure { margin: 0; overflow: hidden; border: 1px solid #26333a; border-radius: 8px; background: #11191f; }
      figcaption { height: 34px; display: flex; align-items: center; justify-content: space-between; padding: 0 10px; border-bottom: 1px solid #26333a; font-size: 12px; }
      figcaption span { color: #65d3a4; font-family: Consolas, monospace; font-size: 10px; }
      img { display: block; width: 100%; height: 500px; object-fit: cover; object-position: top left; }
    </style>
    <header>
      <div><h1>DevFlow · Run Evidence Workspace</h1><p>Phase 1 candidate comparison · real Vue page on the right</p></div>
    </header>
    <main>
      <figure><figcaption>Before · fixed seven-step summary <span>canonical baseline</span></figcaption><img src="data:image/png;base64,${before.toString('base64')}"></figure>
      <figure><figcaption>After · Layered Graphite Evidence Workspace <span>development QA fixture</span></figcaption><img src="data:image/png;base64,${after.toString('base64')}"></figure>
    </main>
  `)
  await page.screenshot({
    path: path.join(candidateDir, 'before-after-trace.png'),
    fullPage: false,
    animations: 'disabled',
  })
  await page.close()
  console.log(path.relative(rootDir, path.join(candidateDir, 'before-after-trace.png')))
}

async function main() {
  await mkdir(candidateDir, { recursive: true })
  await copyFile(path.join(canonicalDir, 'trace-evidence.png'), beforeFile)
  const frontendProcess = await ensureFrontend()
  const { chromium } = await importPlaywright()
  let browser
  try {
    browser = await chromium.launch({ channel: 'chrome', headless: true, args: ['--hide-scrollbars'] })
  } catch {
    browser = await chromium.launch({ headless: true, args: ['--hide-scrollbars'] })
  }

  try {
    await verifyStateMatrix(browser)
    await capturePage(browser, { width: 1440, height: 900 }, 'after-trace-1440x900.png')
    await capturePage(browser, { width: 1280, height: 800 }, 'after-trace-1280x800.png')
    await capturePage(browser, { width: 1024, height: 768 }, 'after-trace-1024x768.png')
    await capturePage(browser, { width: 390, height: 844 }, 'after-trace-390x844.png', true)
    await createComparison(browser)
  } finally {
    await browser.close()
    stopProcessTree(frontendProcess)
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
