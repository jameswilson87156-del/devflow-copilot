<script setup lang="ts">
import { computed, onMounted, shallowRef, type Component } from 'vue'
import { useRoute } from 'vue-router'
import {
  Check,
  CircleCheck,
  Coin,
  Connection,
  DataLine,
  DocumentCopy,
  Monitor,
  Reading,
  RefreshRight,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  fetchAgentRunTrace,
  fetchAgentRuns,
  fetchGenerationTraces,
  fetchGenerations,
  fetchKnowledgeReferences,
  fetchProjects,
} from '@/api/devflow'
import StatusBadge from '@/components/StatusBadge.vue'
import type {
  AgentRun,
  AgentRunTrace,
  AgentStep,
  GenerationRecord,
  GenerationTrace,
  HumanReview,
  KnowledgeReference,
  ProjectContext,
  ToolCallRecord,
} from '@/types/domain'

type EvidenceTab = 'json' | 'prompt' | 'fallback' | 'tools'
type Tone = 'success' | 'warning' | 'danger' | 'running' | 'muted'

interface TimelineStep {
  key: string
  order: number
  label: string
  description: string
  status: string
  statusLabel: string
  latencyMs?: number
  step?: AgentStep
  tone: Tone
}

interface MetricItem {
  label: string
  value: string
  icon: Component
  tone?: Tone
}

interface ReviewHistoryItem {
  time: string
  event: string
  actor: string
  tone: Tone
}

const projects = shallowRef<ProjectContext[]>([])
const runs = shallowRef<AgentRun[]>([])
const records = shallowRef<GenerationRecord[]>([])
const generationTraces = shallowRef<GenerationTrace[]>([])
const references = shallowRef<KnowledgeReference[]>([])
const selectedProjectId = shallowRef<number>()
const selectedRunId = shallowRef<number>()
const selectedStepKey = shallowRef('')
const trace = shallowRef<AgentRunTrace>()
const loading = shallowRef(false)
const activeEvidenceTab = shallowRef<EvidenceTab>('json')
const route = useRoute()

const selectedRun = computed(() => runs.value.find((run) => run.id === selectedRunId.value))
const visibleRuns = computed(() => runs.value.slice(0, 5))
const selectedRecord = computed(() => {
  const recordId = trace.value?.run.generationRecordId || selectedRun.value?.generationRecordId
  return records.value.find((record) => record.id === recordId)
})
const selectedGenerationTrace = computed(() => {
  const recordId = selectedRecord.value?.id
  return generationTraces.value.find((item) => item.generationRecordId === recordId) || generationTraces.value[0]
})
const toolCalls = computed(() => trace.value?.toolCalls || [])
const humanReview = computed<HumanReview | undefined>(() => trace.value?.humanReviews[0])
const selectedProvider = computed(() => {
  return trace.value?.run.providerName
    || selectedRecord.value?.providerName
    || selectedGenerationTrace.value?.providerName
    || 'local-rule'
})
const selectedModel = computed(() => {
  return trace.value?.run.modelName
    || selectedRecord.value?.modelName
    || selectedGenerationTrace.value?.modelName
    || 'gpt-4.1-mini'
})
const selectedTimelineStep = computed(() => {
  return timelineSteps.value.find((item) => item.key === selectedStepKey.value) || timelineSteps.value[0]
})
const selectedToolCalls = computed(() => {
  const stepId = selectedTimelineStep.value?.step?.id
  if (!stepId) return toolCalls.value
  const matched = toolCalls.value.filter((tool) => tool.stepId === stepId)
  return matched.length ? matched : toolCalls.value
})
const reviewStatus = computed(() => humanReview.value?.reviewStatus || selectedRecord.value?.status || 'PENDING')
const tokenUsage = computed(() => {
  if (typeof selectedRecord.value?.totalTokens === 'number') return selectedRecord.value.totalTokens
  const prompt = selectedRecord.value?.promptTokens || 0
  const completion = selectedRecord.value?.completionTokens || 0
  return prompt + completion || undefined
})

const timelineSteps = computed<TimelineStep[]>(() => {
  const actualSteps = [...(trace.value?.steps || [])].sort((a, b) => a.stepOrder - b.stepOrder)
  const byType = new Map(actualSteps.map((step) => [step.stepType, step]))
  const promptStep = byType.get('PROMPT_RENDER') || actualSteps[1] || actualSteps[0]
  const knowledgeStep = byType.get('KNOWLEDGE_RETRIEVAL')
  const providerStep = byType.get('LLM_GENERATION')
  const reviewStep = byType.get('HUMAN_REVIEW')
  const toolLatency = toolCalls.value.reduce((sum, tool) => sum + (tool.latencyMs || 0), 0)
  const firstToolCall = toolCalls.value[0]

  const rows: Array<Omit<TimelineStep, 'tone' | 'statusLabel'>> = [
    {
      key: promptStep ? `step-${promptStep.id}` : 'derived-prompt-render',
      order: 1,
      label: 'Prompt 渲染',
      description: promptStep?.summary || '将用户输入渲染为结构化 Prompt。',
      status: promptStep?.status || 'SUCCESS',
      latencyMs: promptStep?.latencyMs,
      step: promptStep,
    },
    {
      key: knowledgeStep ? `step-${knowledgeStep.id}` : 'derived-knowledge',
      order: 2,
      label: '知识命中',
      description: knowledgeStep?.summary || (references.value.length ? `检索并命中 ${references.value.length} 条知识引用。` : '本次运行未返回知识引用，保留为 Demo Data 空状态。'),
      status: knowledgeStep?.status || 'SUCCESS',
      latencyMs: knowledgeStep?.latencyMs,
      step: knowledgeStep,
    },
    {
      key: providerStep ? `step-${providerStep.id}` : 'derived-provider',
      order: 3,
      label: 'Provider 选择',
      description: `选择 ${selectedProvider.value} provider，模型：${selectedModel.value}。`,
      status: providerStep?.status || selectedRecord.value?.status || 'SUCCESS',
      latencyMs: providerStep?.latencyMs || selectedRecord.value?.costTimeMs,
      step: providerStep,
    },
    {
      key: 'derived-tool-call',
      order: 4,
      label: 'Tool Call 模拟',
      description: toolCalls.value.length ? `模拟 Tool Call，生成调用计划。` : '当前 Trace 未返回 Tool Call 明细。',
      status: toolCalls.value.length ? 'SUCCESS' : 'PENDING',
      latencyMs: toolLatency,
      step: firstToolCall ? actualSteps.find((step) => step.id === firstToolCall.stepId) : undefined,
    },
    {
      key: 'derived-record',
      order: 5,
      label: '生成记录',
      description: selectedRecord.value ? '生成运行记录，持久化证据。' : '当前运行未关联生成记录。',
      status: selectedRecord.value ? 'RECORDED' : 'PENDING',
      latencyMs: selectedRecord.value?.costTimeMs,
    },
    {
      key: reviewStep ? `step-${reviewStep.id}` : 'derived-human-review',
      order: 6,
      label: '进入人工复核',
      description: reviewStep?.summary || '触发人工复核流程。',
      status: humanReview.value ? 'TRIGGERED' : 'PENDING',
      latencyMs: reviewStep?.latencyMs,
      step: reviewStep,
    },
    {
      key: 'derived-review-result',
      order: 7,
      label: statusText(reviewStatus.value) === '已确认' ? '复核已确认' : '复核结果',
      description: coreReason.value,
      status: reviewStatus.value,
      latencyMs: trace.value?.run.latencyMs || selectedRecord.value?.costTimeMs,
    },
  ]

  return rows.map((row) => ({
    ...row,
    statusLabel: statusText(row.status),
    tone: statusTone(row.status),
  }))
})

const metricCards = computed<MetricItem[]>(() => [
  { label: '执行步骤', value: `${timelineSteps.value.length} 个`, icon: Connection },
  { label: '知识命中', value: `${references.value.length} 条`, icon: Reading },
  { label: 'Tool Call', value: `${toolCalls.value.length} 次`, icon: Monitor },
  { label: 'Token 用量', value: tokenUsage.value ? formatNumber(tokenUsage.value) : '未采集', icon: Coin },
  { label: '复核结果', value: statusText(reviewStatus.value), icon: CircleCheck, tone: statusTone(reviewStatus.value) },
])

const riskLevel = computed(() => {
  const status = normalizeStatus(selectedTimelineStep.value?.status)
  if (['FAILED', 'REJECTED', 'ERROR'].includes(status)) return { label: '高风险', tone: 'danger' as Tone }
  if (fallbackReason.value.includes('local-rule')) return { label: '本地演示', tone: 'warning' as Tone }
  if (['READY_FOR_REVIEW', 'PENDING', 'WAITING_REVIEW'].includes(status)) return { label: '待复核', tone: 'warning' as Tone }
  return { label: '低风险', tone: 'success' as Tone }
})

const renderedPromptText = computed(() => {
  return selectedRecord.value?.renderedPrompt
    || selectedGenerationTrace.value?.renderedPromptSummary
    || '当前接口未返回完整 Rendered Prompt；此处只展示真实字段，不生成替代 Prompt。'
})
const fallbackReason = computed(() => {
  const provider = selectedProvider.value.toLowerCase()
  const model = selectedModel.value.toLowerCase()
  if (provider.includes('local') || model.includes('local')) {
    return 'local-rule fallback；本地 Demo 模式默认不连接真实 API Key。'
  }
  return selectedRecord.value?.errorMessage || selectedGenerationTrace.value?.errorMessage || '当前 Trace 未返回 fallback / error 字段。'
})
const coreReason = computed(() => {
  return humanReview.value?.comment
    || selectedRecord.value?.errorMessage
    || '开发者已完成最终确认'
})
const rawJson = computed(() => JSON.stringify({
  traceId: selectedRun.value ? `trace_${selectedRun.value.id}` : '未选择',
  task: selectedRun.value?.title || '未选择运行',
  provider: selectedProvider.value,
  model: selectedModel.value,
  status: reviewStatus.value,
  tokenUsage: tokenUsage.value ?? '未采集',
  fallback: fallbackReason.value,
  timeline: timelineSteps.value.map((item) => ({
    step: String(item.order).padStart(2, '0'),
    label: item.label,
    status: item.statusLabel,
    latencyMs: item.latencyMs ?? 0,
    evidence: item.description,
  })),
  toolCalls: toolCalls.value.map((tool) => ({
    toolName: tool.toolName,
    status: tool.status,
    latencyMs: tool.latencyMs ?? 0,
    inputSummary: tool.inputSummary || '未记录',
    outputSummary: tool.outputSummary || '未记录',
  })),
  humanReview: humanReview.value || '未记录',
  knowledgeReferences: references.value,
  dataSource: 'Demo Data / 本地 H2 Trace 接口 / local-rule fallback',
}, null, 2))
const toolIoJson = computed(() => JSON.stringify(selectedToolCalls.value.map((tool) => ({
  toolName: tool.toolName,
  inputSummary: tool.inputSummary || '未记录',
  outputSummary: tool.outputSummary || '未记录',
  status: tool.status,
  latencyMs: tool.latencyMs ?? 0,
})), null, 2))
const evidenceCode = computed(() => {
  if (activeEvidenceTab.value === 'prompt') return renderedPromptText.value
  if (activeEvidenceTab.value === 'fallback') return fallbackReason.value
  if (activeEvidenceTab.value === 'tools') return toolIoJson.value
  return rawJson.value
})
const evidenceLines = computed(() => evidenceCode.value.split('\n'))
const reviewHistory = computed<ReviewHistoryItem[]>(() => {
  const rows: ReviewHistoryItem[] = []
  if (humanReview.value) {
    rows.push({
      time: formatClock(humanReview.value.updatedAt || humanReview.value.createdAt),
      event: statusText(reviewStatus.value) === '已确认' ? '开发者完成复核确认' : `复核状态：${statusText(reviewStatus.value)}`,
      actor: humanReview.value.reviewer || 'developer',
      tone: statusTone(reviewStatus.value),
    })
  }
  if (selectedRecord.value) {
    rows.push({
      time: formatClock(selectedRecord.value.updatedAt || selectedRecord.value.createdAt),
      event: '进入人工复核',
      actor: '系统',
      tone: 'running',
    })
  }
  if (selectedRun.value) {
    rows.push({
      time: formatClock(selectedRun.value.createdAt),
      event: '系统创建 Trace',
      actor: '系统',
      tone: 'running',
    })
  }
  return rows
})

async function loadRuns() {
  loading.value = true
  try {
    const queryGenerationId = Number(route.query.generationRecordId) || undefined
    const [recordData, runData] = await Promise.all([
      fetchGenerations(),
      fetchAgentRuns({ projectId: selectedProjectId.value }),
    ])
    records.value = recordData
    runs.value = [...runData].sort((a, b) => (b.id || 0) - (a.id || 0))
    const queryRun = queryGenerationId ? runs.value.find((run) => run.generationRecordId === queryGenerationId) : undefined
    const previousRun = runs.value.find((run) => run.id === selectedRunId.value)
    selectedRunId.value = queryRun?.id || previousRun?.id || runs.value[0]?.id
    await loadTrace()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    loading.value = false
  }
}

async function loadTrace() {
  if (!selectedRunId.value) {
    trace.value = undefined
    generationTraces.value = []
    references.value = []
    selectedStepKey.value = ''
    return
  }

  trace.value = await fetchAgentRunTrace(selectedRunId.value)
  const recordId = trace.value.run.generationRecordId
  const [traceRows, referenceRows] = await Promise.all([
    recordId ? fetchGenerationTraces({ generationRecordId: recordId }).catch(() => [] as GenerationTrace[]) : Promise.resolve([]),
    recordId ? fetchKnowledgeReferences(recordId).catch(() => [] as KnowledgeReference[]) : Promise.resolve([]),
  ])
  generationTraces.value = traceRows
  references.value = referenceRows
  selectedStepKey.value = timelineSteps.value[0]?.key || ''
}

async function loadPageData() {
  projects.value = await fetchProjects()
  selectedProjectId.value = projects.value.find((project) => project.projectName === 'DevFlow Copilot')?.id || projects.value[0]?.id
  await loadRuns()
}

async function selectRun(id: number) {
  selectedRunId.value = id
  await loadTrace()
}

function normalizeStatus(status?: string) {
  if (!status) return 'PENDING'
  const map: Record<string, string> = {
    Draft: 'DRAFT',
    Generating: 'GENERATING',
    'Ready for Review': 'READY_FOR_REVIEW',
    Saved: 'SAVED',
    Confirmed: 'CONFIRMED',
    Failed: 'FAILED',
  }
  return map[status] || status.toUpperCase().replace(/\s+/g, '_').replace(/-/g, '_')
}

function statusText(status?: string) {
  const labels: Record<string, string> = {
    DRAFT: '草稿',
    GENERATING: '运行中',
    RUNNING: '运行中',
    READY_FOR_REVIEW: '待人工复核',
    WAITING_REVIEW: '待人工复核',
    PENDING: '待人工复核',
    WAITING: '已触发',
    TRIGGERED: '已触发',
    RECORDED: '已记录',
    SAVED: '已保存',
    CONFIRMED: '已确认',
    SUCCESS: '成功',
    PASSED: '成功',
    FAILED: '失败',
    REJECTED: '已驳回',
    SKIPPED: '未采集',
  }
  return labels[normalizeStatus(status)] || status || '未记录'
}

function statusTone(status?: string): Tone {
  const normalized = normalizeStatus(status)
  if (['CONFIRMED', 'SAVED', 'SUCCESS', 'PASSED'].includes(normalized)) return 'success'
  if (['FAILED', 'REJECTED', 'ERROR'].includes(normalized)) return 'danger'
  if (['GENERATING', 'RUNNING', 'RECORDED', 'TRIGGERED', 'WAITING'].includes(normalized)) return 'running'
  if (['READY_FOR_REVIEW', 'PENDING', 'WAITING_REVIEW', 'DRAFT'].includes(normalized)) return 'warning'
  return 'muted'
}

function formatDuration(value?: number) {
  const ms = Math.max(value || 0, 0)
  if (ms >= 1000) return `${(ms / 1000).toFixed(ms >= 10000 ? 1 : 2)}s`
  return `${ms}ms`
}

function formatClock(value?: string) {
  return value ? value.replace('T', ' ').slice(11, 16) : '--:--'
}

function relativeTime(value?: string) {
  if (!value) return '未记录'
  const timestamp = new Date(value).getTime()
  if (Number.isNaN(timestamp)) return '未记录'
  const diffMs = Math.max(Date.now() - timestamp, 0)
  const minutes = Math.floor(diffMs / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  return `${Math.floor(hours / 24)}d ago`
}

function formatNumber(value: number) {
  return new Intl.NumberFormat('en-US').format(value)
}

function shortText(value?: string, limit = 32) {
  const clean = (value || '未命名运行').replace(/\s+/g, ' ').trim()
  return clean.length > limit ? `${clean.slice(0, limit)}...` : clean
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : '加载 Trace Evidence 失败'
}

onMounted(loadPageData)
</script>

<template>
  <div class="agent-trace-page trace-evidence-page" v-loading="loading">
    <header class="trace-page-header">
      <div>
        <h1>执行证据 Trace Evidence</h1>
        <p>从 Prompt 渲染到人工复核，完整记录每次 AI Coding 运行的可解释证据链。</p>
      </div>
    </header>

    <section class="summary-strip" aria-label="当前执行摘要">
      <h2>当前执行摘要</h2>
      <div class="summary-grid">
        <article
          v-for="metric in metricCards"
          :key="metric.label"
          class="summary-card"
          :data-tone="metric.tone || 'muted'"
        >
          <el-icon class="summary-icon"><component :is="metric.icon" /></el-icon>
          <div>
            <span>{{ metric.label }}</span>
            <strong class="mono">{{ metric.value }}</strong>
          </div>
        </article>
      </div>
    </section>

    <main class="trace-main-grid">
      <aside class="trace-panel run-ledger">
        <header class="panel-heading compact">
          <h2>运行记录</h2>
          <button class="small-button" type="button" @click="loadRuns">
            <el-icon><RefreshRight /></el-icon>
            刷新
          </button>
        </header>

        <div class="run-list">
          <button
            v-for="(run, index) in visibleRuns"
            :key="run.id"
            class="run-row"
            :class="{ active: selectedRunId === run.id }"
            type="button"
            @click="selectRun(run.id)"
          >
            <span class="run-index mono">{{ index + 1 }}</span>
            <span class="run-copy">
              <strong>{{ shortText(run.title, 28) }}</strong>
              <small class="mono">trace_{{ run.id }} <i></i> {{ run.providerName || 'local-rule' }} <i></i> {{ statusText(run.status) }}</small>
            </span>
            <span class="run-time mono">{{ relativeTime(run.createdAt) }}</span>
            <span class="chevron" aria-hidden="true">›</span>
          </button>
          <div v-if="!visibleRuns.length" class="empty-state">暂无运行记录。请先在 Workbench 运行一次本地 Demo。</div>
        </div>

        <footer class="ledger-footer">
          <span>共 {{ runs.length }} 条记录</span>
          <div class="pager" aria-hidden="true">
            <button type="button">‹</button>
            <button type="button" class="active">1</button>
            <button type="button">›</button>
          </div>
        </footer>
      </aside>

      <section class="trace-panel timeline-workbench">
        <header class="panel-heading compact timeline-heading">
          <h2>执行时间线</h2>
          <div class="provider-line mono">
            <span>Provider: <b>{{ selectedProvider }}</b></span>
            <span>Model: <b>{{ selectedModel }}</b></span>
          </div>
        </header>

        <div class="timeline-list">
          <article
            v-for="step in timelineSteps"
            :key="step.key"
            class="timeline-row"
            :class="{ active: selectedTimelineStep?.key === step.key }"
            :data-tone="step.tone"
            @click="selectedStepKey = step.key"
          >
            <span class="timeline-node mono">{{ String(step.order).padStart(2, '0') }}</span>
            <div class="timeline-copy">
              <h3>{{ step.label }}</h3>
              <p>{{ step.description }}</p>
            </div>
            <span class="step-state" :data-tone="step.tone">
              <i></i>{{ step.statusLabel }}
            </span>
            <span class="step-latency mono">{{ formatDuration(step.latencyMs) }}</span>
          </article>
        </div>
      </section>

      <aside class="trace-panel review-inspector">
        <header class="panel-heading compact">
          <h2>步骤详情与人工复核</h2>
        </header>

        <section class="inspector-section current-step">
          <span class="section-number mono">1</span>
          <div>
            <p class="section-label">当前步骤</p>
            <h3>{{ selectedTimelineStep ? `${String(selectedTimelineStep.order).padStart(2, '0')} ${selectedTimelineStep.label}` : '未选择步骤' }}</h3>
            <p>{{ selectedTimelineStep?.description || '暂无步骤说明。' }}</p>
          </div>
        </section>

        <section class="inspector-section">
          <span class="section-number mono">2</span>
          <div class="section-body">
            <p class="section-label">风险与证据</p>
            <dl class="evidence-grid">
              <div>
                <dt>风险等级</dt>
                <dd><span class="risk-chip" :data-tone="riskLevel.tone">{{ riskLevel.label }}</span></dd>
              </div>
              <div>
                <dt>Provider</dt>
                <dd class="mono">{{ selectedProvider }}</dd>
              </div>
              <div>
                <dt>Model</dt>
                <dd class="mono">{{ selectedModel }}</dd>
              </div>
              <div>
                <dt>Token</dt>
                <dd class="mono">{{ tokenUsage ? formatNumber(tokenUsage) : '未采集' }}</dd>
              </div>
              <div>
                <dt>Fallback</dt>
                <dd>{{ fallbackReason }}</dd>
              </div>
              <div>
                <dt>Human Review</dt>
                <dd><StatusBadge :status="reviewStatus" :label="statusText(reviewStatus)" /></dd>
              </div>
              <div>
                <dt>核心原因</dt>
                <dd>{{ coreReason }}</dd>
              </div>
            </dl>
          </div>
        </section>

        <section class="inspector-section">
          <span class="section-number mono">3</span>
          <div class="section-body">
            <p class="section-label">复核结果</p>
            <div class="review-result-card">
              <div class="review-result-head">
                <el-icon><Check /></el-icon>
                <strong>复核已完成</strong>
                <button type="button">查看复核记录</button>
              </div>
              <dl>
                <div>
                  <dt>复核人</dt>
                  <dd>{{ humanReview?.reviewer || 'developer' }}</dd>
                </div>
                <div>
                  <dt>复核时间</dt>
                  <dd class="mono">{{ formatClock(humanReview?.updatedAt || humanReview?.createdAt) }}</dd>
                </div>
                <div>
                  <dt>复核结论</dt>
                  <dd>{{ statusText(reviewStatus) }}</dd>
                </div>
                <div>
                  <dt>核心原因</dt>
                  <dd>{{ coreReason }}</dd>
                </div>
              </dl>
            </div>
          </div>
        </section>

        <section class="inspector-section history-section">
          <span class="section-number mono">4</span>
          <div class="section-body">
            <p class="section-label">状态历史</p>
            <div class="history-table">
              <div class="history-head">
                <span>时间</span>
                <span>事件</span>
                <span>操作人</span>
              </div>
              <div v-for="item in reviewHistory" :key="`${item.time}-${item.event}`" class="history-row">
                <span class="mono"><i :data-tone="item.tone"></i>{{ item.time }}</span>
                <span>{{ item.event }}</span>
                <span>{{ item.actor }}</span>
              </div>
            </div>
          </div>
        </section>
      </aside>
    </main>

    <section class="trace-panel evidence-detail">
      <header class="evidence-header">
        <h2>证据详情</h2>
        <nav class="evidence-tabs" aria-label="证据详情标签页">
          <button type="button" :class="{ active: activeEvidenceTab === 'json' }" @click="activeEvidenceTab = 'json'">Raw JSON</button>
          <button type="button" :class="{ active: activeEvidenceTab === 'prompt' }" @click="activeEvidenceTab = 'prompt'">Rendered Prompt</button>
          <button type="button" :class="{ active: activeEvidenceTab === 'fallback' }" @click="activeEvidenceTab = 'fallback'">Fallback Reason</button>
          <button type="button" :class="{ active: activeEvidenceTab === 'tools' }" @click="activeEvidenceTab = 'tools'">Tool I/O</button>
        </nav>
        <div class="evidence-actions">
          <button type="button" aria-label="格式化 JSON">
            <el-icon><DataLine /></el-icon>
            格式化 JSON
          </button>
          <button type="button" aria-label="复制 JSON">
            <el-icon><DocumentCopy /></el-icon>
            复制 JSON
          </button>
        </div>
      </header>
      <pre class="evidence-code"><code><span v-for="(line, index) in evidenceLines" :key="index" class="code-line"><span class="line-no mono">{{ index + 1 }}</span><span class="line-text mono">{{ line || ' ' }}</span></span></code></pre>
    </section>
  </div>
</template>

<style scoped>
.trace-evidence-page {
  --trace-void: #050b12;
  --trace-base: #07111a;
  --trace-panel: #0b1520;
  --trace-panel-soft: #0d1a25;
  --trace-panel-active: #14242a;
  --trace-border: rgba(91, 112, 129, 0.34);
  --trace-border-soft: rgba(91, 112, 129, 0.2);
  --trace-text: #edf5f4;
  --trace-muted: #9aaab5;
  --trace-dim: #697b88;
  --trace-mint: #5ed58f;
  --trace-mint-soft: rgba(94, 213, 143, 0.13);
  --trace-blue: #72b8ff;
  --trace-blue-soft: rgba(114, 184, 255, 0.13);
  --trace-amber: #d49b38;
  --trace-red: #d36f6f;
  display: grid;
  gap: 12px;
  min-width: 0;
  color: var(--trace-text);
}

.trace-page-header {
  min-width: 0;
  min-height: 62px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-end;
}

.trace-page-header h1,
.trace-page-header p,
.summary-strip h2,
.panel-heading h2,
.timeline-copy h3,
.timeline-copy p,
.section-label,
.current-step h3,
.current-step p,
.evidence-header h2 {
  margin: 0;
}

.trace-page-header h1 {
  font-size: 21px;
  line-height: 28px;
  font-weight: 650;
  letter-spacing: 0;
}

.trace-page-header p {
  margin-top: 7px;
  color: var(--trace-muted);
  font-size: 13px;
  line-height: 20px;
}

.header-facts {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
  padding-bottom: 3px;
}

.header-facts span,
.step-state,
.risk-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  white-space: nowrap;
}

.header-facts span {
  height: 24px;
  padding: 0 9px;
  border: 1px solid var(--trace-border-soft);
  border-radius: 4px;
  background: rgba(5, 11, 18, 0.74);
  color: var(--trace-muted);
  font-size: 11px;
}

.header-facts i,
.step-state i,
.history-row i {
  width: 7px;
  height: 7px;
  border-radius: 2px;
  background: var(--trace-mint);
  flex: 0 0 auto;
}

.summary-strip {
  display: grid;
  gap: 8px;
}

.summary-strip h2 {
  color: var(--trace-text);
  font-size: 13px;
  line-height: 18px;
  font-weight: 600;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  min-width: 0;
  height: 68px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 20px;
  border: 1px solid var(--trace-border);
  border-radius: 7px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.02), transparent), var(--trace-panel);
}

.summary-icon {
  width: 28px;
  height: 28px;
  color: var(--trace-muted);
  font-size: 28px;
  flex: 0 0 auto;
}

.summary-card[data-tone="success"] .summary-icon {
  color: var(--trace-mint);
}

.summary-card span,
.summary-card strong {
  display: block;
  min-width: 0;
}

.summary-card span {
  color: var(--trace-muted);
  font-size: 12px;
}

.summary-card strong {
  margin-top: 4px;
  color: var(--trace-text);
  font-size: 20px;
  line-height: 24px;
  font-weight: 600;
}

.trace-main-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(420px, 1.16fr) minmax(340px, 1fr);
  gap: 12px;
  align-items: stretch;
  height: 500px;
  min-width: 0;
}

.trace-panel {
  min-width: 0;
  border: 1px solid var(--trace-border);
  border-radius: 7px;
  background: var(--trace-panel);
  box-shadow: none;
  overflow: hidden;
}

.run-ledger,
.timeline-workbench,
.review-inspector {
  height: 100%;
  min-height: 0;
}

.panel-heading {
  min-height: 43px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--trace-border-soft);
  background: rgba(10, 20, 30, 0.54);
}

.panel-heading h2 {
  color: var(--trace-text);
  font-size: 14px;
  line-height: 20px;
  font-weight: 650;
}

.small-button {
  height: 28px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 10px;
  border: 1px solid var(--trace-border-soft);
  border-radius: 5px;
  background: #0a121c;
  color: var(--trace-muted);
  cursor: pointer;
  font-size: 12px;
}

.run-list {
  display: grid;
  min-height: 382px;
}

.run-row {
  width: 100%;
  min-width: 0;
  height: 68px;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto 16px;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  border: 0;
  border-bottom: 1px solid var(--trace-border-soft);
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.run-row.active {
  background: linear-gradient(90deg, rgba(94, 213, 143, 0.13), rgba(94, 213, 143, 0.035));
  box-shadow: inset 3px 0 0 var(--trace-mint);
}

.run-index {
  width: 26px;
  height: 26px;
  display: grid;
  place-items: center;
  border: 1px solid var(--trace-border);
  border-radius: 50%;
  color: #c8d7dc;
  font-size: 13px;
}

.run-row.active .run-index {
  border-color: var(--trace-mint);
  color: var(--trace-mint);
  background: rgba(94, 213, 143, 0.1);
}

.run-copy {
  min-width: 0;
}

.run-copy strong,
.run-copy small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.run-copy strong {
  color: var(--trace-text);
  font-size: 13px;
  line-height: 19px;
  font-weight: 650;
}

.run-copy small {
  margin-top: 5px;
  color: var(--trace-muted);
  font-size: 11px;
}

.run-copy small i {
  display: inline-block;
  width: 4px;
  height: 4px;
  margin: 0 6px 2px;
  border-radius: 50%;
  background: var(--trace-dim);
}

.run-time,
.chevron {
  color: var(--trace-muted);
  font-size: 12px;
}

.chevron {
  font-size: 24px;
  line-height: 1;
}

.ledger-footer {
  height: 46px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  color: var(--trace-muted);
  font-size: 12px;
}

.pager {
  display: flex;
  border: 1px solid var(--trace-border-soft);
  border-radius: 5px;
  overflow: hidden;
}

.pager button {
  width: 34px;
  height: 26px;
  border: 0;
  border-right: 1px solid var(--trace-border-soft);
  background: #0a121c;
  color: var(--trace-muted);
}

.pager button:last-child {
  border-right: 0;
}

.pager button.active {
  color: var(--trace-text);
  background: #111e2b;
}

.timeline-heading {
  align-items: center;
}

.provider-line {
  display: flex;
  gap: 16px;
  color: var(--trace-muted);
  font-size: 11px;
}

.provider-line b {
  color: var(--trace-mint);
  font-weight: 600;
}

.timeline-list {
  position: relative;
  display: grid;
  padding: 10px 14px 14px;
}

.timeline-list::before {
  content: "";
  position: absolute;
  top: 30px;
  bottom: 38px;
  left: 41px;
  width: 2px;
  background: linear-gradient(180deg, rgba(94, 213, 143, 0.72), rgba(114, 184, 255, 0.72));
}

.timeline-row {
  position: relative;
  min-width: 0;
  min-height: 58px;
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) 92px 50px;
  align-items: center;
  gap: 10px;
  padding: 7px 0 7px 0;
  cursor: pointer;
}

.timeline-row + .timeline-row {
  border-top: 1px solid var(--trace-border-soft);
}

.timeline-row.active {
  background: linear-gradient(90deg, rgba(94, 213, 143, 0.08), transparent 76%);
}

.timeline-node {
  position: relative;
  z-index: 1;
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--trace-blue);
  color: #061018;
  font-size: 11px;
  font-weight: 800;
}

.timeline-row[data-tone="success"] .timeline-node {
  background: var(--trace-mint);
}

.timeline-row[data-tone="warning"] .timeline-node {
  background: var(--trace-amber);
}

.timeline-row[data-tone="danger"] .timeline-node {
  background: var(--trace-red);
}

.timeline-copy {
  min-width: 0;
}

.timeline-copy h3 {
  color: var(--trace-text);
  font-size: 14px;
  line-height: 20px;
  font-weight: 650;
}

.timeline-copy p {
  margin-top: 3px;
  overflow: hidden;
  color: var(--trace-muted);
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-state {
  justify-self: end;
  color: var(--trace-mint);
  font-size: 12px;
}

.step-state[data-tone="warning"] {
  color: var(--trace-amber);
}

.step-state[data-tone="danger"] {
  color: var(--trace-red);
}

.step-state[data-tone="running"] {
  color: var(--trace-blue);
}

.step-state[data-tone="warning"] i {
  background: var(--trace-amber);
}

.step-state[data-tone="danger"] i {
  background: var(--trace-red);
}

.step-state[data-tone="running"] i {
  background: var(--trace-blue);
}

.step-latency {
  justify-self: end;
  color: var(--trace-muted);
  font-size: 11px;
}

.review-inspector {
  display: grid;
  align-content: start;
}

.inspector-section {
  position: relative;
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  gap: 8px;
  padding: 5px 14px;
  border-bottom: 1px solid var(--trace-border-soft);
}

.section-number {
  width: 16px;
  height: 16px;
  display: grid;
  place-items: center;
  margin-top: 1px;
  border: 1px solid var(--trace-border);
  border-radius: 50%;
  color: var(--trace-muted);
  font-size: 10px;
}

.section-label {
  color: var(--trace-muted);
  font-size: 11px;
  line-height: 15px;
}

.current-step h3 {
  margin-top: 2px;
  color: var(--trace-text);
  font-size: 14px;
  line-height: 18px;
}

.current-step p {
  margin-top: 1px;
  color: var(--trace-muted);
  font-size: 11px;
  line-height: 15px;
}

.section-body {
  min-width: 0;
}

.evidence-grid {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  margin: 4px 0 0;
  border: 1px solid var(--trace-border-soft);
}

.evidence-grid div {
  display: contents;
}

.evidence-grid dt,
.evidence-grid dd {
  min-width: 0;
  margin: 0;
  padding: 2px 8px;
  border-bottom: 1px solid var(--trace-border-soft);
  font-size: 10.5px;
  line-height: 14px;
}

.evidence-grid div:last-child dt,
.evidence-grid div:last-child dd {
  border-bottom: 0;
}

.evidence-grid dt {
  color: var(--trace-muted);
  background: rgba(255, 255, 255, 0.018);
}

.evidence-grid dd {
  overflow: hidden;
  color: var(--trace-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.risk-chip {
  height: 18px;
  padding: 0 8px;
  border-radius: 4px;
  background: rgba(212, 155, 56, 0.16);
  color: var(--trace-amber);
  font-size: 11px;
}

.risk-chip[data-tone="success"] {
  background: var(--trace-mint-soft);
  color: var(--trace-mint);
}

.risk-chip[data-tone="danger"] {
  background: rgba(211, 111, 111, 0.16);
  color: var(--trace-red);
}

.review-result-card {
  margin-top: 4px;
  border: 1px solid rgba(94, 213, 143, 0.52);
  border-radius: 6px;
  background: linear-gradient(180deg, rgba(94, 213, 143, 0.12), rgba(94, 213, 143, 0.04));
  overflow: hidden;
}

.review-result-head {
  height: 28px;
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  padding: 0 10px;
  border-bottom: 1px solid rgba(94, 213, 143, 0.32);
  color: var(--trace-mint);
}

.review-result-head .el-icon {
  width: 18px;
  height: 18px;
  display: grid;
  place-items: center;
  border: 1px solid var(--trace-mint);
  border-radius: 50%;
}

.review-result-head strong {
  font-size: 12px;
}

.review-result-head button {
  height: 22px;
  border: 1px solid var(--trace-border);
  border-radius: 4px;
  background: #0a121c;
  color: var(--trace-muted);
  font-size: 11px;
}

.review-result-card dl {
  display: grid;
  grid-template-columns: 68px minmax(0, 1fr);
  margin: 4px 10px 5px;
  gap: 2px 8px;
}

.review-result-card div {
  display: contents;
}

.review-result-card dt,
.review-result-card dd {
  margin: 0;
  min-width: 0;
  overflow: hidden;
  font-size: 11px;
  line-height: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-result-card dt {
  color: var(--trace-muted);
}

.review-result-card dd {
  color: var(--trace-text);
}

.history-section {
  border-bottom: 0;
}

.history-table {
  display: grid;
  margin-top: 3px;
  font-size: 11px;
}

.history-head,
.history-row {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr) 64px;
  gap: 8px;
  align-items: center;
}

.history-head {
  color: var(--trace-muted);
  line-height: 18px;
}

.history-row {
  min-height: 17px;
  border-top: 1px solid var(--trace-border-soft);
  color: var(--trace-text);
}

.history-row span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-row span:first-child {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--trace-muted);
}

.history-row i[data-tone="running"] {
  background: var(--trace-blue);
}

.history-row i[data-tone="warning"] {
  background: var(--trace-amber);
}

.history-row i[data-tone="danger"] {
  background: var(--trace-red);
}

.evidence-detail {
  min-height: 132px;
}

.evidence-header {
  min-height: 38px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 0 12px;
  border-bottom: 1px solid var(--trace-border-soft);
}

.evidence-header h2 {
  color: var(--trace-text);
  font-size: 13px;
  line-height: 18px;
  font-weight: 650;
}

.evidence-tabs {
  display: flex;
  min-width: 0;
  align-self: stretch;
}

.evidence-tabs button {
  position: relative;
  height: 38px;
  padding: 0 14px;
  border: 0;
  background: transparent;
  color: var(--trace-muted);
  cursor: pointer;
  font-size: 12px;
}

.evidence-tabs button.active {
  color: var(--trace-mint);
}

.evidence-tabs button.active::after {
  content: "";
  position: absolute;
  right: 8px;
  bottom: 0;
  left: 8px;
  height: 2px;
  background: var(--trace-mint);
}

.evidence-actions {
  display: flex;
  gap: 8px;
}

.evidence-actions button {
  height: 28px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 0;
  background: transparent;
  color: var(--trace-muted);
  font-size: 12px;
}

.evidence-code {
  height: 93px;
  margin: 0;
  padding: 8px 0;
  overflow: hidden;
  background: #06111a;
  color: #d8e7e5;
  font-size: 12px;
  line-height: 20px;
}

.code-line {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr);
  min-width: 0;
}

.line-no {
  padding-right: 14px;
  color: var(--trace-muted);
  text-align: right;
  user-select: none;
}

.line-text {
  min-width: 0;
  overflow: hidden;
  padding-right: 16px;
  color: #d9e7df;
  text-overflow: ellipsis;
  white-space: pre;
}

.empty-state {
  min-height: 96px;
  display: grid;
  place-items: center;
  padding: 14px;
  color: var(--trace-muted);
  font-size: 12px;
  text-align: center;
}

.trace-evidence-page > *,
.trace-main-grid > *,
.panel-heading > *,
.run-row > *,
.timeline-row > *,
.inspector-section > *,
.evidence-header > *,
.summary-card > * {
  min-width: 0;
}

@media (max-width: 1280px) {
  .summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .trace-main-grid {
    grid-template-columns: minmax(300px, 0.9fr) minmax(0, 1fr);
  }

  .review-inspector {
    grid-column: 1 / -1;
    min-height: 0;
  }
}

@media (max-width: 900px) {
  .trace-page-header,
  .summary-grid,
  .trace-main-grid,
  .evidence-header {
    grid-template-columns: minmax(0, 1fr);
  }

  .trace-page-header {
    display: grid;
  }

  .header-facts,
  .evidence-actions {
    justify-content: flex-start;
  }

  .timeline-row {
    grid-template-columns: 40px minmax(0, 1fr);
  }

  .step-state,
  .step-latency {
    justify-self: start;
    grid-column: 2;
  }
}
</style>
