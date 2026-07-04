<script setup lang="ts">
import { computed, onMounted, shallowRef } from 'vue'
import { useRoute } from 'vue-router'
import { Check, Close, RefreshRight, Warning } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  fetchAgentRunTrace,
  fetchAgentRuns,
  fetchGenerationTraces,
  fetchGenerations,
  fetchKnowledgeReferences,
  fetchProjects,
} from '@/api/devflow'
import CodeBlock from '@/components/CodeBlock.vue'
import ProviderBadge from '@/components/ProviderBadge.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import type {
  AgentRun,
  AgentRunTrace,
  AgentStep,
  GenerationRecord,
  GenerationTrace,
  KnowledgeReference,
  ProjectContext,
  ToolCallRecord,
} from '@/types/domain'

type EvidenceTab = 'prompt' | 'fallback' | 'tools' | 'json'
type Tone = 'success' | 'warning' | 'danger' | 'running'

interface TimelineStep {
  key: string
  order: number
  label: string
  description: string
  status: string
  latencyMs?: number
  step?: AgentStep
  tone: Tone
}

const STEP_LABELS = [
  'Prompt 渲染',
  '知识命中',
  'Provider 选择',
  'Tool Call 模拟',
  '生成记录',
  '需要人工复核',
  '已确认 / 已驳回',
] as const

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
const evidenceExpanded = shallowRef(false)
const activeEvidenceTab = shallowRef<EvidenceTab>('prompt')
const route = useRoute()

const projectOptions = computed(() => [{ id: undefined, projectName: '全部项目' }, ...projects.value])
const selectedRun = computed(() => runs.value.find((run) => run.id === selectedRunId.value))
const selectedRecord = computed(() => {
  const recordId = trace.value?.run.generationRecordId || selectedRun.value?.generationRecordId
  return records.value.find((record) => record.id === recordId)
})
const selectedGenerationTrace = computed(() => generationTraces.value[0])
const toolCalls = computed(() => trace.value?.toolCalls || [])
const humanReview = computed(() => trace.value?.humanReviews[0])
const selectedTimelineStep = computed(() => {
  return timelineSteps.value.find((item) => item.key === selectedStepKey.value) || timelineSteps.value[0]
})
const selectedToolCalls = computed(() => {
  const stepId = selectedTimelineStep.value?.step?.id
  if (!stepId) return toolCalls.value
  return toolCalls.value.filter((tool) => tool.stepId === stepId)
})

const timelineSteps = computed<TimelineStep[]>(() => {
  const actualSteps = [...(trace.value?.steps || [])].sort((a, b) => a.stepOrder - b.stepOrder)
  return STEP_LABELS.map((label, index) => {
    const order = index + 1
    const step = actualSteps[index]
    const status = step?.status || fallbackStatus(order)
    return {
      key: step ? `step-${step.id}` : `derived-${order}`,
      order,
      label,
      description: step?.summary || fallbackDescription(label),
      status,
      latencyMs: step?.latencyMs,
      step,
      tone: statusTone(status),
    }
  })
})

const riskLevel = computed(() => {
  const status = normalizeStatus(selectedTimelineStep.value?.status)
  if (['FAILED', 'REJECTED', 'ERROR'].includes(status)) return { label: '高风险', tone: 'danger' as Tone }
  if (['READY_FOR_REVIEW', 'PENDING', 'WAITING_REVIEW', 'SAVED'].includes(status)) return { label: '待复核', tone: 'warning' as Tone }
  if (fallbackReason.value.includes('local-rule')) return { label: '本地演示', tone: 'warning' as Tone }
  return { label: '低风险', tone: 'success' as Tone }
})

const reviewStatus = computed(() => humanReview.value?.reviewStatus || selectedRecord.value?.status || 'PENDING')
const renderedPromptText = computed(() => {
  return selectedRecord.value?.renderedPrompt
    || selectedGenerationTrace.value?.renderedPromptSummary
    || '当前接口未返回完整 Rendered Prompt；此处只展示真实字段，不生成替代 Prompt。'
})
const fallbackReason = computed(() => {
  const provider = (trace.value?.run.providerName || selectedRecord.value?.providerName || '').toLowerCase()
  const model = (trace.value?.run.modelName || selectedRecord.value?.modelName || '').toLowerCase()
  if (provider.includes('local') || model.includes('local')) {
    return 'local-rule fallback；本地演示模式不连接真实 API Key。'
  }
  return selectedRecord.value?.errorMessage || selectedGenerationTrace.value?.errorMessage || '当前 Trace 未返回 fallback / error 字段。'
})
const coreReason = computed(() => {
  return humanReview.value?.comment
    || selectedRecord.value?.errorMessage
    || selectedTimelineStep.value?.description
    || '生成结果需要人工检查输出边界、状态机和可解释证据后再确认。'
})
const rawJson = computed(() => JSON.stringify({
  run: trace.value?.run,
  timeline: timelineSteps.value.map((item) => ({
    order: item.order,
    label: item.label,
    status: item.status,
    latencyMs: item.latencyMs,
    description: item.description,
  })),
  toolCalls: toolCalls.value,
  humanReviews: trace.value?.humanReviews,
  generationTrace: selectedGenerationTrace.value,
  knowledgeReferences: references.value,
  dataSource: 'Demo Data / 来源：本地 Trace 接口与本地指标快照',
}, null, 2))
const toolIoJson = computed(() => JSON.stringify(selectedToolCalls.value.map((tool) => ({
  toolName: tool.toolName,
  inputSummary: tool.inputSummary || '未记录',
  outputSummary: tool.outputSummary || '未记录',
  status: tool.status,
  latencyMs: tool.latencyMs,
})), null, 2))
const evidenceCode = computed(() => {
  if (activeEvidenceTab.value === 'prompt') return renderedPromptText.value
  if (activeEvidenceTab.value === 'fallback') return fallbackReason.value
  if (activeEvidenceTab.value === 'tools') return toolIoJson.value
  return rawJson.value
})
const evidenceLanguage = computed(() => {
  if (activeEvidenceTab.value === 'tools' || activeEvidenceTab.value === 'json') return 'json'
  if (activeEvidenceTab.value === 'prompt') return 'markdown'
  return 'text'
})
const evidenceTitle = computed(() => {
  const map: Record<EvidenceTab, string> = {
    prompt: 'Rendered Prompt',
    fallback: 'Fallback Reason',
    tools: 'Tool I/O',
    json: 'Raw JSON',
  }
  return map[activeEvidenceTab.value]
})

async function loadRuns() {
  loading.value = true
  try {
    const queryGenerationId = Number(route.query.generationRecordId)
    const [recordData, runData] = await Promise.all([
      fetchGenerations(),
      queryGenerationId ? fetchAgentRuns({ generationRecordId: queryGenerationId }) : fetchAgentRuns({ projectId: selectedProjectId.value }),
    ])
    records.value = recordData
    runs.value = runData
    selectedRunId.value = runs.value.find((run) => run.id === selectedRunId.value)?.id || runs.value[0]?.id
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
  await loadRuns()
}

async function selectRun(id: number) {
  selectedRunId.value = id
  await loadTrace()
}

function fallbackStatus(order: number) {
  if (order <= 4 && trace.value) return 'SUCCESS'
  if (order === 6) return humanReview.value?.reviewStatus || 'READY_FOR_REVIEW'
  if (order === 7) return selectedRecord.value?.status || 'PENDING'
  return selectedRecord.value?.status || 'PENDING'
}

function fallbackDescription(label: string) {
  const map: Record<string, string> = {
    'Prompt 渲染': selectedGenerationTrace.value?.renderedPromptSummary || '接收生成请求并绑定项目上下文。',
    '知识命中': references.value.length ? `命中 ${references.value.length} 条知识引用，用于生成参考。` : '本次运行未返回知识引用，显示为未采集。',
    'Provider 选择': `${trace.value?.run.providerName || selectedRecord.value?.providerName || 'local-rule fallback'} 负责本次生成路由。`,
    'Tool Call 模拟': toolCalls.value.length ? `记录 ${toolCalls.value.length} 条 Tool Call 输入输出摘要。` : '当前 Trace 未返回 Tool Call 明细。',
    '生成记录': selectedRecord.value ? `生成记录 generation_${selectedRecord.value.id} 已写入状态机。` : '当前运行未关联生成记录。',
    '需要人工复核': humanReview.value?.comment || '生成结果停在 Human Review，等待人工确认。',
    '已确认 / 已驳回': selectedRecord.value ? statusText(selectedRecord.value.status) : '尚未完成最终决策。',
  }
  return map[label] || '当前步骤无补充说明。'
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
    SAVED: '已保存',
    CONFIRMED: '已确认',
    SUCCESS: '成功',
    FAILED: '失败',
    REJECTED: '已驳回',
  }
  return labels[normalizeStatus(status)] || status || '未记录'
}

function statusTone(status?: string): Tone {
  const normalized = normalizeStatus(status)
  if (['CONFIRMED', 'SAVED', 'SUCCESS', 'PASSED'].includes(normalized)) return 'success'
  if (['FAILED', 'REJECTED', 'ERROR'].includes(normalized)) return 'danger'
  if (['GENERATING', 'RUNNING'].includes(normalized)) return 'running'
  return 'warning'
}

function formatDuration(value?: number) {
  const ms = Math.max(value || 0, 0)
  if (ms >= 1000) return `${(ms / 1000).toFixed(ms >= 10000 ? 1 : 2)}s`
  return `${ms}ms`
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '未记录'
}

function shortText(value?: string, limit = 42) {
  const clean = (value || '未命名运行').replace(/\s+/g, ' ').trim()
  return clean.length > limit ? `${clean.slice(0, limit)}...` : clean
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : '加载 Trace Evidence 失败'
}

onMounted(loadPageData)
</script>

<template>
  <div class="agent-trace-page trace-redesign" v-loading="loading">
    <header class="trace-hero">
      <div class="trace-hero__copy">
        <p class="eyebrow mono">TRACE EVIDENCE</p>
        <h1>Trace Evidence</h1>
        <p>可解释执行链路与 Human Review 工作台</p>
        <div class="workflow-chain mono" aria-label="执行链路">
          Prompt <span>-></span> Provider <span>-></span> Trace <span>-></span> Tool Call <span>-></span> Human Review
        </div>
      </div>
      <div class="trace-hero__status" aria-label="数据边界">
        <span><i></i>Demo Data</span>
        <span><i></i>本地模式</span>
        <span><i></i>local-rule fallback</span>
        <span><i></i>OpenAI-compatible 可选</span>
      </div>
    </header>

    <main class="trace-stage">
      <aside class="trace-panel run-panel">
        <header class="panel-heading">
          <div>
            <span class="panel-eyebrow mono">RUN LIST</span>
            <h2>运行记录</h2>
          </div>
          <button class="icon-button" type="button" @click="loadRuns">
            <el-icon><RefreshRight /></el-icon>
            刷新
          </button>
        </header>

        <el-select v-model="selectedProjectId" class="project-filter" placeholder="全部项目" @change="loadRuns">
          <el-option v-for="project in projectOptions" :key="project.id ?? 'all'" :label="project.projectName" :value="project.id" />
        </el-select>

        <div class="run-list">
          <button
            v-for="run in runs"
            :key="run.id"
            class="run-item"
            :class="{ active: selectedRunId === run.id }"
            type="button"
            @click="selectRun(run.id)"
          >
            <i class="tone-dot" :data-tone="statusTone(run.status)" aria-hidden="true"></i>
            <span>
              <strong>{{ shortText(run.title) }}</strong>
              <small class="mono">trace_{{ run.id }} / {{ formatTime(run.createdAt) }}</small>
            </span>
            <StatusBadge :status="run.status" :label="statusText(run.status)" />
          </button>
          <div v-if="!runs.length" class="empty-state">暂无运行记录。请先在 Workbench 运行一次本地 Demo。</div>
        </div>
      </aside>

      <section class="trace-panel timeline-panel">
        <header class="panel-heading timeline-heading">
          <div>
            <span class="panel-eyebrow mono">TRACE TIMELINE</span>
            <h2>执行时间线</h2>
            <p>只保留关键步骤、状态和耗时，让执行证据成为截图主角。</p>
          </div>
          <ProviderBadge :provider="trace?.run.providerName || selectedRecord?.providerName || 'local-rule fallback'" :model="trace?.run.modelName || selectedRecord?.modelName" />
        </header>

        <div class="timeline-context">
          <div>
            <span>当前运行</span>
            <strong>{{ selectedRun?.title || '未选择运行' }}</strong>
            <small>{{ selectedRun?.goal || 'Demo Data / 来源：本地 Trace 接口' }}</small>
          </div>
          <div class="context-metrics">
            <span><b class="mono">{{ references.length }}</b><small>知识命中</small></span>
            <span><b class="mono">{{ toolCalls.length }}</b><small>Tool Call</small></span>
            <span><b class="mono">{{ formatDuration(trace?.run.latencyMs || selectedRecord?.costTimeMs) }}</b><small>耗时</small></span>
          </div>
        </div>

        <div class="timeline-list">
          <article
            v-for="step in timelineSteps"
            :key="step.key"
            class="timeline-step"
            :class="{ active: selectedTimelineStep?.key === step.key }"
            @click="selectedStepKey = step.key"
          >
            <span class="step-number mono">{{ String(step.order).padStart(2, '0') }}</span>
            <span class="step-rail" aria-hidden="true"><i :data-tone="step.tone"></i></span>
            <div class="step-copy">
              <h3>{{ step.label }}</h3>
              <p>{{ step.description }}</p>
            </div>
            <StatusBadge :status="step.status" :label="statusText(step.status)" />
            <span class="step-latency mono">{{ formatDuration(step.latencyMs) }}</span>
          </article>
        </div>
      </section>

      <aside class="trace-panel decision-panel">
        <header class="panel-heading">
          <div>
            <span class="panel-eyebrow mono">INSPECTOR</span>
            <h2>步骤详情</h2>
            <p>人工复核决策区</p>
          </div>
        </header>

        <section class="current-step-card">
          <span class="panel-eyebrow mono">CURRENT STEP</span>
          <h3>{{ selectedTimelineStep ? `${String(selectedTimelineStep.order).padStart(2, '0')} ${selectedTimelineStep.label}` : '未选择步骤' }}</h3>
          <p>{{ selectedTimelineStep?.description || '暂无步骤说明。' }}</p>
        </section>

        <div class="decision-actions" aria-label="人工复核决策">
          <button class="decision-button approve" type="button" disabled>
            <el-icon><Check /></el-icon>
            通过
          </button>
          <button class="decision-button request" type="button" disabled>
            <el-icon><Warning /></el-icon>
            要求修改
          </button>
          <button class="decision-button reject" type="button" disabled>
            <el-icon><Close /></el-icon>
            驳回
          </button>
        </div>

        <dl class="decision-grid">
          <div>
            <dt>风险等级</dt>
            <dd><span class="risk-pill" :data-tone="riskLevel.tone">{{ riskLevel.label }}</span></dd>
          </div>
          <div>
            <dt>Provider</dt>
            <dd><ProviderBadge :provider="trace?.run.providerName || selectedRecord?.providerName || 'local-rule fallback'" :model="trace?.run.modelName || selectedRecord?.modelName" /></dd>
          </div>
          <div>
            <dt>Token</dt>
            <dd class="mono">{{ selectedRecord?.totalTokens || '未采集' }}</dd>
          </div>
          <div>
            <dt>Fallback</dt>
            <dd>{{ fallbackReason }}</dd>
          </div>
          <div>
            <dt>Human Review 状态</dt>
            <dd><StatusBadge :status="reviewStatus" :label="statusText(reviewStatus)" /></dd>
          </div>
          <div>
            <dt>核心原因</dt>
            <dd>{{ coreReason }}</dd>
          </div>
        </dl>

      </aside>
    </main>

    <section class="evidence-drawer" :class="{ expanded: evidenceExpanded }">
      <header class="evidence-header">
        <div>
          <span class="panel-eyebrow mono">EVIDENCE DRAWER</span>
          <h2>原始证据</h2>
        </div>
        <div class="evidence-controls">
          <button type="button" :class="{ active: activeEvidenceTab === 'prompt' }" @click="activeEvidenceTab = 'prompt'">Rendered Prompt</button>
          <button type="button" :class="{ active: activeEvidenceTab === 'fallback' }" @click="activeEvidenceTab = 'fallback'">Fallback Reason</button>
          <button type="button" :class="{ active: activeEvidenceTab === 'tools' }" @click="activeEvidenceTab = 'tools'">Tool I/O</button>
          <button type="button" :class="{ active: activeEvidenceTab === 'json' }" @click="activeEvidenceTab = 'json'">Raw JSON</button>
          <button class="drawer-toggle" type="button" @click="evidenceExpanded = !evidenceExpanded">
            {{ evidenceExpanded ? '收起' : '展开证据' }}
          </button>
        </div>
      </header>
      <CodeBlock v-if="evidenceExpanded" :title="evidenceTitle" :language="evidenceLanguage" :code="evidenceCode" />
    </section>
  </div>
</template>

<style scoped>
.trace-redesign {
  --trace-bg-page: #05080d;
  --trace-panel-low: #0b1119;
  --trace-panel-main: #101923;
  --trace-panel-high: #142231;
  --trace-line: rgba(129, 154, 171, 0.18);
  --trace-line-strong: rgba(94, 234, 212, 0.24);
  --trace-text: #eff7f6;
  --trace-muted: #9fb1bf;
  --trace-dim: #6f8291;
  --trace-mint: #38e2ad;
  --trace-blue: #6bb7ff;
  --trace-amber: #f5b84c;
  --trace-red: #f87171;
  display: grid;
  gap: 16px;
  min-width: 0;
  color: var(--trace-text);
}

.trace-hero {
  min-width: 0;
  min-height: 132px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, auto);
  align-items: end;
  gap: 24px;
  padding: 22px 24px;
  border-radius: var(--radius-card);
  background:
    linear-gradient(135deg, rgba(56, 226, 173, 0.12), rgba(78, 161, 255, 0.06) 52%, transparent),
    var(--trace-panel-main);
  box-shadow: inset 0 0 0 1px var(--trace-line), var(--shadow-card);
}

.trace-hero__copy {
  min-width: 0;
}

.eyebrow,
.panel-eyebrow {
  display: block;
  margin: 0;
  color: var(--trace-mint);
  font-size: 11px;
  letter-spacing: 0.04em;
}

.trace-hero h1,
.trace-hero p,
.panel-heading h2,
.panel-heading p {
  margin: 0;
}

.trace-hero h1 {
  margin-top: 8px;
  font-size: 24px;
  line-height: 32px;
  font-weight: 720;
}

.trace-hero p:not(.eyebrow) {
  margin-top: 8px;
  color: #c8d7df;
  font-size: 14px;
  line-height: 22px;
}

.workflow-chain {
  width: fit-content;
  max-width: 100%;
  margin-top: 16px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  background: rgba(5, 8, 13, 0.62);
  color: #d7e4e7;
  font-size: 12px;
  line-height: 18px;
}

.workflow-chain span {
  color: var(--trace-mint);
  margin: 0 6px;
}

.trace-hero__status {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.trace-hero__status span {
  height: 30px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 10px;
  border-radius: var(--radius-md);
  background: rgba(5, 8, 13, 0.58);
  color: #c0ccd4;
  font-size: 12px;
  white-space: nowrap;
}

.trace-hero__status i,
.tone-dot,
.step-rail i {
  width: 7px;
  height: 7px;
  border-radius: 2px;
  background: var(--trace-mint);
  flex: 0 0 auto;
}

.trace-stage {
  display: grid;
  grid-template-columns: minmax(270px, 0.24fr) minmax(520px, 1fr) minmax(340px, 0.31fr);
  gap: 18px;
  min-width: 0;
  align-items: stretch;
}

.trace-panel {
  min-width: 0;
  min-height: 600px;
  border-radius: var(--radius-card);
  background: var(--trace-panel-low);
  box-shadow: inset 0 0 0 1px var(--trace-line), var(--shadow-card);
  overflow: hidden;
}

.timeline-panel {
  background: var(--trace-panel-main);
  box-shadow: inset 0 0 0 1px var(--trace-line-strong), 0 18px 48px rgba(0, 0, 0, 0.24);
}

.panel-heading {
  min-height: 72px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 16px 18px;
  border-bottom: 1px solid rgba(129, 154, 171, 0.12);
}

.panel-heading h2 {
  margin-top: 4px;
  color: var(--trace-text);
  font-size: 16px;
  line-height: 22px;
  font-weight: 700;
}

.panel-heading p {
  margin-top: 5px;
  color: var(--trace-muted);
  font-size: 13px;
  line-height: 20px;
}

.icon-button {
  height: 32px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 10px;
  border: 0;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.06);
  color: #d7e4e7;
  cursor: pointer;
  font-size: 13px;
}

.project-filter {
  width: calc(100% - 36px);
  margin: 0 18px 12px;
}

.run-list {
  display: grid;
  gap: 8px;
  max-height: 510px;
  padding: 0 12px 16px;
  overflow: auto;
}

.run-item {
  width: 100%;
  min-height: 70px;
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: 0;
  border-radius: var(--radius-md);
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.run-item:hover,
.run-item.active {
  background: rgba(255, 255, 255, 0.055);
}

.run-item.active {
  box-shadow: inset 3px 0 0 var(--trace-mint);
}

.run-item strong,
.run-item small {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.run-item strong {
  color: var(--trace-text);
  font-size: 13px;
  line-height: 18px;
}

.run-item small {
  margin-top: 6px;
  color: var(--trace-dim);
  font-size: 11px;
}

.tone-dot[data-tone="warning"],
.step-rail i[data-tone="warning"] {
  background: var(--trace-amber);
}

.tone-dot[data-tone="danger"],
.step-rail i[data-tone="danger"] {
  background: var(--trace-red);
}

.tone-dot[data-tone="running"],
.step-rail i[data-tone="running"] {
  background: var(--trace-blue);
}

.timeline-heading {
  align-items: center;
  padding-bottom: 16px;
}

.timeline-context {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 252px;
  gap: 12px;
  margin: 12px 18px 12px;
  padding: 12px;
  border-radius: var(--radius-md);
  background: rgba(5, 8, 13, 0.52);
}

.timeline-context span,
.timeline-context strong,
.timeline-context small {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.timeline-context span,
.timeline-context small {
  color: var(--trace-dim);
  font-size: 12px;
  line-height: 18px;
}

.timeline-context > div:first-child small {
  display: -webkit-box;
  max-height: 40px;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.timeline-context strong {
  margin-top: 4px;
  color: var(--trace-text);
  font-size: 15px;
  line-height: 22px;
  white-space: nowrap;
}

.context-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  border-radius: var(--radius-md);
  background: rgba(129, 154, 171, 0.12);
}

.context-metrics span {
  padding: 8px 7px;
  background: rgba(20, 34, 49, 0.74);
  text-align: center;
}

.context-metrics b {
  display: block;
  color: #dff9f3;
  font-size: 15px;
}

.context-metrics small {
  margin-top: 4px;
  font-size: 11px;
}

.timeline-list {
  display: grid;
  gap: 8px;
  padding: 0 18px 18px;
}

.timeline-step {
  min-width: 0;
  min-height: 56px;
  display: grid;
  grid-template-columns: 42px 18px minmax(0, 1fr) auto 64px;
  align-items: center;
  gap: 12px;
  padding: 9px 12px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.034);
  cursor: pointer;
}

.timeline-step.active {
  background: rgba(56, 226, 173, 0.085);
}

.step-number {
  color: #eaf4f3;
  font-size: 15px;
  font-weight: 700;
}

.step-rail {
  position: relative;
  height: 100%;
  display: grid;
  place-items: center;
}

.step-rail::after {
  content: "";
  position: absolute;
  top: 36px;
  bottom: -20px;
  width: 1px;
  background: rgba(129, 154, 171, 0.2);
}

.timeline-step:last-child .step-rail::after {
  display: none;
}

.step-rail i {
  position: relative;
  z-index: 1;
}

.step-copy {
  min-width: 0;
}

.step-copy h3,
.step-copy p {
  margin: 0;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.step-copy h3 {
  color: var(--trace-text);
  font-size: 14px;
  line-height: 20px;
}

.step-copy p {
  margin-top: 2px;
  color: var(--trace-muted);
  font-size: 13px;
  line-height: 19px;
  white-space: nowrap;
}

.step-latency {
  color: var(--trace-dim);
  font-size: 12px;
  text-align: right;
}

.decision-panel {
  background: #0e151f;
}

.current-step-card {
  margin: 16px 18px 12px;
  padding: 14px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.045);
}

.current-step-card h3,
.current-step-card p {
  margin: 0;
}

.current-step-card h3 {
  margin-top: 6px;
  color: var(--trace-text);
  font-size: 16px;
  line-height: 24px;
}

.current-step-card p {
  margin-top: 6px;
  color: var(--trace-muted);
  font-size: 13px;
  line-height: 20px;
  display: -webkit-box;
  max-height: 40px;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.decision-grid {
  display: grid;
  gap: 8px;
  margin: 0 18px;
}

.decision-grid div {
  min-width: 0;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(129, 154, 171, 0.12);
}

.decision-grid dt,
.decision-grid dd {
  min-width: 0;
}

.decision-grid dt {
  color: var(--trace-dim);
  font-size: 12px;
  line-height: 18px;
}

.decision-grid dd {
  margin: 4px 0 0;
  color: var(--trace-text);
  font-size: 13px;
  line-height: 19px;
}

.decision-grid div:nth-child(4) dd,
.decision-grid div:nth-child(6) dd {
  display: -webkit-box;
  max-height: 58px;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.risk-pill {
  height: 24px;
  display: inline-flex;
  align-items: center;
  padding: 0 9px;
  border-radius: var(--radius-md);
  background: rgba(245, 184, 76, 0.14);
  color: var(--trace-amber);
  font-size: 12px;
}

.risk-pill[data-tone="success"] {
  background: rgba(56, 226, 173, 0.12);
  color: var(--trace-mint);
}

.risk-pill[data-tone="danger"] {
  background: rgba(248, 113, 113, 0.14);
  color: var(--trace-red);
}

.decision-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin: 0 18px 14px;
}

.decision-button {
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: 0;
  border-radius: var(--radius-md);
  color: #05100d;
  font-size: 13px;
  font-weight: 700;
}

.decision-button.approve {
  background: var(--trace-mint);
}

.decision-button.request {
  background: rgba(245, 184, 76, 0.9);
}

.decision-button.reject {
  background: rgba(248, 113, 113, 0.9);
}

.decision-button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.evidence-drawer {
  min-width: 0;
  border-radius: var(--radius-card);
  background: rgba(11, 17, 25, 0.72);
  box-shadow: inset 0 0 0 1px rgba(129, 154, 171, 0.13);
  overflow: hidden;
}

.evidence-header {
  min-height: 54px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 10px 14px;
}

.evidence-header h2 {
  margin: 2px 0 0;
  color: var(--trace-text);
  font-size: 15px;
  line-height: 20px;
}

.evidence-controls {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.evidence-controls button {
  height: 28px;
  padding: 0 9px;
  border: 0;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.055);
  color: var(--trace-muted);
  cursor: pointer;
  font-size: 12px;
}

.evidence-controls button.active {
  background: rgba(78, 161, 255, 0.16);
  color: #d8ecff;
}

.evidence-controls .drawer-toggle {
  background: rgba(56, 226, 173, 0.12);
  color: #dff9f3;
}

.evidence-drawer.expanded :deep(.code-block) {
  border: 0;
  border-top: 1px solid rgba(129, 154, 171, 0.13);
  border-radius: 0;
}

.evidence-drawer.expanded :deep(.code-block pre) {
  max-height: 230px;
}

.empty-state {
  min-height: 90px;
  display: grid;
  place-items: center;
  padding: 14px;
  color: var(--trace-dim);
  font-size: 13px;
  text-align: center;
}

.trace-redesign > *,
.trace-hero > *,
.trace-stage > *,
.panel-heading > *,
.run-item > *,
.timeline-context > *,
.context-metrics > *,
.timeline-step > *,
.decision-grid > *,
.evidence-header > *,
.evidence-controls > * {
  min-width: 0;
}

@media (max-width: 1280px) {
  .trace-stage {
    grid-template-columns: minmax(260px, 0.36fr) minmax(0, 1fr);
  }

  .decision-panel {
    grid-column: 1 / -1;
    min-height: 0;
  }
}

@media (max-width: 920px) {
  .trace-hero,
  .trace-stage,
  .timeline-context,
  .context-metrics,
  .timeline-step {
    grid-template-columns: minmax(0, 1fr);
  }

  .trace-hero__status,
  .evidence-controls {
    justify-content: flex-start;
  }

  .trace-panel {
    min-height: auto;
  }

  .step-rail {
    display: none;
  }
}
</style>
