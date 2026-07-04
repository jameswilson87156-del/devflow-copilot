import { mkdir, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const rootDir = path.resolve(__dirname, '..')
const frontendDir = path.join(rootDir, 'frontend')
const imageDir = path.join(rootDir, 'docs', 'images')

const baseUrl = process.env.DEVFLOW_SCREENSHOT_URL || 'http://127.0.0.1:5174'
const apiUrl = process.env.DEVFLOW_API_URL || 'http://127.0.0.1:18081/api'
const onlyShot = readOnlyShotArg()
const viewport = {
  width: Number(process.env.DEVFLOW_SCREENSHOT_WIDTH || 1440),
  height: Number(process.env.DEVFLOW_SCREENSHOT_HEIGHT || 900),
}

const shots = [
  { route: '/', file: 'dashboard.png', aliases: ['dashboard-agentic.png'], waitFor: '.dashboard' },
  { route: '/workbench', file: 'workbench.png', aliases: ['workbench-running.png'], waitFor: '.workbench', prepare: prepareWorkbench },
  { route: '/agent-runs', file: 'trace-evidence.png', aliases: ['agent-run-trace.png'], waitFor: '.agent-trace-page', query: demo => `?generationRecordId=${demo.recordId}` },
  { route: '/knowledge', file: 'knowledge-base-rag.png', waitFor: '.knowledge-page' },
  { route: '/prompts', file: 'prompt-studio.png', waitFor: '.templates-page', prepare: preparePromptStudio },
  { route: '/reviews', file: 'human-review.png', aliases: ['human-review-trace-detail.png'], waitFor: '.human-review-page' },
]

function readOnlyShotArg() {
  const arg = process.argv.find(item => item.startsWith('--only='))
  return (arg ? arg.slice('--only='.length) : process.env.DEVFLOW_SCREENSHOT_ONLY || '').trim()
}

async function importPlaywright() {
  const requireFromFrontend = createRequire(path.join(frontendDir, 'package.json'))
  return requireFromFrontend('playwright')
}

async function api(pathname, options = {}) {
  const response = await fetch(`${apiUrl}${pathname}`, {
    headers: { 'content-type': 'application/json', ...(options.headers || {}) },
    ...options,
  })
  if (!response.ok) {
    throw new Error(`${options.method || 'GET'} ${pathname} failed with HTTP ${response.status}`)
  }
  const payload = await response.json()
  if (payload.code !== 0) {
    throw new Error(`${options.method || 'GET'} ${pathname} failed: ${payload.message}`)
  }
  return payload.data
}

async function waitForService(url, label) {
  const started = Date.now()
  while (Date.now() - started < 45000) {
    try {
      const response = await fetch(url)
      if (response.ok || response.status < 500) return
    } catch {
      // keep waiting
    }
    await new Promise(resolve => setTimeout(resolve, 750))
  }
  throw new Error(`${label} is not ready: ${url}`)
}

async function ensureDemoState() {
  const projects = await api('/projects')
  const project = projects.find(item => item.projectName === 'DevFlow Copilot') || projects[0]
  const projectId = project?.id
  if (!projectId) throw new Error('No project is available for portfolio screenshots')

  const documents = await api('/knowledge/documents')
  const existing = documents.find(item => item.title === 'Portfolio Evidence: Agent Workflow Boundary')
  if (!existing) {
    await api('/knowledge/documents', {
      method: 'POST',
      body: JSON.stringify({
        title: 'Portfolio Evidence: Agent Workflow Boundary',
        sourceType: 'portfolio-demo',
        sourceUri: 'docs/real-provider-verification.md',
        content: [
          'DevFlow Copilot defaults to local-rule so the portfolio demo remains stable without an API key.',
          'OpenAI-compatible provider calls require DEVFLOW_AI_API_KEY, DEVFLOW_AI_BASE_URL and DEVFLOW_AI_MODEL from environment variables.',
          'Generation Trace records prompt version, variables, provider, model, latency and status without storing secrets.',
          'Agent Run Trace is an explainable workflow audit log with steps, tool calls and human review, not a full autonomous multi-agent runtime.',
          'Knowledge Base currently uses keyword and simple similarity retrieval. Embedding fields are reserved for future vector search.'
        ].join('\n\n'),
        metadata: 'portfolio=true;safe-demo=true',
      }),
    })
  }

  const existingRuns = await api(`/agent-runs?projectId=${projectId}`).catch(() => [])
  const scenarios = [
    '整理 Trace Evidence 摘要',
    '解释 local-rule fallback 路径',
    '复核 README 的 Provider 选择',
    '校验知识引用注入',
    '对比 Tool Call 与 Prompt 输出',
  ]
  const missingRuns = Math.max(1, 5 - existingRuns.length)
  let result

  for (let index = 0; index < missingRuns; index += 1) {
    result = await api('/ai/requirement-split', {
      method: 'POST',
      body: JSON.stringify({
        projectId,
        input: `${scenarios[index]}：解释 local-rule、Generation Trace、Agent Run Trace、Knowledge Base 引用和 Human Review 的真实边界。`,
        extraContext: '截图证据收口任务，只生成 review-only Artifact，不自动改代码、不自动提交 Git。',
        knowledgeQuery: 'local-rule provider generation trace agent run human review knowledge base API key',
      }),
    })

    if (result.recordId) {
      await api(`/generations/${result.recordId}/save`, { method: 'POST' })
      await api(`/generations/${result.recordId}/confirm`, { method: 'POST' })
    }
  }

  if (!result) {
    const latestRun = [...existingRuns].sort((a, b) => (b.id || 0) - (a.id || 0))[0]
    result = { recordId: latestRun?.generationRecordId, agentRunId: latestRun?.id }
  }

  return { projectId, recordId: result.recordId, agentRunId: result.agentRunId }
}

async function verifyFrontendApiProxy() {
  const response = await fetch(`${baseUrl.replace(/\/$/, '')}/api/dashboard/stats`)
  if (!response.ok) {
    throw new Error(`Frontend /api proxy failed with HTTP ${response.status}. Start Vite with VITE_API_PROXY_TARGET=${apiUrl.replace(/\/api$/, '')}`)
  }
  const payload = await response.json()
  if (payload.code !== 0 || !payload.data?.agentRunCount || !payload.data?.promptTemplateCount) {
    throw new Error(`Frontend /api proxy does not expose the prepared demo data. Start Vite with VITE_API_PROXY_TARGET=${apiUrl.replace(/\/api$/, '')}`)
  }
}

async function preparePage(page) {
  await page.addStyleTag({
    content: `
      * { caret-color: transparent !important; }
      .el-message { display: none !important; }
      .trace-node.running { animation: none !important; }
    `,
  })
}

async function prepareWorkbench(page) {
  const editor = page.locator('.task-editor textarea, .prompt-editor textarea').first()
  if (await editor.count()) {
    await editor.fill('为 DevFlow Copilot 生成一份可面试解释的 Agentic Coding Workflow Artifact，要求引用 Knowledge Base 并停在人工 Review。')
  }
  const runButton = page.locator('button').filter({ hasText: /运行|Workflow/ }).last()
  if (await runButton.count()) {
    await runButton.click()
    await page.waitForSelector('.result-card .code-block, .markdown-body.output', { timeout: 45000 })
  }
}

async function preparePromptStudio(page) {
  const testButton = page.locator('button').filter({ hasText: /运行测试|试运行|运行模板|Test/ }).first()
  if (await testButton.count()) {
    await testButton.click()
    await page.waitForSelector('.test-result', { timeout: 45000 }).catch(() => undefined)
  }
}

async function capture() {
  await waitForService(`${apiUrl}/dashboard/stats`, 'Backend')
  await waitForService(baseUrl, 'Frontend')

  const demo = await ensureDemoState()
  await verifyFrontendApiProxy()
  const { chromium } = await importPlaywright()
  await mkdir(imageDir, { recursive: true })

  let browser
  try {
    browser = await chromium.launch({ channel: 'chrome', headless: true, args: ['--hide-scrollbars'] })
  } catch {
    browser = await chromium.launch({ headless: true, args: ['--hide-scrollbars'] })
  }
  const page = await browser.newPage({ viewport, deviceScaleFactor: 1 })

  try {
    const selectedShots = onlyShot
      ? shots.filter(shot => shot.file === onlyShot || shot.file.replace(/\.png$/, '') === onlyShot)
      : shots
    if (!selectedShots.length) {
      throw new Error(`No screenshot target matched --only=${onlyShot}`)
    }

    for (const shot of selectedShots) {
      const query = shot.query ? shot.query(demo) : ''
      await page.goto(`${baseUrl}${shot.route}${query}`, { waitUntil: 'networkidle' })
      await page.waitForSelector(shot.waitFor, { timeout: 45000 })
      await preparePage(page)
      if (shot.prepare) await shot.prepare(page, demo)
      await page.evaluate(() => window.scrollTo(0, 0))
      await page.waitForTimeout(500)
      const file = path.join(imageDir, shot.file)
      const screenshot = await page.screenshot({ path: file, fullPage: false, animations: 'disabled' })
      const outputFiles = [file]
      for (const alias of onlyShot ? [] : shot.aliases || []) {
        const aliasFile = path.join(imageDir, alias)
        await writeFile(aliasFile, screenshot)
        outputFiles.push(aliasFile)
      }
      outputFiles.forEach(outputFile => console.log(path.relative(rootDir, outputFile)))
    }
  } finally {
    await browser.close()
  }
}

capture().catch(error => {
  console.error(error.message)
  process.exit(1)
})
