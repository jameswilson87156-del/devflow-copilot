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
  <div class="agent-trace-page" v-loading="loading">
    <section class="run-list panel">
      <div class="panel-header">
        <div>
          <h3 class="panel-title">Agent Run Trace</h3>
          <p class="panel-subtitle">任务拆解、Prompt 渲染、Provider 调用、工具记录和人工确认</p>
        </div>
        <el-button :icon="RefreshRight" @click="loadRuns">刷新</el-button>
      </div>
      <div class="run-filters">
        <el-select v-model="selectedProjectId" placeholder="项目" @change="loadRuns">
          <el-option v-for="project in projectOptions" :key="project.id ?? 'all'" :label="project.projectName" :value="project.id" />
        </el-select>
      </div>
      <div class="run-rows">
        <div v-if="!runs.length" class="empty-state">暂无 Agent Run。先在 AI 工作台运行一次生成。</div>
        <button
          v-for="run in runs"
          :key="run.id"
          class="run-row"
          :class="{ active: selectedRunId === run.id }"
          type="button"
          @click="selectedRunId = run.id; loadTrace()"
        >
          <span class="trace-node" :class="statusTone(run.status)" aria-hidden="true"></span>
          <span class="run-main">
            <strong>{{ run.title }}</strong>
            <small class="mono">RUN-{{ run.id }} · GEN-{{ run.generationRecordId || '—' }} · {{ formatTime(run.createdAt) }}</small>
          </span>
          <span class="run-meta">
            <b>{{ run.status }}</b>
            <small class="mono">{{ run.latencyMs ?? 0 }}ms</small>
          </span>
        </button>
      </div>
    </section>

    <section class="trace-detail">
      <div v-if="selectedRun && trace" class="run-summary panel">
        <div>
          <span class="mono">WORKFLOW</span>
          <strong>{{ selectedRun.title }}</strong>
          <small>{{ selectedRun.goal }}</small>
        </div>
        <div class="summary-grid">
          <div><span>状态</span><strong>{{ trace.run.status }}</strong></div>
          <div><span>Provider</span><strong>{{ trace.run.providerName || 'local-rule' }}</strong></div>
          <div><span>模型</span><strong>{{ trace.run.modelName || 'local-rule-mvp' }}</strong></div>
          <div><span>耗时</span><strong class="mono">{{ trace.run.latencyMs ?? 0 }}ms</strong></div>
        </div>
      </div>

      <div v-if="trace" class="trace-grid">
        <section class="steps-panel panel">
          <div class="panel-header">
            <div>
              <h3 class="panel-title">运行步骤</h3>
              <p class="panel-subtitle">可解释的 Agent Workflow 记录，不模拟复杂多 Agent</p>
            </div>
          </div>
          <div class="step-list">
            <article v-for="step in trace.steps" :key="step.id" class="step-row">
              <span class="spine">
                <i :class="statusTone(step.status)"></i>
              </span>
              <div class="step-body">
                <div class="step-title">
                  <strong>{{ step.stepOrder }}. {{ step.stepName }}</strong>
                  <StatusTag :status="step.status" />
                </div>
                <p>{{ step.summary }}</p>
                <small class="mono">{{ step.stepType }} · {{ step.latencyMs ?? 0 }}ms</small>
                <div v-if="toolCallsByStep.get(step.id)?.length" class="tool-stack">
                  <article v-for="tool in toolCallsByStep.get(step.id)" :key="tool.id">
                    <strong>{{ tool.toolName }}</strong>
                    <span>{{ tool.inputSummary }}</span>
                    <small>{{ tool.outputSummary }}</small>
                  </article>
                </div>
              </div>
            </article>
          </div>
        </section>

        <aside class="review-panel panel">
          <div class="panel-header">
            <div>
              <h3 class="panel-title">Human Review</h3>
              <p class="panel-subtitle">保存、确认或失败都会留下人工复核记录</p>
            </div>
          </div>
          <div class="review-list">
            <article v-for="review in trace.humanReviews" :key="review.id">
              <span class="trace-node" :class="statusTone(review.reviewStatus)" aria-hidden="true"></span>
              <div>
                <strong>{{ review.reviewStatus }}</strong>
                <p>{{ review.comment }}</p>
                <small class="mono">{{ review.reviewer || 'manual' }} · {{ formatTime(review.updatedAt || review.createdAt) }}</small>
              </div>
            </article>
            <div v-if="!trace.humanReviews.length" class="empty-state">暂无人工确认记录</div>
          </div>
        </aside>
      </div>

      <div v-else class="empty-detail panel">选择一条 Agent Run 查看 Trace。</div>
    </section>
  </div>
</template>

<style scoped>
.agent-trace-page { display: grid; grid-template-columns: minmax(340px, 390px) minmax(0, 1fr); gap: 12px; min-width: 0; align-items: start; }
.agent-trace-page > *, .run-row > *, .trace-grid > *, .run-summary > * { min-width: 0; }
.run-filters { padding: 10px; border-bottom: var(--border-subtle); }
.run-rows { display: grid; }
.run-row { width: 100%; min-height: 64px; display: grid; grid-template-columns: 10px minmax(0, 1fr) 78px; gap: 9px; align-items: center; padding: 9px 10px; border: 0; border-bottom: var(--border-subtle); background: transparent; color: inherit; text-align: left; cursor: pointer; }
.run-row:hover, .run-row.active { background: var(--color-active-row); }
.run-row.active { box-shadow: inset 2px 0 var(--color-accent); }
.trace-node { width: 8px; height: 8px; border: 1px solid var(--color-text-ghost); border-radius: 2px; background: var(--color-surface); }
.trace-node.success, .spine i.success { border-color: var(--color-accent); background: var(--color-accent); }
.trace-node.failed, .spine i.failed { border-color: var(--color-error); background: var(--color-error); }
.trace-node.running, .spine i.running { border-color: var(--color-warning); background: var(--color-warning); animation: pulse 1.4s infinite; }
.run-main strong, .run-main small, .run-meta b, .run-meta small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.run-main strong { font-size: 12px; }
.run-main small, .run-meta small { margin-top: 4px; color: var(--color-text-disabled); font-size: 9px; }
.run-meta { text-align: right; }
.run-meta b { color: var(--color-text-secondary); font-size: 9px; }
.trace-detail { display: grid; gap: 12px; min-width: 0; }
.run-summary { padding: 12px; display: grid; grid-template-columns: minmax(0, 1fr) minmax(420px, .9fr); gap: 12px; align-items: center; }
.run-summary span, .run-summary strong, .run-summary small { display: block; overflow: hidden; text-overflow: ellipsis; }
.run-summary span { color: var(--color-accent); font-size: 9px; letter-spacing: .08em; }
.run-summary strong { margin-top: 4px; font-size: 16px; }
.run-summary small { margin-top: 5px; color: var(--color-text-secondary); font-size: 11px; white-space: nowrap; }
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1px; background: var(--color-border-subtle); }
.summary-grid div { padding: 8px; background: var(--color-bg); }
.summary-grid div span { color: var(--color-text-disabled); letter-spacing: 0; }
.summary-grid div strong { margin-top: 4px; font-size: 11px; white-space: nowrap; }
.trace-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(260px, 320px); gap: 12px; align-items: start; }
.step-list { display: grid; }
.step-row { display: grid; grid-template-columns: 32px minmax(0, 1fr); border-bottom: var(--border-subtle); }
.step-row:last-child { border-bottom: 0; }
.spine { position: relative; display: grid; place-items: start center; padding-top: 16px; }
.spine::after { content: ""; position: absolute; top: 28px; bottom: -18px; width: 1px; background: var(--color-border-subtle); }
.step-row:last-child .spine::after { display: none; }
.spine i { position: relative; z-index: 1; width: 8px; height: 8px; border: 1px solid var(--color-text-ghost); border-radius: 2px; background: var(--color-surface); }
.step-body { padding: 12px 12px 12px 0; }
.step-title { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.step-title strong { overflow: hidden; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.step-body p { margin: 6px 0 0; color: var(--color-text-secondary); font-size: 11px; line-height: 17px; }
.step-body > small { display: block; margin-top: 7px; color: var(--color-text-disabled); font-size: 9px; }
.tool-stack { display: grid; gap: 1px; margin-top: 8px; background: var(--color-border-subtle); }
.tool-stack article { min-width: 0; padding: 8px; background: var(--color-bg); }
.tool-stack strong, .tool-stack span, .tool-stack small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tool-stack strong { color: var(--color-accent); font-size: 10px; }
.tool-stack span, .tool-stack small { margin-top: 4px; color: var(--color-text-secondary); font-size: 9px; }
.review-list { display: grid; }
.review-list article { display: grid; grid-template-columns: 12px minmax(0, 1fr); gap: 8px; padding: 11px; border-bottom: var(--border-subtle); }
.review-list article:last-child { border-bottom: 0; }
.review-list strong, .review-list p, .review-list small { display: block; overflow: hidden; text-overflow: ellipsis; }
.review-list strong { font-size: 12px; }
.review-list p { margin: 5px 0 0; color: var(--color-text-secondary); font-size: 11px; line-height: 17px; }
.review-list small { margin-top: 6px; color: var(--color-text-disabled); font-size: 9px; white-space: nowrap; }
.empty-detail { min-height: 240px; display: grid; place-items: center; color: var(--color-text-secondary); }
@media (max-width: 1120px) { .agent-trace-page, .trace-grid, .run-summary { grid-template-columns: 1fr; } }
</style>
