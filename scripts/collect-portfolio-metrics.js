const fs = require('fs')
const path = require('path')
const { spawnSync } = require('child_process')

const rootDir = path.resolve(__dirname, '..')
const runChecks = process.argv.includes('--run-checks')
const now = new Date()

function rel(...parts) {
  return path.join(rootDir, ...parts)
}

function readText(filePath) {
  return fs.existsSync(filePath) ? fs.readFileSync(filePath, 'utf8') : ''
}

function listFiles(dirPath, predicate = () => true) {
  if (!fs.existsSync(dirPath)) return []
  return fs.readdirSync(dirPath, { withFileTypes: true })
    .filter(item => item.isFile())
    .map(item => item.name)
    .filter(predicate)
}

function listFilesRecursive(dirPath, predicate = () => true) {
  if (!fs.existsSync(dirPath)) return []
  const result = []
  for (const item of fs.readdirSync(dirPath, { withFileTypes: true })) {
    const current = path.join(dirPath, item.name)
    if (item.isDirectory()) result.push(...listFilesRecursive(current, predicate))
    if (item.isFile() && predicate(current)) result.push(current)
  }
  return result
}

function countMatches(text, regex) {
  return [...text.matchAll(regex)].length
}

function commandName(name) {
  return name
}

function runCommand(command, args, cwd, timeoutMs = 120000) {
  if (!runChecks) {
    return { status: '未执行', durationMs: null, exitCode: null, output: '未传入 --run-checks' }
  }
  const started = Date.now()
  const options = {
    cwd,
    encoding: 'utf8',
    timeout: timeoutMs,
    maxBuffer: 12 * 1024 * 1024,
  }
  const result = process.platform === 'win32'
    ? spawnSync([command, ...args].join(' '), { ...options, shell: true })
    : spawnSync(command, args, options)
  const durationMs = Date.now() - started
  const output = `${result.error?.message || ''}\n${result.stdout || ''}${result.stderr || ''}`.trim()
  return {
    status: result.status === 0 ? '通过' : '失败',
    durationMs,
    exitCode: result.status,
    output: output.slice(-2400),
  }
}

function parseInsertRowCount(sqlText, tableName) {
  const pattern = new RegExp(`INSERT\\s+INTO\\s+${tableName}\\s*\\([\\s\\S]*?\\)\\s+VALUES\\s*([\\s\\S]*?);`, 'i')
  const match = sqlText.match(pattern)
  if (!match) return 0
  return match[1]
    .split('\n')
    .map(line => line.trim())
    .filter(line => line.startsWith('('))
    .length
}

function formatBytes(bytes) {
  if (bytes === 0) return '0 B'
  if (!Number.isFinite(bytes)) return '当前未采集'
  const units = ['B', 'KB', 'MB', 'GB']
  let size = bytes
  let unit = 0
  while (size >= 1024 && unit < units.length - 1) {
    size /= 1024
    unit += 1
  }
  return `${size.toFixed(size >= 10 || unit === 0 ? 0 : 1)} ${units[unit]}`
}

async function measureApiLatency() {
  const url = process.env.DEVFLOW_METRICS_API_URL || 'http://127.0.0.1:8080/api/dashboard/stats'
  const started = Date.now()
  try {
    const response = await fetch(url, { signal: AbortSignal.timeout(3000) })
    const durationMs = Date.now() - started
    if (!response.ok) {
      return {
        url,
        status: `当前未采集 / 默认接口不可用（HTTP ${response.status}）`,
        durationMs: null,
      }
    }
    return {
      url,
      status: '已采集',
      durationMs,
    }
  } catch (error) {
    return {
      url,
      status: '当前未采集 / 本地服务未启动或不可访问',
      durationMs: null,
      error: error.message,
    }
  }
}

function summarizeBundle() {
  const assetsDir = rel('frontend', 'dist', 'assets')
  if (!runChecks) {
    return { status: '当前未采集 / 本次未执行 npm run build', jsFiles: 0, cssFiles: 0, jsBytes: null, cssBytes: null }
  }
  const files = listFiles(assetsDir)
  const jsFiles = files.filter(name => name.endsWith('.js'))
  const cssFiles = files.filter(name => name.endsWith('.css'))
  const sizeOf = names => names.reduce((sum, name) => sum + fs.statSync(path.join(assetsDir, name)).size, 0)
  if (!fs.existsSync(assetsDir)) {
    return { status: '当前未采集 / frontend/dist/assets 不存在', jsFiles: 0, cssFiles: 0, jsBytes: null, cssBytes: null }
  }
  return {
    status: '已采集',
    jsFiles: jsFiles.length,
    cssFiles: cssFiles.length,
    jsBytes: sizeOf(jsFiles),
    cssBytes: sizeOf(cssFiles),
  }
}

function scanSecretPatterns() {
  const candidateFiles = [
    'README.md',
    'TODO.md',
    'HANDOFF.md',
    'docs/env.example',
    'docs/real-provider-verification.md',
    'docs/deployment-production-demo.md',
    'backend/src/main/resources/application.yml',
    'backend/src/main/resources/application-dev.yml',
    'backend/src/main/resources/application-prod.yml',
  ]
  const patterns = [
    /sk-[A-Za-z0-9_-]{20,}/g,
    /AIza[0-9A-Za-z_-]{20,}/g,
    /xox[baprs]-[0-9A-Za-z-]{20,}/g,
    /(?<!your-)(api[_-]?key|token|secret)[ \t]*[:=][ \t]*['"]?(?!<|your-|demo|example|placeholder|$)[A-Za-z0-9_./+=-]{24,}/gi,
  ]
  const hits = []
  for (const file of candidateFiles) {
    const text = readText(rel(...file.split('/')))
    for (const pattern of patterns) {
      const matches = text.match(pattern) || []
      for (const match of matches) hits.push(`${file}: ${match.slice(0, 24)}...`)
    }
  }
  return hits
}

async function main() {
  const frontendPackage = JSON.parse(readText(rel('frontend', 'package.json')))
  const routerText = readText(rel('frontend', 'src', 'router', 'index.ts'))
  const readmeText = readText(rel('README.md'))
  const controllerFiles = listFiles(rel('backend', 'src', 'main', 'java', 'com', 'devflow', 'copilot', 'controller'), name => name.endsWith('.java'))
  const controllerTexts = controllerFiles.map(name => readText(rel('backend', 'src', 'main', 'java', 'com', 'devflow', 'copilot', 'controller', name)))
  const testFiles = listFilesRecursive(rel('backend', 'src', 'test', 'java'), file => file.endsWith('.java'))
  const testTexts = testFiles.map(readText)
  const migrationFiles = listFiles(rel('backend', 'src', 'main', 'resources', 'db', 'migration'), name => /^V\d+__.*\.sql$/.test(name))
  const migrationText = migrationFiles.map(name => readText(rel('backend', 'src', 'main', 'resources', 'db', 'migration', name))).join('\n')

  const frontendBuild = runCommand('npm', ['run', 'build'], rel('frontend'), 180000)
  const backendTest = runCommand('mvn', ['test'], rel('backend'), 240000)
  const apiLatency = await measureApiLatency()
  const bundle = summarizeBundle()
  const secretHits = scanSecretPatterns()

  const metrics = {
    branch: spawnSync(commandName('git'), ['branch', '--show-current'], { cwd: rootDir, encoding: 'utf8' }).stdout.trim(),
    viewFiles: listFiles(rel('frontend', 'src', 'views'), name => name.endsWith('.vue')).length,
    routeComponentCount: countMatches(routerText, /component:\s*[A-Za-z0-9_]+/g),
    redirectRouteCount: countMatches(routerText, /redirect:/g),
    disabledNavItems: countMatches(routerText, /disabled:\s*true/g),
    controllerCount: controllerFiles.length,
    endpointMappingCount: controllerTexts.reduce((sum, text) => sum + countMatches(text, /@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\b/g), 0),
    testFileCount: testFiles.length,
    testCount: testTexts.reduce((sum, text) => sum + countMatches(text, /@Test\b/g), 0),
    migrationCount: migrationFiles.length,
    seedInsertStatementCount: countMatches(migrationText, /INSERT\s+INTO\b/gi),
    promptTemplateSeedCount: parseInsertRowCount(migrationText, 'prompt_template'),
    generationRecordSeedCount: parseInsertRowCount(migrationText, 'generation_record'),
    knowledgeDocumentSeedCount: parseInsertRowCount(migrationText, 'knowledge_document'),
    knowledgeChunkSeedCount: parseInsertRowCount(migrationText, 'knowledge_chunk'),
    screenshotCount: listFiles(rel('docs', 'images'), name => name.endsWith('.png')).length,
    readmeImageCount: countMatches(readmeText, /!\[[^\]]*]\((?:\.\/)?docs\/images\/[^)]+\.png\)/g),
    workflowCount: listFiles(rel('.github', 'workflows'), name => name.endsWith('.yml') || name.endsWith('.yaml')).length,
    frontendBuild,
    backendTest,
    apiLatency,
    bundle,
    secretHits,
    nodeVersion: process.version,
    npmBuildScript: frontendPackage.scripts?.build || '未配置',
  }

  const lines = [
    '# DevFlow Copilot 作品集指标快照',
    '',
    `采集时间：${now.toISOString()}`,
    `采集命令：\`node scripts/collect-portfolio-metrics.js${runChecks ? ' --run-checks' : ''}\``,
    `当前分支：\`${metrics.branch}\``,
    '',
    '## 功能规模',
    '',
    `- 前端页面文件：${metrics.viewFiles} 个`,
    `- 前端真实 component route：${metrics.routeComponentCount} 个；redirect route：${metrics.redirectRouteCount} 个；disabled nav item：${metrics.disabledNavItems} 个`,
    `- 后端 Controller：${metrics.controllerCount} 个`,
    `- 后端 endpoint mapping：${metrics.endpointMappingCount} 个`,
    `- Flyway migration：${metrics.migrationCount} 个`,
    `- SQL seed insert statement：${metrics.seedInsertStatementCount} 条`,
    `- Prompt 模板 seed：${metrics.promptTemplateSeedCount} 条`,
    `- Generation Record seed：${metrics.generationRecordSeedCount} 条`,
    `- Knowledge Document seed：${metrics.knowledgeDocumentSeedCount} 条；Knowledge Chunk seed：${metrics.knowledgeChunkSeedCount} 条`,
    `- docs/images 截图文件：${metrics.screenshotCount} 张`,
    `- README 顶部 / 正文图片引用：${metrics.readmeImageCount} 张`,
    '',
    '## 工程质量',
    '',
    `- 后端测试文件：${metrics.testFileCount} 个`,
    `- 后端测试源码中的 \`@Test\`：${metrics.testCount} 个`,
    `- GitHub Actions workflow：${metrics.workflowCount} 个`,
    `- 前端 build 脚本：\`${metrics.npmBuildScript}\``,
    `- \`npm run build\`：${metrics.frontendBuild.status}${metrics.frontendBuild.durationMs === null ? '' : `，耗时 ${metrics.frontendBuild.durationMs}ms`}`,
    `- \`mvn test\`：${metrics.backendTest.status}${metrics.backendTest.durationMs === null ? '' : `，耗时 ${metrics.backendTest.durationMs}ms`}`,
    '',
    '## 性能体验',
    '',
    `- Node.js 版本：${metrics.nodeVersion}`,
    `- 前端 bundle 统计：${metrics.bundle.status}`,
    `- JS assets：${metrics.bundle.jsFiles} 个，${formatBytes(metrics.bundle.jsBytes)}`,
    `- CSS assets：${metrics.bundle.cssFiles} 个，${formatBytes(metrics.bundle.cssBytes)}`,
    `- 关键接口响应：${metrics.apiLatency.status}`,
    `- 关键接口 URL：\`${metrics.apiLatency.url}\``,
    `- 关键接口单次耗时：${metrics.apiLatency.durationMs === null ? '当前未采集' : `${metrics.apiLatency.durationMs}ms`}`,
    '',
    '## AI 工作流证据',
    '',
    '- Prompt -> Provider -> Result -> Generation Trace -> Agent Run Trace -> Tool Call -> Human Review 的闭环有实体、表、接口和测试覆盖。',
    '- 当前默认 Provider 是 `local-rule`，不代表真实 LLM 推理。',
    '- OpenAI-compatible Provider 为代码层适配，真实调用必须通过环境变量配置 Key。',
    '- Knowledge Base 当前是关键词 / 简单相似度检索，不是向量数据库。',
    '',
    '## 敏感信息扫描',
    '',
    secretHits.length
      ? `- 发现疑似敏感模式，需人工复核：${secretHits.join('；')}`
      : '- 基础模式扫描未发现真实 API Key / token / secret 形态。注意：这不是完整安全审计。',
    '',
    '## 可写入简历的数据',
    '',
    `- ${metrics.viewFiles} 个 Vue 页面文件、${metrics.routeComponentCount} 个真实前端页面路由。`,
    `- ${metrics.endpointMappingCount} 个后端 endpoint mapping，覆盖 AI 生成、Trace、Knowledge Base、Prompt、History、Review 等模块。`,
    `- ${metrics.testCount} 个后端自动化测试源码；若本快照显示 \`mvn test\` 通过，可写最近一次本地测试通过。`,
    `- ${metrics.screenshotCount} 张本地截图文件，其中 README 引用 ${metrics.readmeImageCount} 张真实页面截图。`,
    '',
    '## 暂时不能写的数据',
    '',
    '- 不能写真实线上用户、商业收益、生产请求量或 SLA。',
    '- 不能写未采集的 Lighthouse 分数或首屏性能分数。',
    '- 不能把 local-rule 写成真实 LLM 推理。',
    '- 不能把 demo seed / mock / UI-only 字段写成生产数据。',
    '- 不能把 Docker Compose runtime 写成已完整部署成功，除非重新完成 `docker compose up --build` 和 smoke test。',
    '',
  ]

  const metricsDir = rel('docs', 'metrics')
  fs.mkdirSync(metricsDir, { recursive: true })
  fs.writeFileSync(path.join(metricsDir, 'metrics_snapshot.md'), `\uFEFF${lines.join('\n')}`, 'utf8')

  console.log(`Wrote ${path.relative(rootDir, path.join(metricsDir, 'metrics_snapshot.md'))}`)
  console.log(`npm run build: ${metrics.frontendBuild.status}`)
  console.log(`mvn test: ${metrics.backendTest.status}`)
}

main().catch(error => {
  console.error(error)
  process.exit(1)
})
