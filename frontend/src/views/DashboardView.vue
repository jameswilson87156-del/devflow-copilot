<script setup lang="ts">
import { computed, onMounted, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Plus } from '@element-plus/icons-vue'
import { fetchDashboardStats, fetchLogHistory, fetchPrompts } from '@/api/devflow'
import type { DashboardStats, GenerationRecord, LogAnalysis, PromptTemplate } from '@/types/domain'

const router = useRouter()
const loading = shallowRef(false)
const stats = shallowRef<DashboardStats>({
  projectCount: 0,
  todayGenerationCount: 0,
  logAnalysisCount: 0,
  promptTemplateCount: 0,
  agentRunCount: 0,
  humanReviewCount: 0,
  successCount: 0,
  successRate: 0,
  averageLatencyMs: 0,
  recentGenerations: [],
})
const promptTemplates = shallowRef<PromptTemplate[]>([])
const logHistory = shallowRef<LogAnalysis[]>([])

const recentRecords = computed(() => stats.value.recentGenerations)
const reviewQueue = computed(() => recentRecords.value.filter(isReviewRecord))
const confirmedCount = computed(() => recentRecords.value.filter((record) => record.confirmed || normalizeStatus(record.status) === 'CONFIRMED').length)
const savedCount = computed(() => recentRecords.value.filter((record) => normalizeStatus(record.status) === 'SAVED').length)
const failedCount = computed(() => recentRecords.value.filter((record) => normalizeStatus(record.status) === 'FAILED').length)
const enabledPromptCount = computed(() => promptTemplates.value.filter((template) => template.enabled).length)
const disabledPromptCount = computed(() => Math.max(promptTemplates.value.length - enabledPromptCount.value, 0))

const metricItems = computed(() => [
  { label: '今日运行数', value: stats.value.todayGenerationCount, code: 'Runs', tone: 'accent' },
  { label: '成功率', value: `${stats.value.successRate || 0}%`, code: 'Success', tone: 'accent' },
  { label: '人工确认数', value: stats.value.humanReviewCount || reviewQueue.value.length, code: 'Review', tone: 'warning' },
  { label: '平均耗时', value: `${stats.value.averageLatencyMs || 0}ms`, code: 'Latency', tone: 'info' },
  { label: 'Agent Run 数', value: stats.value.agentRunCount || 0, code: 'Agent Run', tone: 'accent' },
])

const workflowCards = [
  { step: '01', title: '项目上下文', desc: '读取技术栈、目录、约束和当前需求，避免脱离真实工程。', meta: 'Project Context' },
  { step: '02', title: 'Prompt 编排', desc: '按任务类型选择模板，渲染变量并保留版本。', meta: 'Prompt Template' },
  { step: '03', title: 'Artifact 生成', desc: '输出需求拆解、代码计划、README、Commit Message 或修复 Prompt。', meta: 'AI Artifact' },
  { step: '04', title: '人工 Review', desc: '生成结果必须进入待确认状态，不能绕过人工审查。', meta: 'Human Review' },
  { step: '05', title: '历史沉淀', desc: '记录 provider、model、耗时、模板版本和确认状态，方便复盘。', meta: 'Trace Ledger' },
]

const typeDistribution = computed(() => {
  const typeOrder = ['requirement-split', 'code-plan', 'log-analysis', 'fix-prompt', 'readme-generate', 'commit-message']
  const total = Math.max(recentRecords.value.length, 1)
  return typeOrder
    .map((type) => {
      const count = recentRecords.value.filter((record) => record.generationType === type).length
      return { type, label: formatType(type), count, percent: Math.round((count / total) * 100) }
    })
    .filter((item) => item.count > 0)
})

const promptStatusItems = computed(() => [
  { label: '已启用模板', value: enabledPromptCount.value, tone: 'accent' },
  { label: '停用模板', value: disabledPromptCount.value, tone: 'muted' },
  { label: '默认模板', value: promptTemplates.value.filter((template) => template.isDefault).length, tone: 'warning' },
])

const artifactRecords = computed(() => recentRecords.value.slice(0, 6))
const traceRecords = computed(() => recentRecords.value.slice(0, 5))
const latestLogs = computed(() => logHistory.value.slice(0, 3))

function normalizeStatus(status: string) {
  const map: Record<string, string> = {
    Draft: 'DRAFT',
    Generating: 'GENERATING',
    'Ready for Review': 'READY_FOR_REVIEW',
    Saved: 'SAVED',
    Confirmed: 'CONFIRMED',
    Failed: 'FAILED',
  }
  return map[status] || status
}

function isReviewRecord(record: GenerationRecord) {
  return normalizeStatus(record.status) === 'READY_FOR_REVIEW'
}

function statusClass(record: GenerationRecord) {
  const status = normalizeStatus(record.status)
  if (record.confirmed || status === 'CONFIRMED') return 'confirmed'
  if (status === 'SAVED') return 'saved'
  if (status === 'FAILED') return 'failed'
  if (status === 'READY_FOR_REVIEW') return 'review'
  return 'running'
}

function statusText(record: GenerationRecord) {
  if (record.confirmed || normalizeStatus(record.status) === 'CONFIRMED') return '已确认'
  const map: Record<string, string> = {
    DRAFT: '草稿',
    GENERATING: '正在生成',
    READY_FOR_REVIEW: '待人工确认',
    SAVED: '已保存',
    FAILED: '生成失败',
  }
  return map[normalizeStatus(record.status)] || record.status
}

function formatType(type: string) {
  const map: Record<string, string> = {
    'requirement-split': '需求拆解',
    'code-plan': '代码计划',
    'readme-generate': 'README',
    'commit-message': 'Commit Message',
    'fix-prompt': '修复 Prompt',
    'log-analysis': '日志分析',
  }
  return map[type] || type
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '--'
}

function openRecord(record: GenerationRecord) {
  router.push({ path: '/history', query: { id: record.id } })
}

async function loadDashboard() {
  loading.value = true
  try {
    const [statData, promptData, logData] = await Promise.all([
      fetchDashboardStats(),
      fetchPrompts(),
      fetchLogHistory(),
    ])
    stats.value = statData
    promptTemplates.value = promptData
    logHistory.value = logData
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<template>
  <div class="dashboard command-center" v-loading="loading">
    <header class="command-header">
      <div class="command-title">
        <span class="section-code mono">DEVFLOW / AI CODING COMMAND CENTER</span>
        <h2>AI 编码工作流总控台</h2>
        <p>项目上下文 → Prompt 模板 → Knowledge 检索 → Provider 生成 → Agent Trace → 人工 Review。</p>
      </div>
      <button class="primary-action" type="button" @click="router.push('/workbench')">
        <el-icon><Plus /></el-icon>
        新建工作流
      </button>
    </header>

    <section class="status-strip" aria-label="工作流状态">
      <article v-for="item in metricItems" :key="item.code" class="status-cell" :data-tone="item.tone">
        <span class="mono">{{ item.code }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.label }}</small>
      </article>
    </section>

    <section class="workflow-spine" aria-label="AI 编码主流程">
      <article v-for="card in workflowCards" :key="card.step" class="workflow-card">
        <span class="workflow-index mono">{{ card.step }}</span>
        <div>
          <strong>{{ card.title }}</strong>
          <p>{{ card.desc }}</p>
          <small class="mono">{{ card.meta }}</small>
        </div>
      </article>
    </section>

    <main class="overview-grid">
      <section class="workspace-pane activity-pane" aria-labelledby="activity-title">
        <header class="pane-header">
          <div class="pane-title">
            <span class="pane-mark" aria-hidden="true"></span>
            <div>
              <h3 id="activity-title">工作流活动流</h3>
              <p class="mono">{{ recentRecords.length }} 条最近生成 Trace</p>
            </div>
          </div>
          <button class="text-action" type="button" @click="router.push('/history')">
            查看生成历史
            <el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <div class="activity-table-head" aria-hidden="true">
          <span>Trace</span>
          <span>Artifact</span>
          <span>类型</span>
          <span>Review</span>
          <span>耗时</span>
        </div>

        <div class="activity-list">
          <div v-if="!recentRecords.length" class="empty-state">暂无生成记录，请先在 AI 工作台运行工作流。</div>
          <button
            v-for="(record, index) in recentRecords"
            :key="record.id"
            class="activity-row"
            type="button"
            @click="openRecord(record)"
          >
            <span class="trace-cell" :class="{ last: index === recentRecords.length - 1 }">
              <span class="trace-node" :class="statusClass(record)"></span>
            </span>
            <span class="activity-main">
              <strong>{{ record.inputSummary }}</strong>
              <small class="mono">#{{ record.id }} · {{ record.providerName || 'local-rule' }} · {{ formatTime(record.createdAt) }}</small>
            </span>
            <span class="type-chip">{{ formatType(record.generationType) }}</span>
            <span class="record-status" :class="statusClass(record)">
              <i aria-hidden="true"></i>{{ statusText(record) }}
            </span>
            <span class="cost mono">{{ record.costTimeMs }}ms</span>
          </button>
        </div>
      </section>

      <aside class="side-stack">
        <section class="workspace-pane review-pane" aria-labelledby="review-title">
          <header class="pane-header compact">
            <div class="pane-title">
              <span class="pane-mark amber" aria-hidden="true"></span>
              <div>
                <h3 id="review-title">待人工确认队列</h3>
                <p class="mono">{{ reviewQueue.length }} WAITING REVIEW</p>
              </div>
            </div>
          </header>
          <div class="compact-list">
            <div v-if="!reviewQueue.length" class="mini-empty">当前没有待确认 Artifact</div>
            <button v-for="record in reviewQueue.slice(0, 4)" :key="record.id" type="button" @click="openRecord(record)">
              <strong>{{ record.inputSummary }}</strong>
              <span class="mono">{{ formatType(record.generationType) }} · {{ record.costTimeMs }}ms</span>
            </button>
          </div>
        </section>

        <section class="workspace-pane trace-pane" aria-labelledby="trace-title">
          <header class="pane-header compact">
            <div class="pane-title">
              <span class="pane-mark" aria-hidden="true"></span>
          <div>
            <h3 id="trace-title">最近 Agent Trace</h3>
            <p class="mono">Provider / Model / Status</p>
          </div>
        </div>
      </header>
      <div class="trace-list">
            <button v-for="record in traceRecords" :key="record.id" type="button" @click="router.push({ path: '/agent-runs', query: { generationRecordId: record.id } })">
              <span class="trace-node" :class="statusClass(record)" aria-hidden="true"></span>
              <span>
                <strong>{{ record.modelName }}</strong>
                <small>{{ record.providerName || 'local-rule' }} · {{ statusText(record) }}</small>
              </span>
            </button>
          </div>
        </section>

        <section class="workspace-pane log-pane" aria-labelledby="log-title">
          <header class="pane-header compact">
            <div class="pane-title">
              <span class="pane-mark red" aria-hidden="true"></span>
              <div>
                <h3 id="log-title">最近日志诊断</h3>
                <p class="mono">Spring Boot Root Cause</p>
              </div>
            </div>
          </header>
          <div class="log-list">
            <div v-if="!latestLogs.length" class="mini-empty">暂无日志诊断记录</div>
            <article v-for="item in latestLogs" :key="item.id">
              <strong>{{ item.exceptionType }}</strong>
              <span>{{ item.riskLevel }} · {{ formatTime(item.createdAt) }}</span>
            </article>
          </div>
        </section>
      </aside>
    </main>

    <section class="insight-grid">
      <article class="workspace-pane insight-card">
        <header class="insight-header">
          <div>
            <h3>生成类型分布</h3>
            <p class="mono">Artifact Type Mix</p>
          </div>
        </header>
        <div class="distribution-list">
          <div v-if="!typeDistribution.length" class="mini-empty">暂无类型分布</div>
          <div v-for="item in typeDistribution" :key="item.type" class="distribution-row">
            <span>{{ item.label }}</span>
            <div class="meter"><i :style="{ width: `${item.percent}%` }"></i></div>
            <strong class="mono">{{ item.count }}</strong>
          </div>
        </div>
      </article>

      <article class="workspace-pane insight-card">
        <header class="insight-header">
          <div>
            <h3>Prompt 模板启用状态</h3>
            <p class="mono">Reusable Prompt Orchestration</p>
          </div>
        </header>
        <div class="prompt-status-grid">
          <div v-for="item in promptStatusItems" :key="item.label" :data-tone="item.tone">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
          </div>
        </div>
      </article>

      <article class="workspace-pane insight-card wide">
        <header class="insight-header">
          <div>
            <h3>最近 Artifact 记录</h3>
            <p class="mono">Reviewable Outputs</p>
          </div>
          <span class="mono summary-note">已确认 {{ confirmedCount }} · 已保存 {{ savedCount }} · 失败 {{ failedCount }}</span>
        </header>
        <div class="artifact-grid">
          <button v-for="record in artifactRecords" :key="record.id" type="button" @click="openRecord(record)">
            <span class="trace-node" :class="statusClass(record)" aria-hidden="true"></span>
            <strong>{{ record.inputSummary }}</strong>
            <small>{{ formatType(record.generationType) }} · {{ statusText(record) }}</small>
          </button>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.command-center {
  display: grid;
  gap: 12px;
  width: 100%;
  min-width: 0;
}

.command-header {
  min-width: 0;
  min-height: 64px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: var(--border-subtle);
}

.command-title {
  min-width: 0;
}

.section-code {
  color: var(--color-accent);
  font-size: 10px;
  letter-spacing: 0.08em;
}

.command-title h2 {
  margin: 4px 0 0;
  font-size: 22px;
  font-weight: 650;
  line-height: 1.22;
  letter-spacing: -0.03em;
}

.command-title p {
  margin: 7px 0 0;
  color: var(--color-text-secondary);
  font-size: 12px;
  line-height: 18px;
}

.status-strip {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
  min-width: 0;
}

.status-cell {
  min-width: 0;
  padding: 10px 11px;
  border: var(--border-default);
  border-radius: var(--radius-md);
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.025), transparent 44%),
    var(--color-surface);
}

.status-cell span,
.status-cell strong,
.status-cell small {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-cell span {
  color: var(--color-text-disabled);
  font-size: 9px;
  letter-spacing: 0.07em;
}

.status-cell strong {
  margin-top: 7px;
  color: var(--color-text-primary);
  font-family: var(--font-mono);
  font-size: 22px;
  line-height: 1;
}

.status-cell small {
  margin-top: 5px;
  color: var(--color-text-secondary);
  font-size: 11px;
}

.status-cell[data-tone="warning"] strong {
  color: var(--color-warning);
}

.status-cell[data-tone="info"] strong {
  color: var(--color-info);
}

.workflow-spine {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  min-width: 0;
  border: var(--border-default);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  overflow: hidden;
}

.workflow-card {
  position: relative;
  min-width: 0;
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  gap: 8px;
  padding: 12px 10px;
  border-right: var(--border-subtle);
}

.workflow-card:last-child {
  border-right: 0;
}

.workflow-card:not(:last-child)::after {
  content: '';
  position: absolute;
  right: -4px;
  top: 50%;
  z-index: 1;
  width: 7px;
  height: 7px;
  border-top: var(--border-subtle);
  border-right: var(--border-subtle);
  background: var(--color-surface);
  transform: translateY(-50%) rotate(45deg);
}

.workflow-index {
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  border: 1px solid var(--color-accent-border);
  border-radius: 4px;
  background: var(--color-accent-muted);
  color: var(--color-accent);
  font-size: 10px;
}

.workflow-card strong,
.workflow-card p,
.workflow-card small {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.workflow-card strong {
  font-size: 13px;
  line-height: 18px;
  white-space: nowrap;
}

.workflow-card p {
  margin: 4px 0 0;
  height: 34px;
  color: var(--color-text-secondary);
  font-size: 11px;
  line-height: 17px;
}

.workflow-card small {
  margin-top: 8px;
  color: var(--color-text-disabled);
  font-size: 9px;
  white-space: nowrap;
}

.overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 360px);
  gap: 12px;
  align-items: start;
  min-width: 0;
}

.side-stack {
  min-width: 0;
  display: grid;
  gap: 12px;
}

.workspace-pane {
  min-width: 0;
  border: var(--border-default);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  overflow: hidden;
}

.pane-header {
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
  padding: 8px 12px;
  border-bottom: var(--border-subtle);
  background: var(--color-surface-raised);
}

.pane-header.compact {
  min-height: 44px;
}

.pane-title {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.pane-title > div {
  min-width: 0;
}

.pane-title h3 {
  margin: 0;
  overflow: hidden;
  font-size: 14px;
  line-height: 20px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pane-title p {
  margin: 0;
  overflow: hidden;
  color: var(--color-text-disabled);
  font-size: 9px;
  line-height: 14px;
  letter-spacing: 0.06em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pane-mark {
  width: 3px;
  height: 24px;
  flex: 0 0 auto;
  border-radius: 1px;
  background: var(--color-accent);
}

.pane-mark.amber {
  background: var(--color-warning);
}

.pane-mark.red {
  background: var(--color-error);
}

.primary-action,
.text-action {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  height: 30px;
  border-radius: 5px;
  cursor: pointer;
  font-size: 12px;
  white-space: nowrap;
  transition: background-color 120ms ease, border-color 120ms ease, color 120ms ease;
}

.primary-action {
  padding: 0 12px;
  border: 1px solid var(--color-accent);
  background: var(--color-accent);
  color: #0f1714;
  font-weight: 650;
}

.primary-action:hover {
  border-color: var(--color-accent-hover);
  background: var(--color-accent-hover);
}

.text-action {
  padding: 0 8px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--color-text-secondary);
}

.text-action:hover {
  border-color: var(--color-border);
  background: var(--color-active-row);
  color: var(--color-text-primary);
}

.activity-table-head {
  height: 30px;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) 104px 100px 64px;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  border-bottom: var(--border-subtle);
  color: var(--color-text-disabled);
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.06em;
}

.activity-list {
  min-width: 0;
}

.activity-row {
  width: 100%;
  min-width: 0;
  min-height: 60px;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) 104px 100px 64px;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  border: 0;
  border-bottom: var(--border-subtle);
  border-radius: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.activity-row:last-child {
  border-bottom: 0;
}

.activity-row:hover,
.compact-list button:hover,
.trace-list button:hover,
.artifact-grid button:hover {
  background: var(--color-active-row);
}

.trace-cell {
  position: relative;
  align-self: stretch;
  display: grid;
  place-items: center;
  min-width: 0;
}

.trace-cell::after {
  content: '';
  position: absolute;
  top: calc(50% + 5px);
  bottom: -50%;
  left: 50%;
  width: 1px;
  background: var(--color-border-subtle);
}

.trace-cell.last::after {
  display: none;
}

.trace-node {
  position: relative;
  z-index: 1;
  width: 8px;
  height: 8px;
  flex: 0 0 auto;
  border: 1px solid var(--color-text-disabled);
  border-radius: 2px;
  background: var(--color-surface);
}

.trace-node.review {
  border-color: var(--color-warning);
  background: var(--color-warning);
}

.trace-node.confirmed {
  border-color: var(--color-accent);
  background: var(--color-accent);
}

.trace-node.saved {
  border-color: var(--color-info);
  background: var(--color-info);
}

.trace-node.failed {
  border-color: var(--color-error);
  background: var(--color-error);
}

.trace-node.running {
  border-color: var(--color-accent);
  background: var(--color-accent);
  animation: pulse 1.6s ease-in-out infinite;
}

.activity-main,
.activity-main strong,
.activity-main small {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-main strong {
  color: var(--color-text-primary);
  font-size: 12px;
  font-weight: 550;
  line-height: 18px;
}

.activity-main small {
  margin-top: 3px;
  color: var(--color-text-disabled);
  font-size: 10px;
  line-height: 15px;
}

.type-chip {
  min-width: 0;
  overflow: hidden;
  padding: 4px 7px;
  border: var(--border-subtle);
  border-radius: 4px;
  background: var(--color-bg);
  color: var(--color-text-secondary);
  font-size: 11px;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-status {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 5px;
  overflow: hidden;
  color: var(--color-text-secondary);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-status i {
  width: 5px;
  height: 5px;
  flex: 0 0 auto;
  border-radius: 1px;
  background: currentColor;
}

.record-status.review {
  color: var(--color-warning);
}

.record-status.confirmed {
  color: var(--color-accent);
}

.record-status.saved {
  color: var(--color-info);
}

.record-status.failed {
  color: var(--color-error);
}

.cost {
  min-width: 0;
  overflow: hidden;
  color: var(--color-text-secondary);
  font-size: 10px;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.compact-list,
.trace-list,
.log-list {
  display: grid;
}

.compact-list button,
.trace-list button {
  width: 100%;
  min-width: 0;
  min-height: 48px;
  border: 0;
  border-bottom: var(--border-subtle);
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.compact-list button {
  padding: 8px 11px;
}

.compact-list button:last-child,
.trace-list button:last-child {
  border-bottom: 0;
}

.compact-list strong,
.compact-list span,
.trace-list strong,
.trace-list small,
.log-list strong,
.log-list span {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.compact-list strong {
  font-size: 11px;
}

.compact-list span {
  margin-top: 4px;
  color: var(--color-text-disabled);
  font-size: 9px;
}

.trace-list button {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  padding: 8px 11px;
}

.trace-list strong {
  font-size: 11px;
}

.trace-list small {
  margin-top: 4px;
  color: var(--color-text-disabled);
  font-size: 9px;
}

.log-list article {
  min-width: 0;
  padding: 9px 11px;
  border-bottom: var(--border-subtle);
}

.log-list article:last-child {
  border-bottom: 0;
}

.log-list strong {
  font-size: 11px;
}

.log-list span {
  margin-top: 4px;
  color: var(--color-text-disabled);
  font-size: 9px;
}

.mini-empty {
  min-height: 52px;
  display: grid;
  place-items: center;
  color: var(--color-text-disabled);
  font-size: 11px;
}

.insight-grid {
  display: grid;
  grid-template-columns: minmax(220px, 0.8fr) minmax(220px, 0.8fr) minmax(0, 1.4fr);
  gap: 12px;
  min-width: 0;
}

.insight-card {
  min-height: 150px;
}

.insight-header {
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 12px;
  border-bottom: var(--border-subtle);
  background: var(--color-surface-raised);
}

.insight-header h3,
.insight-header p {
  margin: 0;
}

.insight-header h3 {
  font-size: 13px;
}

.insight-header p,
.summary-note {
  color: var(--color-text-disabled);
  font-size: 9px;
}

.distribution-list {
  display: grid;
  gap: 8px;
  padding: 12px;
}

.distribution-row {
  display: grid;
  grid-template-columns: 82px minmax(0, 1fr) 24px;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.distribution-row span,
.distribution-row strong {
  min-width: 0;
  overflow: hidden;
  color: var(--color-text-secondary);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.distribution-row strong {
  color: var(--color-text-primary);
  text-align: right;
}

.meter {
  height: 6px;
  overflow: hidden;
  border-radius: 2px;
  background: var(--color-bg);
}

.meter i {
  display: block;
  height: 100%;
  min-width: 4px;
  border-radius: inherit;
  background: var(--color-accent);
}

.prompt-status-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1px;
  padding: 1px;
  background: var(--color-border-subtle);
}

.prompt-status-grid div {
  min-width: 0;
  padding: 17px 10px;
  background: var(--color-surface);
  text-align: center;
}

.prompt-status-grid strong,
.prompt-status-grid span {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.prompt-status-grid strong {
  color: var(--color-accent);
  font-family: var(--font-mono);
  font-size: 22px;
}

.prompt-status-grid [data-tone="warning"] strong {
  color: var(--color-warning);
}

.prompt-status-grid [data-tone="muted"] strong {
  color: var(--color-info);
}

.prompt-status-grid span {
  margin-top: 7px;
  color: var(--color-text-secondary);
  font-size: 11px;
}

.artifact-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1px;
  padding: 1px;
  background: var(--color-border-subtle);
}

.artifact-grid button {
  min-width: 0;
  min-height: 62px;
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr);
  align-items: center;
  column-gap: 8px;
  padding: 9px;
  border: 0;
  background: var(--color-surface);
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.artifact-grid strong,
.artifact-grid small {
  grid-column: 2;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.artifact-grid strong {
  align-self: end;
  font-size: 11px;
}

.artifact-grid small {
  align-self: start;
  margin-top: 3px;
  color: var(--color-text-disabled);
  font-size: 9px;
}

.command-header > *,
.status-strip > *,
.workflow-spine > *,
.workflow-card > *,
.overview-grid > *,
.pane-header > *,
.pane-title > *,
.activity-table-head > *,
.activity-row > *,
.insight-grid > *,
.insight-header > *,
.artifact-grid > * {
  min-width: 0;
}

@media (max-width: 1280px) {
  .status-strip {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .workflow-spine {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .workflow-card {
    border-bottom: var(--border-subtle);
  }

  .overview-grid,
  .insight-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 980px) {
  .command-header {
    flex-direction: column;
  }

  .status-strip,
  .workflow-spine {
    grid-template-columns: minmax(0, 1fr);
  }

  .activity-table-head {
    display: none;
  }

  .activity-row {
    grid-template-columns: 28px minmax(0, 1fr);
    padding: 10px 12px;
  }

  .type-chip,
  .record-status,
  .cost {
    grid-column: 2;
    justify-content: flex-start;
    text-align: left;
  }

  .artifact-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (prefers-reduced-motion: reduce) {
  .trace-node.running {
    animation: none;
  }
}
</style>
