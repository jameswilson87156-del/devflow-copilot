<script setup lang="ts">
import { computed, onMounted, shallowRef } from 'vue'
import { useRoute } from 'vue-router'
import { RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { fetchAgentRunTrace, fetchAgentRuns, fetchProjects } from '@/api/devflow'
import StatusTag from '@/components/StatusTag.vue'
import type { AgentRun, AgentRunTrace, ProjectContext, ToolCallRecord } from '@/types/domain'

const projects = shallowRef<ProjectContext[]>([])
const runs = shallowRef<AgentRun[]>([])
const selectedProjectId = shallowRef<number>()
const selectedRunId = shallowRef<number>()
const trace = shallowRef<AgentRunTrace>()
const loading = shallowRef(false)
const route = useRoute()

const projectOptions = computed(() => [{ id: undefined, projectName: '全部项目' }, ...projects.value])
const selectedRun = computed(() => runs.value.find((run) => run.id === selectedRunId.value))
const toolCallsByStep = computed(() => {
  const map = new Map<number, ToolCallRecord[]>()
  for (const tool of trace.value?.toolCalls || []) {
    if (!tool.stepId) continue
    map.set(tool.stepId, [...(map.get(tool.stepId) || []), tool])
  }
  return map
})

async function loadRuns() {
  loading.value = true
  try {
    const queryGenerationId = Number(route.query.generationRecordId)
    if (queryGenerationId) {
      runs.value = await fetchAgentRuns({ generationRecordId: queryGenerationId })
    } else {
      runs.value = await fetchAgentRuns({ projectId: selectedProjectId.value })
    }
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
    return
  }
  trace.value = await fetchAgentRunTrace(selectedRunId.value)
}

async function loadPageData() {
  projects.value = await fetchProjects()
  await loadRuns()
}

function statusTone(status?: string) {
  const value = (status || '').toUpperCase()
  if (['SUCCESS', 'CONFIRMED', 'WAITING_REVIEW', 'SAVED'].includes(value)) return 'success'
  if (['FAILED', 'REJECTED'].includes(value)) return 'failed'
  if (['WAITING', 'PENDING', 'RUNNING'].includes(value)) return 'running'
  return 'idle'
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '—'
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : '加载 Agent Run Trace 失败'
}

onMounted(loadPageData)
</script>

<template>
  <div class="agent-trace-page trace-skin" v-loading="loading">
    <section class="run-list trace-panel run-directory">
      <div class="panel-header trace-section-header">
        <div>
          <h3 class="panel-title">Agent Run Trace</h3>
          <p class="panel-subtitle">任务拆解、Prompt 渲染、Provider 调用、工具记录和人工确认</p>
        </div>
        <el-button class="refresh-action" :icon="RefreshRight" @click="loadRuns">刷新</el-button>
      </div>
      <div class="run-filters trace-filter-bar">
        <el-select v-model="selectedProjectId" placeholder="项目" @change="loadRuns">
          <el-option v-for="project in projectOptions" :key="project.id ?? 'all'" :label="project.projectName" :value="project.id" />
        </el-select>
      </div>
      <div class="run-rows">
        <div v-if="!runs.length" class="empty-state">暂无 Agent Run。先在 AI 工作台运行一次生成。</div>
        <button
          v-for="run in runs"
          :key="run.id"
          class="run-row trace-run-row"
          :class="{ active: selectedRunId === run.id }"
          type="button"
          @click="selectedRunId = run.id; loadTrace()"
        >
          <span class="trace-node status-light" :class="statusTone(run.status)" aria-hidden="true"></span>
          <span class="run-main">
            <strong>{{ run.title }}</strong>
            <small class="mono">RUN-{{ run.id }} · GEN-{{ run.generationRecordId || '—' }} · {{ formatTime(run.createdAt) }}</small>
          </span>
          <span class="run-meta">
            <b class="status-badge" :class="statusTone(run.status)">{{ run.status }}</b>
            <small class="mono">{{ run.latencyMs ?? 0 }}ms</small>
          </span>
        </button>
      </div>
    </section>

    <section class="trace-detail">
      <div v-if="selectedRun && trace" class="run-summary metrics-strip">
        <div class="workflow-title">
          <span class="mono">WORKFLOW</span>
          <strong>{{ selectedRun.title }}</strong>
          <small>{{ selectedRun.goal }}</small>
        </div>
        <div class="summary-grid metrics-bar">
          <div class="metric-cell metric-status"><span>状态</span><strong class="status-badge" :class="statusTone(trace.run.status)">{{ trace.run.status }}</strong></div>
          <div class="metric-cell"><span>Provider</span><strong>{{ trace.run.providerName || 'local-rule' }}</strong></div>
          <div class="metric-cell"><span>模型</span><strong>{{ trace.run.modelName || 'local-rule-mvp' }}</strong></div>
          <div class="metric-cell"><span>耗时</span><strong class="mono">{{ trace.run.latencyMs ?? 0 }}ms</strong></div>
        </div>
      </div>

      <div v-if="trace" class="trace-grid">
        <section class="steps-panel trace-panel trace-region">
          <div class="panel-header trace-section-header">
            <div>
              <h3 class="panel-title">运行步骤</h3>
              <p class="panel-subtitle">可解释的 Agent Workflow 记录，不模拟复杂多 Agent</p>
            </div>
          </div>
          <div class="step-list">
            <article v-for="step in trace.steps" :key="step.id" class="step-row trace-step-row">
              <span class="spine trace-spine">
                <i class="spine-dot" :class="statusTone(step.status)"></i>
              </span>
              <div class="step-body trace-cell">
                <div class="step-title">
                  <strong>{{ step.stepOrder }}. {{ step.stepName }}</strong>
                  <StatusTag :status="step.status" />
                </div>
                <p>{{ step.summary }}</p>
                <small class="mono">{{ step.stepType }} · {{ step.latencyMs ?? 0 }}ms</small>
                <div v-if="toolCallsByStep.get(step.id)?.length" class="tool-stack tool-ledger">
                  <article v-for="tool in toolCallsByStep.get(step.id)" :key="tool.id" class="tool-entry">
                    <strong>{{ tool.toolName }}</strong>
                    <span>{{ tool.inputSummary }}</span>
                    <small>{{ tool.outputSummary }}</small>
                  </article>
                </div>
              </div>
            </article>
          </div>
        </section>

        <aside class="review-panel trace-panel trace-region review-inspector">
          <div class="panel-header trace-section-header">
            <div>
              <h3 class="panel-title">Human Review</h3>
              <p class="panel-subtitle">保存、确认或失败都会留下人工复核记录</p>
            </div>
          </div>
          <div class="review-list">
            <article v-for="review in trace.humanReviews" :key="review.id" class="review-row">
              <span class="trace-node status-light" :class="statusTone(review.reviewStatus)" aria-hidden="true"></span>
              <div>
                <strong class="status-badge" :class="statusTone(review.reviewStatus)">{{ review.reviewStatus }}</strong>
                <p>{{ review.comment }}</p>
                <small class="mono">{{ review.reviewer || 'manual' }} · {{ formatTime(review.updatedAt || review.createdAt) }}</small>
              </div>
            </article>
            <div v-if="!trace.humanReviews.length" class="empty-state">暂无人工确认记录</div>
          </div>
        </aside>
      </div>

      <div v-else class="empty-detail trace-panel">选择一条 Agent Run 查看 Trace。</div>
    </section>
  </div>
</template>

<style scoped>
.agent-trace-page {
  --trace-bg: #050505;
  --trace-panel: #090909;
  --trace-panel-soft: #0d0d0d;
  --trace-line: #222;
  --trace-line-soft: rgba(255, 255, 255, 0.075);
  --trace-ink: #999;
  --trace-ink-soft: #6f6f6f;
  --trace-strong: #fff;
  --trace-blue: #60a5fa;
  --trace-green: #4ade80;
  --trace-amber: #fbbf24;
  --trace-red: #f87171;
  display: grid;
  grid-template-columns: minmax(340px, 390px) minmax(0, 1fr);
  gap: 0;
  min-width: 0;
  min-height: calc(100dvh - 72px);
  align-items: start;
  border: 0.5px solid var(--trace-line);
  background: var(--trace-bg);
  color: var(--trace-ink);
}

.agent-trace-page > *,
.run-row > *,
.trace-grid > *,
.run-summary > * {
  min-width: 0;
}

.trace-panel {
  border: 0;
  border-radius: 0;
  background: var(--trace-bg);
  box-shadow: none;
}

.run-directory {
  min-height: calc(100dvh - 73px);
  border-right: 0.5px solid var(--trace-line);
}

.trace-section-header {
  min-height: 58px;
  padding: 15px 16px;
  border-bottom: 0.5px solid var(--trace-line);
  background: var(--trace-bg);
}

.panel-title {
  color: var(--trace-strong);
  font-size: 13px;
  font-weight: 620;
  letter-spacing: 0;
}

.panel-subtitle {
  max-width: 58ch;
  color: var(--trace-ink-soft);
  font-size: 11px;
  line-height: 1.5;
}

.refresh-action {
  --el-button-bg-color: transparent;
  --el-button-border-color: var(--trace-line);
  --el-button-text-color: var(--trace-ink);
  --el-button-hover-bg-color: rgba(255, 255, 255, 0.035);
  --el-button-hover-border-color: #333;
  --el-button-hover-text-color: var(--trace-strong);
  border-radius: 999px;
}

.trace-filter-bar {
  padding: 12px 14px;
  border-bottom: 0.5px solid var(--trace-line);
  background: #070707;
}

.trace-filter-bar :deep(.el-select__wrapper) {
  min-height: 32px;
  border-radius: 0 !important;
  background: #050505 !important;
  box-shadow: 0 0 0 0.5px var(--trace-line) inset !important;
}

.trace-filter-bar :deep(.el-select__placeholder),
.trace-filter-bar :deep(.el-select__selected-item) {
  color: var(--trace-ink);
}

.run-rows {
  display: grid;
}

.trace-run-row {
  width: 100%;
  min-height: 72px;
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr) 118px;
  gap: 10px;
  align-items: center;
  padding: 12px 14px;
  border: 0;
  border-bottom: 0.5px solid var(--trace-line);
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: background 140ms ease, color 140ms ease;
}

.trace-run-row:hover,
.trace-run-row.active {
  background: rgba(255, 255, 255, 0.035);
}

.trace-run-row.active {
  box-shadow: inset 2px 0 var(--trace-blue);
}

.trace-node,
.spine-dot {
  width: 8px;
  height: 8px;
  border: 0.5px solid #3a3a3a;
  border-radius: 999px;
  background: #111;
}

.status-light.success,
.spine-dot.success {
  border-color: rgba(74, 222, 128, 0.65);
  background: var(--trace-green);
}

.status-light.failed,
.spine-dot.failed {
  border-color: rgba(248, 113, 113, 0.65);
  background: var(--trace-red);
}

.status-light.running,
.spine-dot.running {
  border-color: rgba(96, 165, 250, 0.65);
  background: var(--trace-blue);
  animation: trace-breathe 1.6s ease-in-out infinite;
}

.status-light.idle,
.spine-dot.idle {
  border-color: rgba(96, 165, 250, 0.42);
  background: rgba(96, 165, 250, 0.55);
}

.run-main strong,
.run-main small,
.run-meta b,
.run-meta small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.run-main strong {
  color: var(--trace-strong);
  font-size: 12px;
  font-weight: 560;
}

.run-main small,
.run-meta small {
  margin-top: 6px;
  color: var(--trace-ink-soft);
  font-size: 9px;
}

.run-meta {
  display: grid;
  justify-items: end;
  text-align: right;
}

.trace-detail {
  display: grid;
  min-width: 0;
  background: var(--trace-bg);
}

.metrics-strip {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(560px, 0.95fr);
  align-items: stretch;
  border-bottom: 0.5px solid var(--trace-line);
  background: var(--trace-bg);
}

.workflow-title {
  padding: 16px 18px;
  border-right: 0.5px solid var(--trace-line);
}

.workflow-title span,
.workflow-title strong,
.workflow-title small,
.metric-cell span,
.metric-cell strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
}

.workflow-title span,
.metric-cell span {
  color: var(--trace-ink-soft);
  font-size: 9px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.workflow-title strong {
  margin-top: 7px;
  color: var(--trace-strong);
  font-size: 17px;
  font-weight: 620;
  line-height: 1.25;
}

.workflow-title small {
  margin-top: 7px;
  color: var(--trace-ink);
  font-size: 11px;
  line-height: 1.45;
  white-space: nowrap;
}

.metrics-bar {
  display: grid;
  grid-template-columns: minmax(150px, 1.35fr) repeat(3, minmax(0, 1fr));
  background: var(--trace-bg);
}

.metric-cell {
  display: grid;
  align-content: center;
  min-height: 74px;
  padding: 12px 14px;
  border-right: 0.5px solid var(--trace-line);
}

.metric-cell:last-child {
  border-right: 0;
}

.metric-cell strong {
  margin-top: 7px;
  color: var(--trace-strong);
  font-size: 12px;
  font-weight: 560;
  white-space: nowrap;
}

.metric-cell .mono {
  font-variant-numeric: tabular-nums;
}

.trace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 340px);
  gap: 0;
  align-items: start;
}

.trace-region {
  border-right: 0.5px solid var(--trace-line);
}

.review-inspector {
  border-right: 0;
}

.step-list,
.review-list {
  display: grid;
}

.trace-step-row {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  border-bottom: 0.5px solid var(--trace-line);
}

.trace-step-row:last-child {
  border-bottom: 0;
}

.trace-spine {
  position: relative;
  display: grid;
  place-items: start center;
  padding-top: 21px;
}

.trace-spine::after {
  content: "";
  position: absolute;
  top: 31px;
  bottom: -20px;
  width: 0.5px;
  background: var(--trace-line);
}

.trace-step-row:last-child .trace-spine::after {
  display: none;
}

.spine-dot {
  position: relative;
  z-index: 1;
}

.trace-cell {
  padding: 16px 18px 16px 0;
}

.step-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.step-title strong {
  overflow: hidden;
  color: var(--trace-strong);
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-body p {
  margin: 8px 0 0;
  color: var(--trace-ink);
  font-size: 12px;
  line-height: 1.65;
}

.step-body > small {
  display: block;
  margin-top: 8px;
  color: var(--trace-ink-soft);
  font-size: 9px;
}

.tool-ledger {
  display: grid;
  margin-top: 12px;
  border-top: 0.5px solid var(--trace-line);
}

.tool-entry {
  min-width: 0;
  padding: 10px 0 10px 12px;
  border-bottom: 0.5px solid var(--trace-line);
  border-left: 0.5px solid var(--trace-line);
  background: transparent;
}

.tool-entry:last-child {
  border-bottom: 0;
}

.tool-entry strong,
.tool-entry span,
.tool-entry small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tool-entry strong {
  color: var(--trace-blue);
  font-size: 10px;
  font-weight: 560;
  letter-spacing: 0.02em;
}

.tool-entry span,
.tool-entry small {
  margin-top: 5px;
  color: var(--trace-ink);
  font-size: 10px;
}

.tool-entry small {
  color: var(--trace-ink-soft);
}

.review-row {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr);
  gap: 10px;
  padding: 14px;
  border-bottom: 0.5px solid var(--trace-line);
}

.review-row:last-child {
  border-bottom: 0;
}

.review-list p,
.review-list small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
}

.review-list p {
  margin: 8px 0 0;
  color: var(--trace-ink);
  font-size: 12px;
  line-height: 1.6;
}

.review-list small {
  margin-top: 8px;
  color: var(--trace-ink-soft);
  font-size: 9px;
  white-space: nowrap;
}

.status-badge {
  display: inline-flex !important;
  max-width: 100%;
  height: 22px;
  align-items: center;
  gap: 6px;
  padding: 0 8px;
  border: 0.5px solid rgba(96, 165, 250, 0.24);
  border-radius: 999px;
  background: rgba(96, 165, 250, 0.1);
  color: var(--trace-blue);
  font-size: 10px;
  font-weight: 560;
  line-height: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-badge::before {
  content: "";
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: currentColor;
  box-shadow: 0 0 0 3px color-mix(in srgb, currentColor 12%, transparent);
  animation: trace-breathe 1.6s ease-in-out infinite;
  flex: 0 0 auto;
}

.status-badge.success {
  border-color: rgba(74, 222, 128, 0.24);
  background: rgba(74, 222, 128, 0.1);
  color: var(--trace-green);
}

.status-badge.failed {
  border-color: rgba(248, 113, 113, 0.26);
  background: rgba(248, 113, 113, 0.1);
  color: var(--trace-red);
}

.status-badge.running {
  border-color: rgba(251, 191, 36, 0.26);
  background: rgba(251, 191, 36, 0.1);
  color: var(--trace-amber);
}

.agent-trace-page :deep(.status-tag) {
  height: 22px;
  padding: 0 8px;
  border: 0.5px solid rgba(96, 165, 250, 0.24);
  border-radius: 999px;
  background: rgba(96, 165, 250, 0.1);
  color: var(--trace-blue);
  font-size: 10px;
  font-weight: 560;
}

.agent-trace-page :deep(.status-tag::before) {
  width: 6px;
  height: 6px;
  margin-right: 6px;
  border-radius: 999px;
  background: currentColor;
  box-shadow: 0 0 0 3px color-mix(in srgb, currentColor 12%, transparent);
  animation: trace-breathe 1.6s ease-in-out infinite;
}

.agent-trace-page :deep(.status-tag.saved),
.agent-trace-page :deep(.status-tag.confirmed),
.agent-trace-page :deep(.status-tag.success) {
  border-color: rgba(74, 222, 128, 0.24);
  background: rgba(74, 222, 128, 0.1);
  color: var(--trace-green);
}

.agent-trace-page :deep(.status-tag.failed),
.agent-trace-page :deep(.status-tag.rejected) {
  border-color: rgba(248, 113, 113, 0.26);
  background: rgba(248, 113, 113, 0.1);
  color: var(--trace-red);
}

.agent-trace-page :deep(.status-tag.generating),
.agent-trace-page :deep(.status-tag.pending),
.agent-trace-page :deep(.status-tag.running),
.agent-trace-page :deep(.status-tag.waiting) {
  border-color: rgba(251, 191, 36, 0.26);
  background: rgba(251, 191, 36, 0.1);
  color: var(--trace-amber);
}

.empty-state,
.empty-detail {
  min-height: 96px;
  display: grid;
  place-items: center;
  border: 0;
  border-top: 0.5px solid var(--trace-line);
  border-bottom: 0.5px solid var(--trace-line);
  border-radius: 0;
  background: transparent;
  color: var(--trace-ink-soft);
  text-align: center;
  font-size: 12px;
}

.empty-detail {
  min-height: 260px;
}

.agent-trace-page :deep(.el-loading-mask) {
  background-color: rgba(5, 5, 5, 0.82);
}

@keyframes trace-breathe {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.46;
    transform: scale(0.72);
  }
}

@media (max-width: 1120px) {
  .agent-trace-page,
  .trace-grid,
  .metrics-strip {
    grid-template-columns: 1fr;
  }

  .run-directory,
  .workflow-title,
  .trace-region {
    border-right: 0;
  }

  .metrics-bar {
    border-top: 0.5px solid var(--trace-line);
  }
}

@media (prefers-reduced-motion: reduce) {
  .status-light.running,
  .spine-dot.running,
  .status-badge::before,
  .agent-trace-page :deep(.status-tag::before) {
    animation: none;
  }
}
</style>
