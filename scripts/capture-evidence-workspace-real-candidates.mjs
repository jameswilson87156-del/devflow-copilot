import { copyFile, mkdir, readFile } from 'node:fs/promises'
import { createRequire } from 'node:module'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const rootDir = path.resolve(__dirname, '..')
const frontendDir = path.join(rootDir, 'frontend')
const candidateDir = path.join(rootDir, 'docs', 'images', 'candidates', 'evidence-workspace-system-v1-1')
const baseUrl = process.env.DEVFLOW_EVIDENCE_REAL_URL || 'http://127.0.0.1:5181'
const requireFromFrontend = createRequire(path.join(frontendDir, 'package.json'))
const { chromium } = requireFromFrontend('playwright')

async function ensureRealApi() {
  const response = await fetch(`${baseUrl}/api/projects`)
  if (!response.ok) throw new Error(`Real local API is unavailable at ${baseUrl}/api/projects`)
  const body = await response.json()
  if (!Array.isArray(body.data) || !body.data.length) throw new Error('Real local API returned no seeded projects')
}

async function openRealPage(browser, viewport) {
  const page = await browser.newPage({ viewport, deviceScaleFactor: 1 })
  await page.goto(`${baseUrl}/agent-runs`, { waitUntil: 'networkidle' })
  await page.waitForSelector('.evidence-event-row', { timeout: 30000 })
  const pageText = await page.locator('body').innerText()
  for (const forbidden of ['Development QA Fixture', '重构计划']) {
    if (pageText.includes(forbidden)) throw new Error(`Real candidate contains forbidden text: ${forbidden}`)
  }
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth)
  if (overflow) throw new Error(`${viewport.width}px candidate has horizontal overflow`)
  await page.addStyleTag({ content: '* { caret-color: transparent !important; } .el-message { display: none !important; }' })
  return page
}

async function screenshot(browser, viewport, name, openInspector = false) {
  const page = await openRealPage(browser, viewport)
  try {
    if (openInspector) {
      await page.locator('.mobile-inspector-trigger').click()
      await page.waitForSelector('.inspector-pane.open')
    } else if (await page.locator('.inspector-pane.open').count() && viewport.width <= 1180) {
      throw new Error(`${name} must capture the inspector closed state`)
    }
    await page.screenshot({ path: path.join(candidateDir, name), fullPage: false, animations: 'disabled' })
  } finally { await page.close() }
  console.log(path.relative(rootDir, path.join(candidateDir, name)))
}

async function comparison(browser) {
  const before = await readFile(path.join(rootDir, 'docs', 'images', 'trace-evidence.png'))
  const after = await readFile(path.join(candidateDir, 'trace-real-1440x900.png'))
  const page = await browser.newPage({ viewport: { width: 1600, height: 620 }, deviceScaleFactor: 1 })
  await page.setContent(`<!doctype html><style>*{box-sizing:border-box}body{margin:0;padding:18px;background:#0a0f13;color:#edf3f1;font-family:"Microsoft YaHei UI",sans-serif}h1{margin:0;font-size:20px}p{margin:5px 0 14px;color:#8c999c;font-size:11px}main{display:grid;grid-template-columns:1fr 1fr;gap:14px}figure{margin:0;overflow:hidden;border:1px solid #26333a;border-radius:8px;background:#11191f}figcaption{height:34px;display:flex;align-items:center;padding:0 10px;border-bottom:1px solid #26333a;font-size:12px}img{display:block;width:100%;height:500px;object-fit:cover;object-position:top left}</style><h1>DevFlow · Trace Visual Convergence</h1><p>Phase 1 baseline / Phase 1.1 real local API candidate</p><main><figure><figcaption>Phase 1 baseline</figcaption><img src="data:image/png;base64,${before.toString('base64')}"></figure><figure><figcaption>Phase 1.1 · real local API</figcaption><img src="data:image/png;base64,${after.toString('base64')}"></figure></main>`)
  await page.screenshot({ path: path.join(candidateDir, 'trace-before-phase1-after-phase1-1.png'), fullPage: false, animations: 'disabled' })
  await page.close()
  console.log(path.relative(rootDir, path.join(candidateDir, 'trace-before-phase1-after-phase1-1.png')))
}

async function main() {
  await mkdir(candidateDir, { recursive: true })
  await ensureRealApi()
  let browser
  try { browser = await chromium.launch({ channel: 'chrome', headless: true, args: ['--hide-scrollbars'] }) } catch { browser = await chromium.launch({ headless: true, args: ['--hide-scrollbars'] }) }
  try {
    await screenshot(browser, { width: 1440, height: 900 }, 'trace-real-1440x900.png')
    await screenshot(browser, { width: 1280, height: 800 }, 'trace-real-1280x800.png')
    await screenshot(browser, { width: 1024, height: 768 }, 'trace-tablet-closed-1024x768.png')
    await screenshot(browser, { width: 1024, height: 768 }, 'trace-tablet-open-1024x768.png', true)
    await screenshot(browser, { width: 390, height: 844 }, 'trace-mobile-closed-390x844.png')
    await screenshot(browser, { width: 390, height: 844 }, 'trace-mobile-open-390x844.png', true)
    await comparison(browser)
  } finally { await browser.close() }
}
main().catch((error) => { console.error(error); process.exit(1) })
