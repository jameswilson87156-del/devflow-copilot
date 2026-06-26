<script setup lang="ts">
import { computed, onMounted, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Plus } from '@element-plus/icons-vue'
import {
  fetchAgentRunTrace,
  fetchAgentRuns,
  fetchDashboardStats,
  fetchKnowledgeDocuments,
  fetchKnowledgeReferences,
  fetchLogHistory,
  fetchPrompts,
} from '@/api/devflow'
import MetricCard from '@/components/MetricCard.vue'
import ProviderBadge from '@/components/ProviderBadge.vue'
import SectionCard from '@/components/SectionCard.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import type {
  AgentRun,
  AgentRunTrace,
  DashboardStats,
  GenerationRecord,
  HumanReview,
  KnowledgeDocument,
  KnowledgeReference,
  LogAnalysis,
  PromptTemplate,
  ToolCallRecord,
} from '@/types/domain'

type MetricTone = 'accent' | 'success' | 'warning' | 'danger' | 'running' | 'muted' | 'info'

interface ProviderHealthRow {
  provider: string
  model: string
  count: number
  successRate: number
  averageLatencyMs: number
  status: 'SUCCESS' | 'READY_FOR_REVIEW' | 'FAILED'
}

interface TimelineItem {
  id: string
  tone: 'success' | 'warning' | 'danger' | 'running' | 'info'
  title: string
  detail: string
  time?: string
}

// dashboard/stats does not expose tool-call totals, knowledge-hit totals,
// provider health details, or review rows. This page derives those from the
// existing trace/reference APIs; when fields are absent it shows zero or an
// empty state instead of pretending they are backend statistics.
const DASHBOARD_SAFE_FALLBACK = {
  providerName: 'local-rule',
  modelName: 'local-rule',
  reviewer: '未分配',
  emptyMetric: 0,
} as const

const WORKFLOW_STEPS = [
  { key: 'Prompt', label: 'Prompt', desc: '模板渲染' },
  { key: 'Provider', label: 'Provider', desc: '模型路由' },
  { key: 'Tool Call', label: 'Tool Call', desc: '工具调用' },
  { key: 'Trace', label: 'Trace', desc: '过程记录' },
  { key: 'Human Review', label: 'Human Review', desc: '人工审核' },
] as const

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
const agentRuns = shallowRef<AgentRun[]>([])
const traceSnapshots = shallowRef<AgentRunTrace[]>([])
const knowledgeReferences = shallowRef<KnowledgeReference[]>([])
const knowledgeDocuments = shallowRef<KnowledgeDocument[]>([])

const recentRecords = computed(() => sortByTime(stats.value.recentGenerations).slice(0, 6))
const generationById = computed(() => new Map(recentRecords.value.map((record) => [record.id, record])))
const enabledPrompts = computed(() => promptTemplates.value.filter((template) => template.enabled).slice(0, 5))
const recentAgentRuns = computed(() => sortByTime(agentRuns.value).slice(0, 5))
const toolCalls = computed(() => traceSnapshots.value.flatMap((trace) => trace.toolCalls))
const humanReviews = computed(() => traceSnapshots.value.flatMap((trace) => {
  return trace.humanReviews.map((review) => ({
    ...review,
    run: trace.run,
    record: trace.run.generationRecordId ? generationById.value.get(trace.run.generationRecordId) : undefined,
  }))
}))

const kpiItems = computed(() => {
  const items: Array<{ label: string; value: string | number; code: string; tone: MetricTone }> = [
    {
      label: '今日运行数',
      value: stats.value.todayGenerationCount,
      code: 'Runs',
      tone: 'accent',
    },
    {
      label: '成功率',
      value: formatPercent(stats.value.successRate),
      code: 'Success',
      tone: stats.value.successRate >= 80 ? 'success' : 'warning',
    },
    {
      label: '平均耗时',
      value: formatDuration(stats.value.averageLatencyMs),
      code: 'Latency',
      tone: 'running',
    },
    {
      label: '人工审核',
      value: stats.value.humanReviewCount,
      code: 'Human Review',
      tone: 'warning',
    },
    {
      label: '工具调用',
      value: toolCalls.value.length || DASHBOARD_SAFE_FALLBACK.emptyMetric,
      code: 'Tool Call',
      tone: 'info',
    },
    {
      label: '知识库命中',
      value: knowledgeReferences.value.length || DASHBOARD_SAFE_FALLBACK.emptyMetric,
      code: 'RAG Hits',
      tone: 'success',
    },
  ]
  return items
})

const providerHealthRows = computed<ProviderHealthRow[]>(() => {
  const providers = new Map<string, GenerationRecord[]>()
  recentRecords.value.forEach((record) => {
    const provider = record.providerName || DASHBOARD_SAFE_FALLBACK.providerName
    const current = providers.get(provider) || []
    current.push(record)
    providers.set(provider, current)
  })

  return Array.from(providers.entries()).map(([provider, records]) => {
    const successCount = records.filter((record) => Boolean(record.success) || normalizeStatus(record.status) === 'CONFIRMED').length
    const failedCount = records.filter((record) => normalizeStatus(record.status) === 'FAILED').length
    const avgLatency = average(records.map((record) => record.costTimeMs || 0))
    const model = records.find((record) => record.modelName)?.modelName || DASHBOARD_SAFE_FALLBACK.modelName
    const successRate = records.length ? Math.round((successCount / records.length) * 1000) / 10 : 0
    return {
      provider,
      model,
      count: records.length,
      successRate,
      averageLatencyMs: avgLatency,
      status: failedCount > successCount ? 'FAILED' : successRate >= 80 ? 'SUCCESS' : 'READY_FOR_REVIEW',
    }
  })
})

const topTools = computed(() => {
  const counts = new Map<string, { count: number; latencyMs: number }>()
  toolCalls.value.forEach((tool) => {
    const current = counts.get(tool.toolName) || { count: 0, latencyMs: 0 }
    current.count += 1
    current.latencyMs += tool.latencyMs || 0
    counts.set(tool.toolName, current)
  })
  return Array.from(counts.entries())
    .map(([toolName, value]) => ({
      toolName,
      count: value.count,
      averageLatencyMs: value.count ? Math.round(value.latencyMs / value.count) : 0,
    }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 5)
})

const latestKnowledgeRows = computed(() => {
  if (knowledgeReferences.value.length > 0) {
    return knowledgeReferences.value.slice(0, 5).map((reference) => ({
      key: `ref-${reference.chunkId}`,
      title: reference.documentTitle,
      meta: `${reference.citationLabel} · score ${formatScore(reference.score)}`,
      snippet: reference.snippet,
      isReference: true,
    }))
  }

  return knowledgeDocuments.value.slice(0, 4).map((document) => ({
    key: `doc-${document.id}`,
    title: document.title,
    meta: `${document.sourceType} · ${document.chunkCount} chunks`,
    snippet: '已有 Knowledge 文档，最近生成暂未返回引用命中。',
    isReference: false,
  }))
})

const timelineItems = computed<TimelineItem[]>(() => {
  const generationEvents = recentRecords.value.map((record) => ({
    id: `generation-${record.id}`,
    tone: timelineTone(normalizeStatus(record.status)),
    title: `${statusLabel(record.status, record.confirmed)}：${record.inputSummary || formatType(record.generationType)}`,
    detail: `${record.providerName || DASHBOARD_SAFE_FALLBACK.providerName} · ${formatDuration(record.costTimeMs)}`,
    time: record.createdAt,
  }))

  const reviewEvents = humanReviews.value.map((review) => ({
    id: `review-${review.id}`,
    tone: timelineTone(review.reviewStatus),
    title: `${reviewStatusLabel(review.reviewStatus)}：${review.record?.inputSummary || review.run.title}`,
    detail: `${review.reviewer || DASHBOARD_SAFE_FALLBACK.reviewer} · Human Review`,
    time: review.updatedAt || review.createdAt,
  }))

  const knowledgeEvents = knowledgeReferences.value.slice(0, 3).map((reference) => ({
    id: `knowledge-${reference.chunkId}`,
    tone: 'info' as const,
    title: `知识命中：${reference.documentTitle}`,
    detail: `${reference.citationLabel} · RAG reference`,
    time: undefined,
  }))

  const logEvents = logHistory.value.slice(0, 3).map((log) => ({
    id: `log-${log.id}`,
    tone: riskTone(log.riskLevel),
    title: `日志诊断：${log.exceptionType}`,
    detail: `${log.riskLevel} · Root Cause`,
    time: log.createdAt,
  }))

  return [...generationEvents, ...reviewEvents, ...knowledgeEvents, ...logEvents]
    .sort((a, b) => toTime(b.time) - toTime(a.time))
    .slice(0, 8)
})

function normalizeStatus(status: string) {
  const map: Record<string, string> = {
    Draft: 'DRAFT',
    Generating: 'GENERATING',
    'Ready for Review': 'READY_FOR_REVIEW',
    Saved: 'SAVED',
    Confirmed: 'CONFIRMED',
    Failed: 'FAILED',
  }
  return map[status] || status.toUpperCase().replace(/\s+/g, '_')
}

function statusLabel(status: string, confirmed = false) {
  if (confirmed || normalizeStatus(status) === 'CONFIRMED') return '已确认'
  const labels: Record<string, string> = {
    DRAFT: '草稿',
    GENERATING: '运行中',
    READY_FOR_REVIEW: '待审核',
    SAVED: '已保存',
    CONFIRMED: '已确认',
    FAILED: '失败',
    SUCCESS: '成功',
  }
  return labels[normalizeStatus(status)] || status
}

function reviewStatusLabel(status: string) {
  const labels: Record<string, string> = {
    PENDING: '待审核',
    SAVED: '已保存',
    CONFIRMED: '已确认',
    REJECTED: '已驳回',
  }
  return labels[normalizeStatus(status)] || status
}

function formatType(type: string) {
  const map: Record<string, string> = {
    'requirement-split': '需求拆解',
    'code-plan': '代码计划',
    'readme-generate': 'README',
    'commit-message': 'Commit Message',
    'fix-prompt': '修复 Prompt',
    'log-analysis': '日志诊断',
  }
  return map[type] || type
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '--'
}

function formatRelativeTime(value?: string) {
  if (!value) return '刚刚'
  const deltaMs = Date.now() - new Date(value).getTime()
  if (!Number.isFinite(deltaMs) || deltaMs < 0) return formatTime(value)
  const minutes = Math.max(1, Math.round(deltaMs / 60000))
  if (minutes < 60) return `${minutes}m 前`
  const hours = Math.round(minutes / 60)
  if (hours < 24) return `${hours}h 前`
  return formatTime(value).slice(5)
}

function formatDuration(value?: number) {
  const ms = Math.max(value || 0, 0)
  if (ms >= 1000) {
    return `${(ms / 1000).toFixed(ms >= 10000 ? 1 : 2)}s`
  }
  return `${ms}ms`
}

function formatPercent(value?: number) {
  const normalized = Number.isFinite(value) ? Number(value) : 0
  return `${Math.round(normalized * 10) / 10}%`
}

function formatScore(value?: number) {
  if (!Number.isFinite(value)) return '0.00'
  return Number(value).toFixed(2)
}

function countVariables(template: PromptTemplate) {
  const matches = template.variables?.match(/[\w\u4e00-\u9fa5-]+/g)
  return matches?.length || 0
}

function average(values: number[]) {
  const valid = values.filter((value) => Number.isFinite(value))
  if (!valid.length) return 0
  return Math.round(valid.reduce((sum, value) => sum + value, 0) / valid.length)
}

function toTime(value?: string) {
  if (!value) return 0
  const time = new Date(value).getTime()
  return Number.isFinite(time) ? time : 0
}

function sortByTime<T extends { createdAt?: string; id?: number }>(items: T[]) {
  return [...items].sort((a, b) => {
    const byTime = toTime(b.createdAt) - toTime(a.createdAt)
    if (byTime !== 0) return byTime
    return (b.id || 0) - (a.id || 0)
  })
}

function timelineTone(status: string): TimelineItem['tone'] {
  const normalized = normalizeStatus(status)
  if (['CONFIRMED', 'SAVED', 'SUCCESS'].includes(normalized)) return 'success'
  if (['FAILED', 'REJECTED', 'ERROR'].includes(normalized)) return 'danger'
  if (['GENERATING', 'RUNNING'].includes(normalized)) return 'running'
  return 'warning'
}

function riskTone(riskLevel: string): TimelineItem['tone'] {
  const normalized = riskLevel.toUpperCase()
  if (normalized.includes('HIGH')) return 'danger'
  if (normalized.includes('MEDIUM')) return 'warning'
  return 'info'
}

function toolStatus(tool: ToolCallRecord) {
  return normalizeStatus(tool.status || 'SUCCESS')
}

function openRecord(record: GenerationRecord) {
  router.push({ path: '/history', query: { id: record.id } })
}

function openAgentRun(run: AgentRun) {
  router.push({ path: '/agent-runs', query: { generationRecordId: run.generationRecordId } })
}

async function loadDashboard() {
  loading.value = true
  try {
    const [statData, promptData, logData, runData, documentData] = await Promise.all([
      fetchDashboardStats(),
      fetchPrompts(),
      fetchLogHistory(),
      fetchAgentRuns(),
      fetchKnowledgeDocuments(),
    ])

    stats.value = statData
    promptTemplates.value = promptData
    logHistory.value = logData
    agentRuns.value = runData
    knowledgeDocuments.value = documentData

    const latestRuns = sortByTime(runData).slice(0, 4)
    const traces = await Promise.all(
      latestRuns.map((run) => fetchAgentRunTrace(run.id).catch(() => undefined)),
    )
    traceSnapshots.value = traces.filter((trace): trace is AgentRunTrace => Boolean(trace))

    const referenceResults = await Promise.all(
      sortByTime(statData.recentGenerations)
        .slice(0, 4)
        .map((record) => fetchKnowledgeReferences(record.id).catch(() => [] as KnowledgeReference[])),
    )
    knowledgeReferences.value = referenceResults.flat()
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<template>
  <div class="dashboard dashboard-command" v-loading="loading">
    <section class="dashboard-hero" aria-labelledby="dashboard-title">
      <div class="hero-copy">
        <span class="hero-kicker mono">DEVFLOW / AI CODING WORKFLOW</span>
        <h1 id="dashboard-title">DevFlow Copilot</h1>
        <h2>Agentic Coding 工作流控制台</h2>
        <p>Prompt、Provider、Trace、Knowledge、Human Review</p>
        <div class="hero-actions">
          <button class="primary-action" type="button" @click="router.push('/workbench')">
            <el-icon><Plus /></el-icon>
            新建 Workflow
          </button>
          <button class="ghost-action" type="button" @click="router.push('/agent-runs')">
            查看 Trace
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
      </div>

      <div class="hero-workflow" aria-label="Agent Workflow Overview">
        <div class="workflow-card-mini" v-for="(step, index) in WORKFLOW_STEPS" :key="step.key">
          <span class="workflow-node mono">{{ index + 1 }}</span>
          <strong>{{ step.label }}</strong>
          <small>{{ step.desc }}</small>
        </div>
      </div>
    </section>

    <section class="kpi-grid" aria-label="Dashboard KPI">
      <MetricCard
        v-for="item in kpiItems"
        :key="item.code"
        :code="item.code"
        :label="item.label"
        :tone="item.tone"
        :value="item.value"
      />
    </section>
    <p class="data-source-note">
      KPI 主统计来自 <span class="mono">GET /api/dashboard/stats</span>；Tool Call 与 RAG Hits 由现有 Trace / Knowledge 引用接口派生，缺失时显示 0。
    </p>

    <main class="dashboard-grid">
      <SectionCard
        class="recent-runs-card"
        title="最近智能体运行"
        subtitle="来自 agent-runs 与 dashboard/stats 的最近运行记录"
        eyebrow="Agent Runs"
      >
        <template #actions>
          <button class="section-link" type="button" @click="router.push('/agent-runs')">
            查看全部
            <el-icon><ArrowRight /></el-icon>
          </button>
        </template>

        <div class="run-table-head" aria-hidden="true">
          <span>运行 ID</span>
          <span>Prompt</span>
          <span>Provider</span>
          <span>状态</span>
          <span>耗时</span>
          <span>审核人</span>
        </div>
        <div class="run-table">
          <div v-if="!recentAgentRuns.length" class="empty-state">暂无 Agent Run 记录。</div>
          <button
            v-for="run in recentAgentRuns"
            :key="run.id"
            class="run-row"
            type="button"
            @click="openAgentRun(run)"
          >
            <span class="mono run-id">run_{{ run.id }}</span>
            <span class="run-title">
              <strong>{{ run.title }}</strong>
              <small>{{ run.goal || generationById.get(run.generationRecordId || 0)?.inputSummary || 'Workflow audit record' }}</small>
            </span>
            <ProviderBadge :provider="run.providerName || DASHBOARD_SAFE_FALLBACK.providerName" :model="run.modelName" />
            <StatusBadge :status="run.status" :label="statusLabel(run.status)" />
            <span class="mono run-cost">{{ formatDuration(run.latencyMs) }}</span>
            <span class="reviewer">{{ humanReviews.find((review) => review.run.id === run.id)?.reviewer || DASHBOARD_SAFE_FALLBACK.reviewer }}</span>
          </button>
        </div>
      </SectionCard>

      <SectionCard
        class="prompts-card"
        title="启用中的 Prompt 模板"
        subtitle="只展示已启用模板，不写死使用次数"
        eyebrow="Prompt Studio"
      >
        <template #actions>
          <button class="section-link" type="button" @click="router.push('/prompts')">
            查看全部
            <el-icon><ArrowRight /></el-icon>
          </button>
        </template>

        <div class="prompt-list">
          <div v-if="!enabledPrompts.length" class="empty-state">暂无启用中的 Prompt 模板。</div>
          <button v-for="template in enabledPrompts" :key="template.id" type="button" @click="router.push('/prompts')">
            <span class="prompt-icon mono">{{ template.templateType.slice(0, 2).toUpperCase() }}</span>
            <span>
              <strong>{{ template.templateName }}</strong>
              <small>{{ template.templateKey }} · {{ countVariables(template) }} variables</small>
            </span>
            <em class="mono">v{{ template.version }}</em>
          </button>
        </div>
      </SectionCard>

      <SectionCard class="workflow-overview-card" title="Agent Workflow Overview" subtitle="Prompt -> Provider -> Tool Call -> Trace -> Human Review" eyebrow="Workflow">
        <div class="overview-flow">
          <div v-for="(step, index) in WORKFLOW_STEPS" :key="step.key" class="overview-step">
            <span class="overview-step__node mono">{{ index + 1 }}</span>
            <strong>{{ step.label }}</strong>
            <small>{{ step.desc }}</small>
          </div>
        </div>
      </SectionCard>

      <SectionCard class="provider-card" title="Provider 健康状态" subtitle="由最近生成记录聚合，不新增 Provider 健康接口" eyebrow="Provider Health">
        <div class="provider-list">
          <div v-if="!providerHealthRows.length" class="empty-state">暂无 Provider 调用记录。</div>
          <article v-for="provider in providerHealthRows" :key="provider.provider" class="provider-row">
            <div class="provider-row__top">
              <ProviderBadge :provider="provider.provider" :model="provider.model" />
              <StatusBadge :status="provider.status" :label="provider.status === 'SUCCESS' ? '正常' : provider.status === 'FAILED' ? '失败' : '待观察'" />
            </div>
            <div class="provider-metrics">
              <span><strong>{{ formatDuration(provider.averageLatencyMs) }}</strong><small>平均耗时</small></span>
              <span><strong>{{ formatPercent(provider.successRate) }}</strong><small>成功率</small></span>
              <span><strong>{{ provider.count }}</strong><small>请求数</small></span>
            </div>
          </article>
        </div>
      </SectionCard>

      <SectionCard class="review-card" title="最近人工审核" subtitle="来自 Agent Run Trace 的 Human Review 明细" eyebrow="Human Review">
        <div class="review-list">
          <div v-if="!humanReviews.length" class="empty-state">暂无 Human Review 明细。</div>
          <article v-for="review in humanReviews.slice(0, 5)" :key="review.id" class="review-row">
            <div>
              <strong>{{ review.record?.inputSummary || review.run.title }}</strong>
              <small>{{ review.reviewer || DASHBOARD_SAFE_FALLBACK.reviewer }} · {{ formatRelativeTime(review.updatedAt || review.createdAt) }}</small>
            </div>
            <StatusBadge :status="review.reviewStatus" :label="reviewStatusLabel(review.reviewStatus)" />
          </article>
        </div>
      </SectionCard>

      <SectionCard class="knowledge-card" title="最新知识引用" subtitle="优先展示 generation_knowledge_reference 命中" eyebrow="Knowledge / RAG">
        <template #actions>
          <button class="section-link" type="button" @click="router.push('/knowledge')">
            查看知识库
            <el-icon><ArrowRight /></el-icon>
          </button>
        </template>

        <div class="knowledge-list">
          <div v-if="!latestKnowledgeRows.length" class="empty-state">暂无 Knowledge 文档或引用。</div>
          <article v-for="item in latestKnowledgeRows" :key="item.key" class="knowledge-row" :data-reference="item.isReference">
            <span class="knowledge-marker"></span>
            <div>
              <strong>{{ item.title }}</strong>
              <small>{{ item.meta }}</small>
              <p>{{ item.snippet }}</p>
            </div>
          </article>
        </div>
      </SectionCard>

      <SectionCard class="timeline-card" title="最近活动时间线" subtitle="生成、审核、日志诊断与知识引用统一按时间展示" eyebrow="Recent Activity">
        <div class="timeline-list">
          <div v-if="!timelineItems.length" class="empty-state">暂无最近活动。</div>
          <article v-for="item in timelineItems" :key="item.id" class="timeline-row" :data-tone="item.tone">
            <span class="timeline-dot" aria-hidden="true"></span>
            <div>
              <strong>{{ item.title }}</strong>
              <small>{{ item.detail }}</small>
            </div>
            <time>{{ formatRelativeTime(item.time) }}</time>
          </article>
        </div>
      </SectionCard>

      <SectionCard class="tools-card" title="高频工具" subtitle="来自最近 Agent Trace 的 Tool Call 记录" eyebrow="Top Used Tools">
        <div class="tools-list">
          <div v-if="!topTools.length" class="empty-state">暂无 Tool Call 记录。</div>
          <article v-for="(tool, index) in topTools" :key="tool.toolName" class="tool-row">
            <span class="mono">{{ index + 1 }}</span>
            <strong>{{ tool.toolName }}</strong>
            <div class="tool-meter"><i :style="{ width: `${Math.max(12, (tool.count / Math.max(topTools[0]?.count || 1, 1)) * 100)}%` }"></i></div>
            <small class="mono">{{ tool.count }} 次 · {{ formatDuration(tool.averageLatencyMs) }}</small>
          </article>
        </div>
      </SectionCard>
    </main>
  </div>
</template>

<style scoped>
.dashboard-command {
  display: grid;
  gap: 12px;
  width: 100%;
  min-width: 0;
}

.dashboard-hero {
  min-width: 0;
  min-height: 130px;
  display: grid;
  grid-template-columns: minmax(0, 0.98fr) minmax(420px, 0.82fr);
  gap: 18px;
  padding: 20px 22px;
  border: var(--border-default);
  border-radius: var(--radius-card);
  background:
    linear-gradient(135deg, rgba(124, 92, 255, 0.22), rgba(34, 211, 238, 0.05) 48%, rgba(59, 130, 246, 0.16)),
    linear-gradient(90deg, rgba(11, 16, 32, 0.92), rgba(16, 24, 38, 0.96)),
    var(--color-card);
  overflow: hidden;
}

.hero-copy {
  min-width: 0;
}

.hero-kicker {
  display: block;
  color: var(--color-accent-cyan);
  font-size: 10px;
  letter-spacing: 0.08em;
}

.hero-copy h1,
.hero-copy h2,
.hero-copy p {
  min-width: 0;
  margin: 0;
}

.hero-copy h1 {
  margin-top: 6px;
  font-size: clamp(28px, 3vw, 40px);
  line-height: 1.08;
  font-weight: 720;
  color: var(--color-text-primary);
}

.hero-copy h2 {
  margin-top: 7px;
  color: var(--color-text-secondary);
  font-size: 16px;
  line-height: 22px;
  font-weight: 520;
}

.hero-copy p {
  margin-top: 10px;
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 20px;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.primary-action,
.ghost-action,
.section-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 30px;
  border-radius: 5px;
  cursor: pointer;
  font-size: 12px;
  white-space: nowrap;
  transition: background-color 140ms ease, border-color 140ms ease, color 140ms ease;
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

.ghost-action,
.section-link {
  border: 1px solid var(--color-border);
  background: rgba(8, 11, 18, 0.42);
  color: var(--color-text-secondary);
}

.ghost-action {
  padding: 0 10px;
}

.section-link {
  height: 26px;
  padding: 0 8px;
  border-color: transparent;
  background: transparent;
  color: var(--color-running);
}

.ghost-action:hover,
.section-link:hover {
  border-color: var(--color-accent-border);
  background: var(--color-accent-muted);
  color: var(--color-text-primary);
}

.hero-workflow {
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  align-content: center;
  gap: 0;
  padding: 12px;
  border: 1px solid rgba(124, 92, 255, 0.26);
  border-radius: 8px;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.035), transparent),
    rgba(8, 11, 18, 0.44);
}

.workflow-card-mini {
  position: relative;
  min-width: 0;
  padding: 8px 9px;
  border-right: var(--border-subtle);
}

.workflow-card-mini:last-child {
  border-right: 0;
}

.workflow-card-mini:not(:last-child)::after {
  content: "->";
  position: absolute;
  right: -10px;
  top: 17px;
  z-index: 1;
  color: var(--color-text-disabled);
  font-family: var(--font-mono);
  font-size: 10px;
}

.workflow-node,
.overview-step__node {
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  border: 1px solid var(--color-accent-border);
  border-radius: 4px;
  background: var(--color-accent-muted);
  color: var(--color-accent-cyan);
  font-size: 10px;
}

.workflow-card-mini strong,
.workflow-card-mini small,
.overview-step strong,
.overview-step small {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workflow-card-mini strong {
  margin-top: 8px;
  font-size: 12px;
}

.workflow-card-mini small {
  margin-top: 4px;
  color: var(--color-text-disabled);
  font-size: 10px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  min-width: 0;
}

.data-source-note {
  margin: -3px 0 0;
  color: var(--color-text-disabled);
  font-size: 11px;
  line-height: 18px;
}

.data-source-note span {
  color: var(--color-text-secondary);
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(280px, 0.82fr) minmax(300px, 0.9fr);
  grid-auto-flow: dense;
  gap: 12px;
  min-width: 0;
  align-items: start;
}

.recent-runs-card {
  grid-column: span 2;
}

.workflow-overview-card,
.timeline-card,
.tools-card {
  grid-column: 3;
}

.provider-card,
.review-card,
.knowledge-card,
.prompts-card {
  min-height: 232px;
}

.run-table-head,
.run-row {
  display: grid;
  grid-template-columns: 86px minmax(0, 1.35fr) minmax(168px, 0.72fr) 86px 66px 72px;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.run-table-head {
  height: 30px;
  padding: 0 10px;
  border-bottom: var(--border-subtle);
  color: var(--color-text-disabled);
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.06em;
}

.run-table {
  min-width: 0;
}

.run-row {
  width: 100%;
  min-height: 54px;
  padding: 8px 10px;
  border: 0;
  border-bottom: var(--border-subtle);
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.run-row:last-child {
  border-bottom: 0;
}

.run-row:hover,
.prompt-list button:hover {
  background: var(--color-active-row);
}

.run-id,
.run-cost,
.reviewer,
.run-title strong,
.run-title small,
.prompt-list strong,
.prompt-list small,
.prompt-list em {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.run-id,
.run-cost {
  color: var(--color-text-secondary);
  font-size: 10px;
}

.reviewer {
  color: var(--color-text-secondary);
  font-size: 11px;
}

.run-title {
  min-width: 0;
}

.run-title strong,
.run-title small {
  display: block;
}

.run-title strong {
  color: var(--color-text-primary);
  font-size: 12px;
  line-height: 18px;
  font-weight: 600;
}

.run-title small {
  margin-top: 2px;
  color: var(--color-text-disabled);
  font-size: 10px;
}

.prompt-list,
.provider-list,
.review-list,
.knowledge-list,
.timeline-list,
.tools-list {
  display: grid;
  min-width: 0;
}

.prompt-list button {
  width: 100%;
  min-height: 48px;
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr) 38px;
  align-items: center;
  gap: 9px;
  padding: 8px 10px;
  border: 0;
  border-bottom: var(--border-subtle);
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.prompt-list button:last-child {
  border-bottom: 0;
}

.prompt-icon {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border: 1px solid var(--color-accent-border);
  border-radius: 4px;
  background: var(--color-accent-muted);
  color: var(--color-accent-cyan);
  font-size: 9px;
}

.prompt-list strong,
.prompt-list small {
  display: block;
}

.prompt-list strong {
  color: var(--color-text-primary);
  font-size: 12px;
}

.prompt-list small,
.prompt-list em {
  color: var(--color-text-disabled);
  font-size: 10px;
  font-style: normal;
}

.overview-flow {
  display: grid;
  gap: 8px;
}

.overview-step {
  position: relative;
  min-width: 0;
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 8px;
  padding: 8px;
  border: var(--border-subtle);
  border-radius: 6px;
  background: var(--color-card-soft);
}

.overview-step:not(:last-child)::after {
  content: "";
  position: absolute;
  left: 21px;
  top: 32px;
  bottom: -10px;
  width: 1px;
  background: var(--color-accent-border);
}

.overview-step strong {
  align-self: end;
  font-size: 12px;
}

.overview-step small {
  grid-column: 2;
  color: var(--color-text-disabled);
  font-size: 10px;
}

.provider-list {
  gap: 8px;
}

.provider-row {
  min-width: 0;
  padding: 10px;
  border: var(--border-subtle);
  border-radius: 6px;
  background: var(--color-card-soft);
}

.provider-row__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.provider-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.provider-metrics span {
  min-width: 0;
  display: block;
  padding-left: 8px;
  border-left: var(--border-subtle);
}

.provider-metrics strong,
.provider-metrics small {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.provider-metrics strong {
  color: var(--color-text-primary);
  font-family: var(--font-mono);
  font-size: 14px;
}

.provider-metrics small {
  margin-top: 3px;
  color: var(--color-text-disabled);
  font-size: 10px;
}

.review-row,
.knowledge-row,
.timeline-row,
.tool-row {
  min-width: 0;
  border-bottom: var(--border-subtle);
}

.review-row {
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 4px;
}

.review-row:last-child,
.knowledge-row:last-child,
.timeline-row:last-child,
.tool-row:last-child {
  border-bottom: 0;
}

.review-row div,
.knowledge-row div,
.timeline-row div {
  min-width: 0;
}

.review-row strong,
.review-row small,
.knowledge-row strong,
.knowledge-row small,
.knowledge-row p,
.timeline-row strong,
.timeline-row small,
.timeline-row time,
.tool-row strong,
.tool-row small {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-row strong,
.knowledge-row strong,
.timeline-row strong,
.tool-row strong {
  color: var(--color-text-primary);
  font-size: 12px;
}

.review-row small,
.knowledge-row small,
.timeline-row small,
.timeline-row time,
.tool-row small {
  color: var(--color-text-disabled);
  font-size: 10px;
}

.knowledge-row {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr);
  gap: 8px;
  padding: 9px 4px;
}

.knowledge-marker {
  width: 8px;
  height: 8px;
  margin-top: 5px;
  border-radius: 2px;
  background: var(--color-running);
}

.knowledge-row[data-reference="true"] .knowledge-marker {
  background: var(--color-success);
}

.knowledge-row p {
  margin: 5px 0 0;
  color: var(--color-text-secondary);
  font-size: 11px;
  line-height: 16px;
  white-space: normal;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.timeline-row {
  min-height: 44px;
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr) 48px;
  align-items: center;
  gap: 8px;
  padding: 7px 0;
}

.timeline-dot {
  width: 7px;
  height: 7px;
  border-radius: 2px;
  background: var(--timeline-color, var(--color-info));
}

.timeline-row[data-tone="success"] {
  --timeline-color: var(--color-success);
}

.timeline-row[data-tone="warning"] {
  --timeline-color: var(--color-warning);
}

.timeline-row[data-tone="danger"] {
  --timeline-color: var(--color-error);
}

.timeline-row[data-tone="running"] {
  --timeline-color: var(--color-running);
}

.tool-row {
  min-height: 42px;
  display: grid;
  grid-template-columns: 22px minmax(98px, 0.7fr) minmax(0, 1fr) 84px;
  align-items: center;
  gap: 8px;
  padding: 7px 0;
}

.tool-row > span {
  color: var(--color-text-disabled);
  font-size: 10px;
}

.tool-meter {
  height: 6px;
  overflow: hidden;
  border-radius: 2px;
  background: var(--color-bg);
}

.tool-meter i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--color-accent), var(--color-running));
}

.empty-state {
  min-height: 68px;
  display: grid;
  place-items: center;
  color: var(--color-text-disabled);
  text-align: center;
  border: 1px dashed var(--color-border);
  border-radius: 6px;
  background: var(--color-bg);
  font-size: 12px;
}

.dashboard-hero > *,
.hero-workflow > *,
.kpi-grid > *,
.dashboard-grid > *,
.run-table-head > *,
.run-row > *,
.provider-row__top > *,
.provider-metrics > *,
.tool-row > * {
  min-width: 0;
}

@media (max-width: 1360px) {
  .dashboard-hero {
    grid-template-columns: minmax(0, 1fr);
  }

  .hero-workflow {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }

  .kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .dashboard-grid {
    grid-template-columns: minmax(0, 1fr) minmax(300px, 0.75fr);
  }

  .recent-runs-card,
  .workflow-overview-card,
  .timeline-card,
  .tools-card {
    grid-column: auto;
  }
}

@media (max-width: 980px) {
  .dashboard-grid,
  .kpi-grid,
  .hero-workflow {
    grid-template-columns: minmax(0, 1fr);
  }

  .workflow-card-mini {
    border-right: 0;
    border-bottom: var(--border-subtle);
  }

  .workflow-card-mini:last-child {
    border-bottom: 0;
  }

  .workflow-card-mini:not(:last-child)::after,
  .run-table-head {
    display: none;
  }

  .run-row {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
