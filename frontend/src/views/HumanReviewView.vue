<script setup lang="ts">
import { computed, onMounted, shallowRef } from 'vue'
import { Check, Close, RefreshRight, Warning } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  confirmGeneration,
  fetchAgentRunTrace,
  fetchAgentRuns,
  fetchGenerations,
  saveGeneration,
} from '@/api/devflow'
import CodeBlock from '@/components/CodeBlock.vue'
import ProviderBadge from '@/components/ProviderBadge.vue'
import SectionCard from '@/components/SectionCard.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import type { AgentRun, AgentRunTrace, GenerationRecord, HumanReview } from '@/types/domain'

interface ReviewQueueItem {
  key: string
  record: GenerationRecord
  run?: AgentRun
  review?: HumanReview
  trace?: AgentRunTrace
}

const loading = shallowRef(false)
const actionLoading = shallowRef(false)
const records = shallowRef<GenerationRecord[]>([])
const runs = shallowRef<AgentRun[]>([])
const traces = shallowRef<AgentRunTrace[]>([])
const selectedKey = shallowRef('')
const reviewReason = shallowRef('本地 Demo 复核：确认输出边界、状态机和可解释性后再通过。')

const queueItems = computed<ReviewQueueItem[]>(() => {
  const runByRecord = new Map<number, AgentRun>()
  runs.value.forEach((run) => {
    if (run.generationRecordId && !runByRecord.has(run.generationRecordId)) runByRecord.set(run.generationRecordId, run)
  })

  return records.value
    .slice()
    .sort((a, b) => toTime(b.updatedAt || b.createdAt) - toTime(a.updatedAt || a.createdAt))
    .map((record) => {
      const run = runByRecord.get(record.id)
      const trace = run ? traces.value.find((item) => item.run.id === run.id) : undefined
      return {
        key: `record-${record.id}`,
        record,
        run,
        trace,
        review: trace?.humanReviews[0],
      }
    })
})
const selectedItem = computed(() => queueItems.value.find((item) => item.key === selectedKey.value) || queueItems.value[0])
const reviewMetrics = computed(() => {
  const pending = queueItems.value.filter((item) => isPendingStatus(item.record.status) || isPendingStatus(item.review?.reviewStatus)).length
  const confirmed = queueItems.value.filter((item) => normalizeStatus(item.record.status) === 'CONFIRMED').length
  const failed = queueItems.value.filter((item) => ['FAILED', 'REJECTED'].includes(normalizeStatus(item.record.status))).length
  return [
    { label: '复核队列', value: queueItems.value.length, code: 'Queue', tone: 'accent' as const },
    { label: '待人工复核', value: pending, code: 'Pending', tone: 'warning' as const },
    { label: '已确认', value: confirmed, code: 'Confirmed', tone: 'success' as const },
    { label: '失败 / 驳回', value: failed, code: 'Risk', tone: 'danger' as const },
  ]
})
const riskTags = computed(() => buildRiskTags(selectedItem.value))
const statusHistory = computed(() => {
  const item = selectedItem.value
  if (!item) return []
  return [
    {
      key: 'record',
      title: '生成记录',
      detail: `Generation Record #${item.record.id}`,
      status: item.record.status,
      time: item.record.createdAt,
    },
    item.run && {
      key: 'run',
      title: 'Agent Run',
      detail: `trace_${item.run.id} / ${item.run.providerName || item.record.providerName || 'local-rule fallback'}`,
      status: item.run.status,
      time: item.run.completedAt || item.run.startedAt || item.run.createdAt,
    },
    item.review && {
      key: 'review',
      title: 'Human Review checkpoint',
      detail: item.review.comment || '已进入人工复核停点',
      status: item.review.reviewStatus,
      time: item.review.updatedAt || item.review.createdAt,
    },
  ].filter(Boolean) as Array<{ key: string; title: string; detail: string; status: string; time?: string }>
})
const canApprove = computed(() => {
  const status = normalizeStatus(selectedItem.value?.record.status)
  return ['READY_FOR_REVIEW', 'SAVED'].includes(status)
})

async function loadPage() {
  loading.value = true
  try {
    const [recordData, runData] = await Promise.all([
      fetchGenerations(),
      fetchAgentRuns(),
    ])
    records.value = recordData
    runs.value = runData
    const recentRuns = runData.slice(0, 8)
    const traceRows = await Promise.all(recentRuns.map((run) => fetchAgentRunTrace(run.id).catch(() => undefined)))
    traces.value = traceRows.filter((trace): trace is AgentRunTrace => Boolean(trace))
    selectedKey.value = queueItems.value.find((item) => item.key === selectedKey.value)?.key || queueItems.value[0]?.key || ''
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    loading.value = false
  }
}

async function approveSelected() {
  const item = selectedItem.value
  if (!item || !canApprove.value) return
  actionLoading.value = true
  try {
    const status = normalizeStatus(item.record.status)
    if (status === 'READY_FOR_REVIEW') {
      await saveGeneration(item.record.id)
      ElMessage.success('已保存生成记录，请再次通过以完成确认。')
    } else if (status === 'SAVED') {
      await confirmGeneration(item.record.id)
      ElMessage.success('已通过 Human Review 并确认。')
    }
    await loadPage()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    actionLoading.value = false
  }
}

function selectItem(key: string) {
  selectedKey.value = key
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

function isPendingStatus(status?: string) {
  return ['READY_FOR_REVIEW', 'WAITING_REVIEW', 'PENDING', 'DRAFT', 'SAVED'].includes(normalizeStatus(status))
}

function buildRiskTags(item?: ReviewQueueItem) {
  if (!item) return []
  const tags: Array<{ label: string; status: string }> = []
  if (isPendingStatus(item.record.status)) tags.push({ label: '需人工确认', status: 'READY_FOR_REVIEW' })
  if ((item.record.providerName || item.run?.providerName || '').toLowerCase().includes('local')) tags.push({ label: 'local-rule fallback', status: 'PENDING' })
  if (!item.trace?.toolCalls.length) tags.push({ label: 'Tool Call 未采集', status: 'DRAFT' })
  if (item.record.errorMessage) tags.push({ label: 'Provider Error', status: 'FAILED' })
  if (!item.record.totalTokens) tags.push({ label: 'Token 未采集', status: 'PENDING' })
  return tags
}

function shortText(value?: string, limit = 86) {
  const clean = (value || '未记录').replace(/\s+/g, ' ').trim()
  return clean.length > limit ? `${clean.slice(0, limit)}...` : clean
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '未记录'
}

function formatDuration(value?: number) {
  const ms = Math.max(value || 0, 0)
  if (ms >= 1000) return `${(ms / 1000).toFixed(ms >= 10000 ? 1 : 2)}s`
  return `${ms}ms`
}

function toTime(value?: string) {
  return value ? new Date(value).getTime() || 0 : 0
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : 'Human Review 操作失败'
}

onMounted(loadPage)
</script>

<template>
  <div class="human-review-page" v-loading="loading">
    <header class="review-header">
      <div>
        <p class="eyebrow mono">HUMAN REVIEW / local demo</p>
        <h2>Human Review</h2>
        <p>复核队列、风险标签、Artifact 预览、决策面板与状态历史集中展示，突出 Human-in-the-loop 和 AI 输出可解释性。</p>
      </div>
      <div class="review-source">
        <span><i></i>Demo Data / 来源：本地接口</span>
        <span><i></i>不包装为生产审核系统</span>
      </div>
    </header>

    <section class="review-metrics">
      <article v-for="metric in reviewMetrics" :key="metric.code" class="review-metric" :data-tone="metric.tone">
        <span class="mono">{{ metric.code }}</span>
        <strong class="mono">{{ metric.value }}</strong>
        <small>{{ metric.label }}</small>
      </article>
    </section>

    <main class="review-console">
      <aside class="queue-panel">
        <SectionCard title="复核队列" subtitle="来自 /api/generations 与 /api/agent-runs" eyebrow="Queue">
          <div class="queue-list">
            <button
              v-for="item in queueItems"
              :key="item.key"
              type="button"
              class="queue-row"
              :class="{ active: selectedItem?.key === item.key }"
              @click="selectItem(item.key)"
            >
              <span class="queue-dot" :data-status="normalizeStatus(item.record.status)"></span>
              <span>
                <strong>{{ shortText(item.record.inputSummary || item.record.inputContent, 48) }}</strong>
                <small class="mono">generation_{{ item.record.id }} / trace_{{ item.run?.id || '未关联' }}</small>
              </span>
              <StatusBadge :status="item.review?.reviewStatus || item.record.status" :label="statusText(item.review?.reviewStatus || item.record.status)" />
            </button>
            <div v-if="!queueItems.length" class="empty-state">暂无可复核记录。</div>
          </div>
        </SectionCard>
      </aside>

      <section class="artifact-panel">
        <SectionCard title="Artifact 预览" subtitle="只展示后端真实生成内容，不生成替代结果" eyebrow="Artifact Preview">
          <template #actions>
            <ProviderBadge :provider="selectedItem?.record.providerName || selectedItem?.run?.providerName || 'local-rule fallback'" :model="selectedItem?.record.modelName || selectedItem?.run?.modelName" />
          </template>
          <div class="artifact-summary">
            <div>
              <span>输入摘要</span>
              <strong>{{ selectedItem?.record.inputSummary || '未记录' }}</strong>
            </div>
            <div>
              <span>状态</span>
              <StatusBadge :status="selectedItem?.record.status || 'PENDING'" :label="statusText(selectedItem?.record.status)" />
            </div>
            <div>
              <span>耗时</span>
              <strong class="mono">{{ formatDuration(selectedItem?.record.costTimeMs) }}</strong>
            </div>
            <div>
              <span>Token</span>
              <strong class="mono">{{ selectedItem?.record.totalTokens || '未采集' }}</strong>
            </div>
          </div>
          <CodeBlock title="生成结果预览" language="markdown" :code="selectedItem?.record.outputContent || '未选择生成记录。'" />
        </SectionCard>
      </section>

      <aside class="decision-panel">
        <SectionCard title="人工复核决策 / 复核结果" subtitle="通过沿用现有 save/confirm；Request Changes / Reject / 重新生成为 demo disabled 控制" eyebrow="Decision">
          <span class="decision-label">风险标签</span>
          <div class="risk-tags">
            <StatusBadge v-for="tag in riskTags" :key="tag.label" :status="tag.status" :label="tag.label" />
            <span v-if="!riskTags.length" class="mini-empty">暂无风险标签。</span>
          </div>

          <div class="review-result-line">
            <span>复核结果</span>
            <strong>{{ statusText(selectedItem?.review?.reviewStatus || selectedItem?.record.status) }}</strong>
          </div>

          <div class="review-reason">
            <span>复核原因</span>
            <el-input v-model="reviewReason" type="textarea" :autosize="{ minRows: 4, maxRows: 6 }" />
          </div>

          <div class="decision-actions">
            <el-button type="primary" :icon="Check" :loading="actionLoading" :disabled="!canApprove" @click="approveSelected">通过</el-button>
            <el-button :icon="Warning" disabled title="当前后端未提供 Request Changes API">要求修改</el-button>
            <el-button :icon="Close" disabled title="当前后端未提供 Reject API">驳回</el-button>
            <el-button :icon="RefreshRight" disabled title="请返回 Workbench 重新运行真实生成请求">重新生成</el-button>
          </div>

          <div class="why-review">
            <strong>为什么需要人工复核</strong>
            <p>DevFlow 只把 AI 生成结果推进到可审核 Artifact，不自动提交代码、不自动合并、不把 local-rule 当作真实 LLM。复核人需要检查输出边界、风险标签、Trace 证据和知识引用后再确认。</p>
          </div>
        </SectionCard>
      </aside>
    </main>

    <section class="review-bottom">
      <SectionCard title="状态历史" subtitle="Generation Record / Agent Run / Human Review checkpoint" eyebrow="State History">
        <div class="history-list">
          <article v-for="row in statusHistory" :key="row.key">
            <span class="queue-dot" :data-status="normalizeStatus(row.status)"></span>
            <div>
              <strong>{{ row.title }}</strong>
              <small>{{ row.detail }}</small>
            </div>
            <StatusBadge :status="row.status" :label="statusText(row.status)" />
            <time class="mono">{{ formatTime(row.time) }}</time>
          </article>
          <div v-if="!statusHistory.length" class="empty-state">暂无状态历史。</div>
        </div>
      </SectionCard>

      <SectionCard title="执行证据" subtitle="Trace、Tool Call 与复核意见摘要" eyebrow="Evidence">
        <div class="evidence-grid">
          <div><span>Trace ID</span><strong class="mono">trace_{{ selectedItem?.run?.id || '未关联' }}</strong></div>
          <div><span>Tool Call</span><strong class="mono">{{ selectedItem?.trace?.toolCalls.length || 0 }}</strong></div>
          <div><span>复核人</span><strong>{{ selectedItem?.review?.reviewer || '未分配' }}</strong></div>
          <div><span>复核意见</span><strong>{{ selectedItem?.review?.comment || '暂无真实复核意见' }}</strong></div>
        </div>
      </SectionCard>
    </section>
  </div>
</template>

<style scoped>
.human-review-page {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.review-header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 16px;
  min-width: 0;
}

.review-header h2,
.review-header p {
  margin: 0;
}

.review-header h2 {
  margin-top: 4px;
  font-size: 22px;
  line-height: 28px;
}

.review-header p:not(.eyebrow) {
  max-width: 780px;
  margin-top: 6px;
  color: var(--color-text-secondary);
  font-size: 12px;
  line-height: 18px;
}

.review-source,
.risk-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.review-source {
  justify-content: flex-end;
}

.review-source span {
  height: 26px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 9px;
  border: var(--border-subtle);
  border-radius: var(--radius-md);
  background: var(--color-card);
  color: var(--color-text-secondary);
  font-size: 11px;
  white-space: nowrap;
}

.review-source i {
  width: 6px;
  height: 6px;
  border-radius: 2px;
  background: var(--color-accent);
}

.review-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.review-metric {
  min-width: 0;
  padding: var(--card-padding-sm);
  border: var(--border-default);
  border-radius: var(--radius-card);
  background: var(--color-card);
}

.review-metric span,
.review-metric strong,
.review-metric small {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-metric span,
.review-metric small {
  color: var(--color-text-disabled);
  font-size: 10px;
}

.review-metric strong {
  margin-top: 7px;
  color: var(--color-text-primary);
  font-size: 22px;
  line-height: 1;
}

.review-metric[data-tone="success"] strong {
  color: var(--color-success);
}

.review-metric[data-tone="warning"] strong {
  color: var(--color-warning);
}

.review-metric[data-tone="danger"] strong {
  color: var(--color-error);
}

.review-console {
  display: grid;
  grid-template-columns: minmax(300px, 0.28fr) minmax(460px, 0.46fr) minmax(320px, 0.26fr);
  gap: 16px;
  min-width: 0;
  align-items: start;
}

.queue-list {
  display: grid;
}

.queue-row {
  width: 100%;
  min-height: 62px;
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border: 0;
  border-bottom: var(--border-subtle);
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.queue-row:hover,
.queue-row.active {
  background: var(--color-active-row);
}

.queue-row.active {
  box-shadow: inset 2px 0 var(--color-accent);
}

.queue-dot {
  width: 8px;
  height: 8px;
  border-radius: 2px;
  background: var(--color-warning);
}

.queue-dot[data-status="CONFIRMED"],
.queue-dot[data-status="SAVED"],
.queue-dot[data-status="SUCCESS"] {
  background: var(--color-success);
}

.queue-dot[data-status="FAILED"],
.queue-dot[data-status="REJECTED"] {
  background: var(--color-error);
}

.queue-dot[data-status="GENERATING"],
.queue-dot[data-status="RUNNING"] {
  background: var(--color-running);
  animation: pulse 1.4s ease-in-out infinite;
}

.queue-row strong,
.queue-row small {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.queue-row strong {
  color: var(--color-text-primary);
  font-size: 12px;
}

.queue-row small {
  margin-top: 5px;
  color: var(--color-text-disabled);
  font-size: 10px;
}

.artifact-summary,
.evidence-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  margin-bottom: 12px;
  border: var(--border-subtle);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--color-border-subtle);
}

.artifact-summary div,
.evidence-grid div {
  min-width: 0;
  padding: 10px;
  background: var(--color-bg);
}

.artifact-summary span,
.artifact-summary strong,
.evidence-grid span,
.evidence-grid strong {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.artifact-summary span,
.evidence-grid span {
  color: var(--color-text-disabled);
  font-size: 10px;
}

.artifact-summary strong,
.evidence-grid strong {
  margin-top: 5px;
  color: var(--color-text-primary);
  font-size: 11px;
}

.review-reason {
  display: grid;
  gap: 6px;
  margin-top: 12px;
}

.decision-label,
.review-reason span {
  color: var(--color-text-disabled);
  font-size: 10px;
}

.decision-label {
  display: block;
  margin-bottom: 6px;
}

.review-result-line {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 12px;
  padding: 9px 10px;
  border: var(--border-subtle);
  border-radius: var(--radius-md);
  background: var(--color-bg);
}

.review-result-line span {
  color: var(--color-text-disabled);
  font-size: 10px;
}

.review-result-line strong {
  min-width: 0;
  overflow: hidden;
  color: var(--color-text-primary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.decision-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.why-review {
  margin-top: 12px;
  padding: 12px;
  border: var(--border-subtle);
  border-radius: var(--radius-md);
  background: var(--color-bg);
}

.why-review strong {
  color: var(--color-text-primary);
  font-size: 12px;
}

.why-review p {
  margin: 7px 0 0;
  color: var(--color-text-secondary);
  font-size: 12px;
  line-height: 18px;
}

.mini-empty {
  color: var(--color-text-disabled);
  font-size: 11px;
}

.review-bottom {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
  gap: 16px;
  min-width: 0;
}

.history-list {
  display: grid;
}

.history-list article {
  min-height: 44px;
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto 112px;
  align-items: center;
  gap: 10px;
  border-bottom: var(--border-subtle);
}

.history-list article:last-child {
  border-bottom: 0;
}

.history-list strong,
.history-list small,
.history-list time {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-list strong {
  color: var(--color-text-primary);
  font-size: 12px;
}

.history-list small,
.history-list time {
  color: var(--color-text-disabled);
  font-size: 10px;
}

.empty-state {
  min-height: 72px;
  display: grid;
  place-items: center;
  padding: 12px;
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg);
  color: var(--color-text-disabled);
  text-align: center;
  font-size: 12px;
}

.human-review-page > *,
.review-header > *,
.review-source > *,
.review-metrics > *,
.review-console > *,
.queue-row > *,
.artifact-summary > *,
.decision-actions > *,
.review-bottom > *,
.history-list article > *,
.evidence-grid > * {
  min-width: 0;
}

@media (max-width: 1280px) {
  .review-console {
    grid-template-columns: minmax(280px, 0.82fr) minmax(0, 1fr);
  }

  .decision-panel {
    grid-column: 1 / -1;
  }
}

@media (max-width: 920px) {
  .review-header {
    align-items: stretch;
    flex-direction: column;
  }

  .review-source {
    justify-content: flex-start;
  }

  .review-metrics,
  .review-console,
  .artifact-summary,
  .decision-actions,
  .review-bottom,
  .history-list article,
  .evidence-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (prefers-reduced-motion: reduce) {
  .queue-dot[data-status="GENERATING"],
  .queue-dot[data-status="RUNNING"] {
    animation: none;
  }
}
</style>
