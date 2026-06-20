<script setup lang="ts">
import { computed, onMounted, shallowRef } from 'vue'
import { CircleCheck, DocumentChecked, View } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { confirmGeneration, fetchGenerations, fetchProjects, saveGeneration } from '@/api/devflow'
import CopyButton from '@/components/CopyButton.vue'
import MarkdownPanel from '@/components/MarkdownPanel.vue'
import StatusTag from '@/components/StatusTag.vue'
import type { GenerationRecord, ProjectContext } from '@/types/domain'

const projects = shallowRef<ProjectContext[]>([])
const records = shallowRef<GenerationRecord[]>([])
const selectedProjectId = shallowRef<number>()
const selectedType = shallowRef('')
const selectedRecord = shallowRef<GenerationRecord>()
const loading = shallowRef(false)
const route = useRoute()

const generationTypes = [
  { label: '全部类型', value: '' },
  { label: '需求拆解', value: 'requirement-split' },
  { label: '代码修改计划', value: 'code-plan' },
  { label: 'README', value: 'readme-generate' },
  { label: 'Commit Message', value: 'commit-message' },
  { label: '修复 Prompt', value: 'fix-prompt' },
  { label: '日志分析', value: 'log-analysis' },
]

const projectOptions = computed(() => [{ id: undefined, projectName: '全部项目' }, ...projects.value])
const projectNameMap = computed(() => new Map(projects.value.map((project) => [project.id, project.projectName])))

async function loadRecords() {
  loading.value = true
  try {
    records.value = await fetchGenerations({ projectId: selectedProjectId.value, generationType: selectedType.value || undefined })
    const queryId = Number(route.query.id)
    selectedRecord.value = records.value.find((record) => record.id === queryId)
      || records.value.find((record) => record.id === selectedRecord.value?.id)
      || records.value[0]
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    loading.value = false
  }
}

async function loadPageData() {
  try {
    projects.value = await fetchProjects()
    await loadRecords()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
}

async function saveRecord(record: GenerationRecord) {
  await mutateRecord(() => saveGeneration(record.id), '生成记录已保存')
}

async function confirmRecord(record: GenerationRecord) {
  await mutateRecord(() => confirmGeneration(record.id), '已完成人工确认')
}

async function mutateRecord(action: () => Promise<GenerationRecord>, message: string) {
  try {
    const updated = await action()
    records.value = records.value.map((item) => (item.id === updated.id ? updated : item))
    selectedRecord.value = updated
    ElMessage.success(message)
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
}

function typeLabel(type: string) {
  return generationTypes.find((item) => item.value === type)?.label || type
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '—'
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败'
}

onMounted(loadPageData)
</script>

<template>
  <div class="history-page" v-loading="loading">
    <section class="history-list-panel panel">
      <div class="panel-header">
        <div><h3 class="panel-title">生成历史</h3><p class="panel-subtitle">追踪 Provider、Prompt 模板与人工确认状态</p></div>
        <span class="record-count mono">{{ records.length }} RECORDS</span>
      </div>

      <div class="filters">
        <el-select v-model="selectedProjectId" placeholder="项目" @change="loadRecords"><el-option v-for="project in projectOptions" :key="project.id ?? 'all'" :label="project.projectName" :value="project.id" /></el-select>
        <el-select v-model="selectedType" placeholder="类型" @change="loadRecords"><el-option v-for="type in generationTypes" :key="type.value" :label="type.label" :value="type.value" /></el-select>
      </div>

      <div class="history-list">
        <div v-if="!records.length" class="empty-state">暂无生成历史</div>
        <button v-for="record in records" :key="record.id" class="history-row" :class="{ active: selectedRecord?.id === record.id }" @click="selectedRecord = record">
          <span class="trace-node" :class="record.status.toLowerCase()" aria-hidden="true"></span>
          <span class="record-main">
            <strong>{{ record.inputSummary }}</strong>
            <span>{{ typeLabel(record.generationType) }} · {{ projectNameMap.get(record.projectId) || `项目 #${record.projectId}` }}</span>
            <small class="mono">{{ record.providerName || 'local-rule' }} / {{ record.modelName }} · {{ record.costTimeMs }}ms</small>
          </span>
          <span class="record-side"><StatusTag :status="record.status" /><small class="mono">{{ formatTime(record.createdAt) }}</small></span>
        </button>
      </div>
    </section>

    <section class="detail-panel">
      <div v-if="selectedRecord" class="detail-actions panel">
        <div class="detail-title">
          <strong>#{{ selectedRecord.id }} · {{ typeLabel(selectedRecord.generationType) }}</strong>
          <span>{{ projectNameMap.get(selectedRecord.projectId) || `项目 #${selectedRecord.projectId}` }} · {{ formatTime(selectedRecord.createdAt) }}</span>
        </div>
        <div class="action-group">
          <CopyButton :text="selectedRecord.outputContent || ''" />
          <el-button v-if="selectedRecord.status === 'READY_FOR_REVIEW'" :icon="DocumentChecked" @click="saveRecord(selectedRecord)">保存记录</el-button>
          <el-button v-if="selectedRecord.status === 'SAVED'" :icon="CircleCheck" type="success" @click="confirmRecord(selectedRecord)">人工确认</el-button>
          <StatusTag v-if="['CONFIRMED', 'FAILED'].includes(selectedRecord.status)" :status="selectedRecord.status" />
        </div>
      </div>

      <div v-if="selectedRecord" class="telemetry panel">
        <div><span>Provider</span><strong>{{ selectedRecord.providerName || '—' }}</strong></div>
        <div><span>模型</span><strong>{{ selectedRecord.modelName || '—' }}</strong></div>
        <div><span>延迟</span><strong class="mono">{{ selectedRecord.costTimeMs }}ms</strong></div>
        <div><span>Token</span><strong class="mono">{{ selectedRecord.totalTokens ?? '—' }}</strong></div>
        <div><span>Prompt 模板</span><strong>{{ selectedRecord.promptTemplateName || '内置模板' }}</strong></div>
        <div><span>模板版本</span><strong class="mono">v{{ selectedRecord.promptTemplateVersion ?? 1 }}</strong></div>
        <div><span>保存状态</span><strong>{{ ['SAVED', 'CONFIRMED'].includes(selectedRecord.status) ? '已保存' : '未保存' }}</strong></div>
        <div><span>确认状态</span><strong>{{ selectedRecord.confirmed ? '已确认' : '待确认' }}</strong></div>
      </div>

      <div v-if="selectedRecord?.errorMessage" class="error-panel panel"><strong>生成备注 / 失败原因</strong><span>{{ selectedRecord.errorMessage }}</span></div>

      <div v-if="selectedRecord" class="detail-workspace">
        <section class="prompt-preview panel">
          <div class="panel-header"><div><h3 class="panel-title">已渲染 Prompt</h3><p class="panel-subtitle">记录生成时实际使用的模板内容</p></div></div>
          <pre>{{ selectedRecord.renderedPrompt || selectedRecord.inputContent }}</pre>
        </section>
        <MarkdownPanel :content="selectedRecord.outputContent || selectedRecord.errorMessage || '暂无生成内容'" :title="`生成 Artifact · ${selectedRecord.status}`" />
      </div>

      <div v-else class="panel empty-detail"><el-icon><View /></el-icon><span>选择一条历史记录查看完整追踪信息</span></div>
    </section>
  </div>
</template>

<style scoped>
.history-page { display: grid; grid-template-columns: minmax(350px, 390px) minmax(0, 1fr); gap: 12px; min-width: 0; align-items: start; }
.history-page > *, .panel-header > *, .history-row > *, .detail-actions > *, .telemetry > *, .detail-workspace > * { min-width: 0; }
.record-count { color: var(--color-text-disabled); font-size: 9px; }.filters { padding: 10px; display: grid; grid-template-columns: 1fr 1fr; gap: 8px; border-bottom: var(--border-subtle); }
.history-list { display: grid; }.history-row { width: 100%; min-height: 68px; display: grid; grid-template-columns: 8px minmax(0, 1fr) auto; align-items: center; gap: 9px; padding: 9px 10px; border: 0; border-bottom: var(--border-subtle); background: transparent; color: inherit; cursor: pointer; text-align: left; }.history-row:last-child { border-bottom: 0; }.history-row:hover, .history-row.active { background: var(--color-active-row); }.history-row.active { box-shadow: inset 2px 0 var(--color-accent); }
.trace-node { width: 7px; height: 7px; border: 1px solid var(--color-text-ghost); border-radius: 2px; }.trace-node.ready_for_review { border-color: var(--color-warning); background: var(--color-warning); }.trace-node.saved { border-color: var(--color-info); background: var(--color-info); }.trace-node.confirmed { border-color: var(--color-accent); background: var(--color-accent); }.trace-node.failed { border-color: var(--color-error); background: var(--color-error); }.trace-node.generating { border-color: var(--color-accent); animation: pulse 1.5s infinite; }
.record-main strong, .record-main span, .record-main small, .record-side small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.record-main strong { font-size: 11px; }.record-main span { margin-top: 3px; color: var(--muted); font-size: 9px; }.record-main small { margin-top: 5px; color: var(--color-text-disabled); font-size: 8px; }.record-side { display: grid; justify-items: end; gap: 5px; }.record-side small { max-width: 108px; color: var(--color-text-disabled); font-size: 8px; }
.detail-panel { min-width: 0; display: grid; gap: 10px; }.detail-actions { min-height: 48px; padding: 8px 10px; display: flex; justify-content: space-between; align-items: center; gap: 10px; }.detail-title strong, .detail-title span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.detail-title strong { font-size: 12px; }.detail-title span { margin-top: 3px; color: var(--muted); font-size: 9px; }.action-group { display: flex; align-items: center; gap: 7px; flex-wrap: wrap; }
.telemetry { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1px; padding: 1px; background: var(--color-border-subtle); }.telemetry div { padding: 8px 9px; background: var(--color-surface); }.telemetry span, .telemetry strong { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.telemetry span { color: var(--muted); font-size: 9px; }.telemetry strong { margin-top: 4px; font-size: 10px; }
.error-panel { padding: 9px 10px; border-left: 2px solid var(--color-warning); }.error-panel strong, .error-panel span { display: block; }.error-panel strong { color: var(--color-warning); font-size: 10px; }.error-panel span { margin-top: 4px; color: var(--muted); font-size: 10px; }
.detail-workspace { display: grid; grid-template-columns: minmax(280px, 5fr) minmax(0, 7fr); gap: 10px; align-items: start; }.prompt-preview { overflow: hidden; }.prompt-preview pre { margin: 0; padding: 11px; overflow: auto; color: var(--muted); background: var(--color-bg); font: 10px/1.65 var(--font-mono); white-space: pre-wrap; overflow-wrap: anywhere; }.empty-detail { min-height: 220px; display: grid; place-items: center; color: var(--muted); }
@media (max-width: 1180px) { .history-page { grid-template-columns: 330px minmax(0, 1fr); }.telemetry { grid-template-columns: repeat(2, minmax(0, 1fr)); }.detail-workspace { grid-template-columns: 1fr; } }
@media (max-width: 900px) { .history-page { grid-template-columns: 1fr; } }
</style>
