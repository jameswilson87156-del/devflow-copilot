import { mkdir, readFile } from 'node:fs/promises'
import { createRequire } from 'node:module'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const rootDir = path.resolve(__dirname, '..')
const frontendDir = path.join(rootDir, 'frontend')
const candidateDir = path.join(rootDir, 'docs', 'images', 'candidates', 'evidence-workspace-system-v1-2')
const previousDir = path.join(rootDir, 'docs', 'images', 'candidates', 'evidence-workspace-system-v1-1')
const baseUrl = process.env.DEVFLOW_EVIDENCE_REAL_URL || 'http://127.0.0.1:5181'
const generationRecordId = process.env.DEVFLOW_EVIDENCE_FREEZE_RECORD_ID || ''
const requireFromFrontend = createRequire(path.join(frontendDir, 'package.json'))
const { chromium } = requireFromFrontend('playwright')

async function ensureRealApi() {
  const response = await fetch(`${baseUrl}/api/projects`)
  if (!response.ok) throw new Error(`Real local API is unavailable at ${baseUrl}/api/projects`)
  const body = await response.json()
  if (!Array.isArray(body.data) || !body.data.length) throw new Error('Real local API returned no seeded projects')
}

async function assertNoOverflow(page, name) {
  const overflow = await page.evaluate(() => ({
    innerWidth: window.innerWidth,
    scrollWidth: document.documentElement.scrollWidth,
  }))
  if (overflow.scrollWidth > overflow.innerWidth) {
    throw new Error(`${name} has horizontal overflow: ${overflow.scrollWidth} > ${overflow.innerWidth}`)
  }
}

async function assertFormalCandidate(page, name) {
  const pageText = await page.locator('body').innerText()
  for (const forbidden of ['Development QA Fixture', '调试', '重构计划', 'CURRENT PAGE', 'Selected run evidence', 'Run Navigation']) {
    if (pageText.includes(forbidden)) throw new Error(`${name} contains forbidden text: ${forbidden}`)
  }
  await assertNoOverflow(page, name)
}

async function openRealPage(browser, viewport, name) {
  const page = await browser.newPage({ viewport, deviceScaleFactor: 1 })
  const query = generationRecordId ? `?generationRecordId=${encodeURIComponent(generationRecordId)}` : ''
  await page.goto(`${baseUrl}/agent-runs${query}`, { waitUntil: 'networkidle' })
  await page.waitForSelector('.evidence-event-row', { timeout: 30000 })
  await page.addStyleTag({ content: '* { caret-color: transparent !important; } .el-message { display: none !important; }' })
  await assertFormalCandidate(page, name)
  return page
}

async function screenshot(browser, viewport, name, setup) {
  const page = await openRealPage(browser, viewport, name)
  try {
    if (setup) await setup(page)
    await page.waitForTimeout(180)
    await assertFormalCandidate(page, name)
    await page.screenshot({ path: path.join(candidateDir, name), fullPage: false, animations: 'disabled' })
  } finally {
    await page.close()
  }
  console.log(path.relative(rootDir, path.join(candidateDir, name)))
}

async function createComparison(browser) {
  const before = await readFile(path.join(previousDir, 'trace-real-1440x900.png'))
  const after = await readFile(path.join(candidateDir, 'trace-freeze-1440x900.png'))
  const page = await browser.newPage({ viewport: { width: 1600, height: 620 }, deviceScaleFactor: 1 })
  await page.setContent(`<!doctype html><style>*{box-sizing:border-box}body{margin:0;padding:18px;background:#0a0f13;color:#edf3f1;font-family:"Microsoft YaHei UI",sans-serif}h1{margin:0;font-size:20px}p{margin:5px 0 14px;color:#8c999c;font-size:12px}main{display:grid;grid-template-columns:1fr 1fr;gap:14px}figure{margin:0;overflow:hidden;border:1px solid #26333a;border-radius:8px;background:#11191f}figcaption{height:34px;display:flex;align-items:center;padding:0 10px;border-bottom:1px solid #26333a;font-size:12px}img{display:block;width:100%;height:500px;object-fit:cover;object-position:top left}</style><h1>DevFlow · Trace System Freeze</h1><p>Phase 1.1 real local API / Phase 1.2 hardened freeze candidate</p><main><figure><figcaption>Phase 1.1</figcaption><img src="data:image/png;base64,${before.toString('base64')}"></figure><figure><figcaption>Phase 1.2 · TRACE_FRONTEND_FROZEN_FOR_PHASE2</figcaption><img src="data:image/png;base64,${after.toString('base64')}"></figure></main>`)
  await page.screenshot({ path: path.join(candidateDir, 'trace-phase1-1-vs-phase1-2.png'), fullPage: false, animations: 'disabled' })
  await page.close()
  console.log(path.relative(rootDir, path.join(candidateDir, 'trace-phase1-1-vs-phase1-2.png')))
}

async function main() {
  await mkdir(candidateDir, { recursive: true })
  await ensureRealApi()
  let browser
  try {
    browser = await chromium.launch({ channel: 'chrome', headless: true, args: ['--hide-scrollbars'] })
  } catch {
    browser = await chromium.launch({ headless: true, args: ['--hide-scrollbars'] })
  }

  try {
    await screenshot(browser, { width: 1440, height: 900 }, 'trace-freeze-1440x900.png')
    await screenshot(browser, { width: 1280, height: 800 }, 'trace-freeze-1280x800.png')
    await screenshot(browser, { width: 1024, height: 768 }, 'trace-tablet-closed-1024x768.png')
    await screenshot(browser, { width: 1024, height: 768 }, 'trace-tablet-open-1024x768.png', async (page) => {
      await page.locator('.mobile-inspector-trigger').click()
      await page.waitForSelector('.inspector-pane.open[role="dialog"]')
    })
    await screenshot(browser, { width: 390, height: 844 }, 'trace-mobile-closed-390x844.png')
    await screenshot(browser, { width: 390, height: 844 }, 'trace-mobile-run-selector-390x844.png', async (page) => {
      await page.locator('.mobile-run-selector').scrollIntoViewIfNeeded()
      const selector = page.locator('.mobile-run-selector select')
      if (await selector.count()) await selector.focus()
    })
    await screenshot(browser, { width: 390, height: 844 }, 'trace-mobile-inspector-390x844.png', async (page) => {
      await page.locator('.mobile-inspector-trigger').click()
      await page.waitForSelector('.inspector-pane.open[role="dialog"]')
    })
    await createComparison(browser)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
