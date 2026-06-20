<script setup lang="ts">
import { computed, onMounted, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Plus } from '@element-plus/icons-vue'
import { fetchDashboardStats } from '@/api/devflow'
import type { DashboardStats, GenerationRecord } from '@/types/domain'

const router = useRouter()
const loading = shallowRef(false)
const stats = shallowRef<DashboardStats>({
  projectCount: 0,
  todayGenerationCount: 0,
  logAnalysisCount: 0,
  promptTemplateCount: 0,
  recentGenerations: [],
})

const recentTasks = computed(() => stats.value.recentGenerations)
const recentActivities = computed(() => stats.value.recentGenerations)
const metricItems = computed(() => [
  { label: '项目', value: stats.value.projectCount, code: 'PROJECTS' },
  { label: '今日生成', value: stats.value.todayGenerationCount, code: 'TODAY' },
  { label: '日志分析', value: stats.value.logAnalysisCount, code: 'LOGS' },
  { label: 'Prompt 模板', value: stats.value.promptTemplateCount, code: 'PROMPTS' },
])

function statusClass(record: GenerationRecord) {
  if (record.status === 'CONFIRMED' || record.status === 'Confirmed' || record.confirmed) return 'confirmed'
  if (record.status === 'SAVED' || record.status === 'Saved') return 'saved'
  if (record.status === 'FAILED' || record.status === 'Failed') return 'failed'
  if (record.status === 'READY_FOR_REVIEW' || record.status === 'Ready for Review') return 'review'
  return 'running'
}

function statusText(record: GenerationRecord) {
  if (record.status === 'CONFIRMED' || record.status === 'Confirmed' || record.confirmed) return '已确认'
  const map: Record<string, string> = {
    SAVED: '已保存',
    FAILED: '生成失败',
    READY_FOR_REVIEW: '待人工确认',
    GENERATING: '正在生成',
    DRAFT: '草稿',
    Saved: '已保存',
    Failed: '生成失败',
    'Ready for Review': '待人工确认',
    Generating: '正在生成',
    Draft: '草稿',
  }
  return map[record.status] || record.status
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

function formatTime(value: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '--'
}

function openRecord(record: GenerationRecord) {
  router.push({ path: '/history', query: { id: record.id } })
}

async function loadStats() {
  loading.value = true
  try {
    stats.value = await fetchDashboardStats()
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<template>
  <div class="dashboard" v-loading="loading">
    <header class="dashboard-header">
      <div class="page-intro">
        <span class="section-code mono">DEVFLOW / OVERVIEW</span>
        <h2>工作台概览</h2>
        <p>集中查看 AI Coding 生成记录、人工确认状态与本地 MVP 运行结果。</p>
      </div>

      <div class="summary-grid" aria-label="工作台统计">
        <div v-for="item in metricItems" :key="item.code" class="summary-cell">
          <span class="summary-code mono">{{ item.code }}</span>
          <div class="summary-value">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
          </div>
        </div>
      </div>
    </header>

    <section class="dashboard-grid">
      <section class="workspace-pane task-pane" aria-labelledby="recent-task-title">
        <header class="pane-header">
          <div class="pane-title">
            <span class="pane-mark" aria-hidden="true"></span>
            <div>
              <h3 id="recent-task-title">最近生成任务</h3>
              <p class="mono">{{ recentTasks.length }} GENERATION RECORDS</p>
            </div>
          </div>
          <button class="primary-action" type="button" @click="router.push('/workbench')">
            <el-icon><Plus /></el-icon>
            新建任务
          </button>
        </header>

        <div class="task-table-head" aria-hidden="true">
          <span>TRACE</span>
          <span>生成任务</span>
          <span>状态</span>
          <span>耗时</span>
        </div>

        <div class="task-list">
          <div v-if="!recentTasks.length" class="empty-state">暂无生成任务</div>
          <button
            v-for="(record, index) in recentTasks"
            :key="record.id"
            class="task-row"
            type="button"
            @click="openRecord(record)"
          >
            <span class="trace-cell" :class="{ last: index === recentTasks.length - 1 }">
              <span class="trace-node" :class="statusClass(record)"></span>
            </span>
            <span class="task-content">
              <strong>{{ record.inputSummary }}</strong>
              <span class="task-meta mono">
                #{{ record.id }} · {{ formatType(record.generationType) }} · {{ record.modelName }}
              </span>
            </span>
            <span class="record-status" :class="statusClass(record)">
              <i aria-hidden="true"></i>{{ statusText(record) }}
            </span>
            <span class="task-cost mono">{{ record.costTimeMs }}ms</span>
          </button>
        </div>
      </section>

      <aside class="workspace-pane activity-pane" aria-labelledby="recent-activity-title">
        <header class="pane-header">
          <div class="pane-title">
            <span class="pane-mark amber" aria-hidden="true"></span>
            <div>
              <h3 id="recent-activity-title">最近活动</h3>
              <p class="mono">REVIEW LEDGER</p>
            </div>
          </div>
          <button class="text-action" type="button" @click="router.push('/history')">
            生成历史
            <el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <div class="activity-list">
          <div v-if="!recentActivities.length" class="empty-state">暂无最近活动</div>
          <button
            v-for="record in recentActivities"
            :key="record.id"
            class="activity-row"
            type="button"
            @click="openRecord(record)"
          >
            <span class="activity-node" :class="statusClass(record)" aria-hidden="true"></span>
            <span class="activity-main">
              <strong>{{ record.inputSummary }}</strong>
              <span class="mono">{{ formatTime(record.createdAt) }} · {{ record.costTimeMs }}ms</span>
            </span>
            <span class="record-status" :class="statusClass(record)">
              <i aria-hidden="true"></i>{{ statusText(record) }}
            </span>
          </button>
        </div>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.dashboard {
  display: grid;
  gap: 16px;
  width: 100%;
  min-width: 0;
}

.dashboard-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  min-width: 0;
  padding-bottom: 16px;
  border-bottom: var(--border-subtle);
}

.page-intro {
  flex: 1 1 340px;
  min-width: 0;
}

.section-code,
.summary-code {
  color: var(--color-accent);
  font-size: 10px;
  letter-spacing: 0.08em;
}

.page-intro h2 {
  margin: 4px 0 0;
  font-size: 20px;
  line-height: 28px;
  font-weight: 620;
  letter-spacing: -0.02em;
}

.page-intro p {
  margin: 3px 0 0;
  overflow: hidden;
  color: var(--color-text-secondary);
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-grid {
  flex: 0 1 520px;
  min-width: min(100%, 440px);
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border: var(--border-default);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  overflow: hidden;
}

.summary-cell {
  min-width: 0;
  padding: 9px 12px 8px;
  border-right: var(--border-subtle);
}

.summary-cell:last-child {
  border-right: 0;
}

.summary-code {
  display: block;
  overflow: hidden;
  color: var(--color-text-disabled);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-value {
  display: flex;
  align-items: baseline;
  gap: 7px;
  min-width: 0;
  margin-top: 2px;
}

.summary-value strong {
  flex: 0 0 auto;
  color: var(--color-text-primary);
  font-family: var(--font-mono);
  font-size: 20px;
  font-variant-numeric: tabular-nums;
  line-height: 24px;
}

.summary-value span {
  min-width: 0;
  overflow: hidden;
  color: var(--color-text-secondary);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 7fr) minmax(360px, 5fr);
  gap: 16px;
  align-items: start;
  min-width: 0;
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
  gap: 12px;
  min-width: 0;
  padding: 8px 12px;
  border-bottom: var(--border-subtle);
  background: var(--color-surface-raised);
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

.primary-action,
.text-action {
  flex: 0 0 auto;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  border-radius: 5px;
  cursor: pointer;
  font-size: 12px;
  white-space: nowrap;
  transition: background-color 120ms ease, border-color 120ms ease, color 120ms ease;
}

.primary-action {
  padding: 0 11px;
  border: 1px solid var(--color-accent);
  background: var(--color-accent);
  color: #0f1714;
  font-weight: 600;
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

.task-table-head {
  height: 28px;
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) 92px 64px;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 0 12px;
  border-bottom: var(--border-subtle);
  color: var(--color-text-disabled);
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.06em;
}

.task-table-head span:nth-child(3),
.task-table-head span:nth-child(4) {
  text-align: right;
}

.task-list,
.activity-list {
  min-width: 0;
}

.task-row {
  width: 100%;
  min-width: 0;
  min-height: 58px;
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) 92px 64px;
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

.task-row:last-child {
  border-bottom: 0;
}

.task-row:hover,
.activity-row:hover {
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

.trace-node,
.activity-node {
  position: relative;
  z-index: 1;
  width: 8px;
  height: 8px;
  border: 1px solid var(--color-text-disabled);
  border-radius: 2px;
  background: var(--color-surface);
}

.trace-node.review,
.activity-node.review {
  border-color: var(--color-warning);
  background: var(--color-warning);
}

.trace-node.confirmed,
.activity-node.confirmed {
  border-color: var(--color-accent);
  background: var(--color-accent);
}

.trace-node.saved,
.activity-node.saved {
  border-color: var(--color-info);
  background: var(--color-info);
}

.trace-node.failed,
.activity-node.failed {
  border-color: var(--color-error);
  background: var(--color-error);
}

.trace-node.running,
.activity-node.running {
  border-color: var(--color-accent);
  background: var(--color-accent);
  animation: pulse 1.6s ease-in-out infinite;
}

.task-content,
.activity-main {
  display: block;
  min-width: 0;
}

.task-content strong,
.task-meta,
.activity-main strong,
.activity-main > span {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-content strong,
.activity-main strong {
  color: var(--color-text-primary);
  font-size: 12px;
  font-weight: 550;
  line-height: 18px;
}

.task-meta,
.activity-main > span {
  margin-top: 2px;
  color: var(--color-text-disabled);
  font-size: 10px;
  line-height: 15px;
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

.task-cost {
  min-width: 0;
  overflow: hidden;
  color: var(--color-text-secondary);
  font-size: 10px;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-row {
  width: 100%;
  min-width: 0;
  min-height: 58px;
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  align-items: center;
  gap: 9px;
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

.activity-row .record-status {
  max-width: 88px;
}

.empty-state {
  min-height: 96px;
  border: 0;
  border-radius: 0;
  background: transparent;
}

.dashboard-header > *,
.summary-grid > *,
.summary-value > *,
.dashboard-grid > *,
.pane-header > *,
.pane-title > *,
.task-table-head > *,
.task-row > *,
.activity-row > * {
  min-width: 0;
}

@media (max-width: 1120px) {
  .dashboard-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (prefers-reduced-motion: reduce) {
  .trace-node.running,
  .activity-node.running {
    animation: none;
  }
}
</style>
