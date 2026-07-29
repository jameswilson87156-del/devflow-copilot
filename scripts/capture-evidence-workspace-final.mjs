import { mkdir } from 'node:fs/promises'
import { createRequire } from 'node:module'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const rootDir = path.resolve(__dirname, '..')
const frontendDir = path.join(rootDir, 'frontend')
const candidateDir = path.join(rootDir, 'docs', 'images', 'candidates', 'devflow-final')
const baseUrl = process.env.DEVFLOW_EVIDENCE_REAL_URL || 'http://127.0.0.1:5181'
const generationRecordId = process.env.DEVFLOW_EVIDENCE_FINAL_RECORD_ID || ''
const requireFromFrontend = createRequire(path.join(frontendDir, 'package.json'))
const { chromium } = requireFromFrontend('playwright')

async function assertFinalCandidate(page, name) {
  const text = await page.locator('body').innerText()
  const forbidden = ['Development QA Fixture', 'CURRENT PAGE', 'Selected run evidence', 'Run Navigation', 'Phase 1', '重构', '调试']
  for (const value of forbidden) {
    if (text.includes(value)) throw new Error(`${name} contains forbidden text: ${value}`)
  }
  if (/\?{4,}/.test(text)) throw new Error(`${name} contains four or more consecutive question marks`)
  const { scrollWidth, innerWidth } = await page.evaluate(() => ({ scrollWidth: document.documentElement.scrollWidth, innerWidth: window.innerWidth }))
  if (scrollWidth > innerWidth) throw new Error(`${name} has horizontal overflow: ${scrollWidth} > ${innerWidth}`)
}

async function openPage(browser, viewport, name) {
  const page = await browser.newPage({ viewport, deviceScaleFactor: 1 })
  const query = generationRecordId ? `?generationRecordId=${encodeURIComponent(generationRecordId)}` : ''
  await page.goto(`${baseUrl}/agent-runs${query}`, { waitUntil: 'networkidle' })
  await page.waitForSelector('.evidence-event-row', { timeout: 30000 })
  await page.addStyleTag({ content: '* { caret-color: transparent !important; } .el-message { display: none !important; }' })
  await assertFinalCandidate(page, name)
  return page
}

async function capture(browser, viewport, name) {
  const page = await openPage(browser, viewport, name)
  try {
    await page.screenshot({ path: path.join(candidateDir, name), fullPage: false, animations: 'disabled' })
    await assertFinalCandidate(page, name)
  } finally { await page.close() }
  console.log(path.relative(rootDir, path.join(candidateDir, name)))
}

async function main() {
  await mkdir(candidateDir, { recursive: true })
  const api = await fetch(`${baseUrl}/api/projects`)
  if (!api.ok) throw new Error(`Real local API is unavailable at ${baseUrl}/api/projects`)
  let browser
  try { browser = await chromium.launch({ channel: 'chrome', headless: true, args: ['--hide-scrollbars'] }) }
  catch { browser = await chromium.launch({ headless: true, args: ['--hide-scrollbars'] }) }
  try {
    await capture(browser, { width: 1440, height: 900 }, 'trace-final-1440x900.png')
    await capture(browser, { width: 1280, height: 800 }, 'trace-final-1280x800.png')
    await capture(browser, { width: 390, height: 844 }, 'trace-final-mobile-390x844.png')
  } finally { await browser.close() }
}

main().catch((error) => { console.error(error); process.exit(1) })
