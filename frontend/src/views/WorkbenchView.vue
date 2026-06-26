<script setup lang="ts">
import { computed, onMounted, reactive, shallowRef, watch } from 'vue'
import { ArrowRight, Check, CircleCheck, DocumentCopy, MagicStick, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  confirmGeneration,
  fetchAgentRuns,
  fetchAgentRunTrace,
  fetchGenerationTraces,
  fetchGenerations,
  fetchKnowledgeReferences,
  fetchProjects,
  fetchPrompts,
  generateAi,
  saveGeneration,
} from '@/api/devflow'
import CodeBlock from '@/components/CodeBlock.vue'
import ProviderBadge from '@/components/ProviderBadge.vue'
import SectionCard from '@/components/SectionCard.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import type {
  AgentRunTrace,
  AgentStep,
  AiGenerateResponse,
  GenerationRecord,
  GenerationTrace,
  HumanReview,
  KnowledgeReference,
  ProjectContext,
  PromptTemplate,
  ToolCallRecord,
} from '@/types/domain'

type ResultTab = 'preview' | 'diff' | 'raw' | 'json'

interface StructuredArtifact {
  status: string
  module: string
  features: string[]
  files_created: string[]
  files_changed: string[]
  tests_added: string[]
  dependencies: string[]
  summary: string
  source: 'response-json' | 'derived'
}

interface TraceRow {
  key: string
  name: string
  status: string
  summary: string
  latencyMs?: number
  time?: string
}

interface StatusHistoryRow {
  key: string
  label: string
  status: string
  detail: string
  time?: string
}

// Workbench still sends the original real generation payload. UI-only fields
// such as branch, priority, language, and context files are local configuration
// metadata until the backend exposes persisted fields for them.
const WORKBENCH_SAFE_FALLBACK = {
  providerName: 'local-rule',
  modelName: 'local-rule',
  reviewer: '未分配',
  branch: 'main',
  tracePending: '待生成',
  fallbackPolicy: '由后端 Provider Router 决定；无 Key 时可降级 local-rule。',
} as const

const generationTypes = [
  { label: '需求拆解', value: 'requirement-split' },
  { label: '代码变更计划', value: 'code-plan' },
  { label: 'README 内容', value: 'readme-generate' },
  { label: 'Commit Message', value: 'commit-message' },
  { label: '修复 Prompt', value: 'fix-prompt' },
]

const priorityOptions = ['高', '中', '低']
const languageOptions = ['Java / Vue', 'Java', 'TypeScript', 'Markdown', 'JSON']
const defaultConstraints = ['保留现有 REST API', '不提交 API Key', '所有输出进入 Human Review']

const projects = shallowRef<ProjectContext[]>([])
const prompts = shallowRef<PromptTemplate[]>([])
const recentRecords = shallowRef<GenerationRecord[]>([])
const selectedProjectId = shallowRef<number>()
const loading = shallowRef(false)
const result = shallowRef<AiGenerateResponse>()
const agentTrace = shallowRef<AgentRunTrace>()
const generationTraces = shallowRef<GenerationTrace[]>([])
const fetchedKnowledgeReferences = shallowRef<KnowledgeReference[]>([])
const activeResultTab = shallowRef<ResultTab>('preview')
const draftSnapshot = shallowRef<{ title: string; savedAt: string }>()

const form = reactive({
  title: '日志分析历史筛选',
  type: 'requirement-split',
  priority: '高',
  input: '为 DevFlow Copilot 增加日志分析历史筛选，并保证所有输出需要人工确认。',
  extraContext: '',
  knowledgeQuery: 'DevFlow Copilot local-rule Provider Generation Trace',
  templateId: undefined as number | undefined,
  branch: 'main',
  contextFiles: 'frontend/src/views, backend/src/main/java',
  focusArea: '状态机、API 边界、人工审核',
  language: 'Java / Vue',
  constraints: defaultConstraints.join('\n'),
})

const selectedProject = computed(() => projects.value.find((project) => project.id === selectedProjectId.value))
const availableTemplates = computed(() => prompts.value.filter((item) => item.enabled && item.templateType === form.type))
const selectedTemplate = computed(() => prompts.value.find((item) => item.id === form.templateId))
const currentRecord = computed(() => recentRecords.value.find((record) => record.id === result.value?.recordId))
const displayStatus = computed(() => (loading.value ? 'GENERATING' : result.value?.status || currentRecord.value?.status || 'DRAFT'))
const canSave = computed(() => result.value?.status === 'READY_FOR_REVIEW')
const canConfirm = computed(() => result.value?.status === 'SAVED')
const knowledgeReferences = computed(() => result.value?.knowledgeReferences?.length ? result.value.knowledgeReferences : fetchedKnowledgeReferences.value)
const humanReview = computed<HumanReview | undefined>(() => agentTrace.value?.humanReviews[0])
const toolCalls = computed(() => agentTrace.value?.toolCalls || [])
const traceId = computed(() => result.value?.recordId ? `trace_${result.value.recordId}` : WORKBENCH_SAFE_FALLBACK.tracePending)
const agentRunId = computed(() => result.value?.agentRunId || agentTrace.value?.run.id)
const totalTokenCount = computed(() => (result.value?.totalTokens ?? ((result.value?.promptTokens || 0) + (result.value?.completionTokens || 0))) || undefined)
const startedAt = computed(() => agentTrace.value?.run.startedAt || generationTraces.value[0]?.createdAt)
const completedAt = computed(() => agentTrace.value?.run.completedAt || currentRecord.value?.updatedAt)

const structuredArtifact = computed(() => buildStructuredArtifact(result.value?.outputContent || ''))
const suggestedFiles = computed(() => {
  const created = structuredArtifact.value.files_created.map((name) => ({ name, type: '新建', status: displayStatus.value }))
  const changed = structuredArtifact.value.files_changed.map((name) => ({ name, type: '修改', status: displayStatus.value }))
  return [...created, ...changed]
})
const diffContent = computed(() => extractDiff(result.value?.outputContent || ''))
const rawJson = computed(() => {
  if (!result.value) return '{\n  "status": "DRAFT"\n}'
  return JSON.stringify(result.value, null, 2)
})
const structuredJson = computed(() => JSON.stringify(structuredArtifact.value, null, 2))
const templateVariables = computed(() => parseVariables(selectedTemplate.value?.variables))
const constraintRows = computed(() => form.constraints.split(/\r?\n/).map((item) => item.trim()).filter(Boolean))

const traceRows = computed<TraceRow[]>(() => {
  if (agentTrace.value?.steps.length) {
    return agentTrace.value.steps
      .slice()
      .sort((a, b) => a.stepOrder - b.stepOrder)
      .map((step) => ({
        key: `step-${step.id}`,
        name: step.stepName || step.stepType,
        status: step.status,
        summary: step.summary || step.stepType,
        latencyMs: step.latencyMs,
        time: step.completedAt || step.startedAt,
      }))
  }

  if (generationTraces.value.length) {
    return generationTraces.value.map((trace) => ({
      key: `trace-${trace.id}`,
      name: 'Generation Trace',
      status: trace.status,
      summary: trace.renderedPromptSummary || trace.providerName || '后端生成追踪',
      latencyMs: trace.latencyMs,
      time: trace.createdAt,
    }))
  }

  return []
})

const statusHistory = computed<StatusHistoryRow[]>(() => {
  const rows: StatusHistoryRow[] = [
    {
      key: 'draft',
      label: '草稿已就绪',
      status: 'DRAFT',
      detail: selectedProject.value ? `项目上下文：${selectedProject.value.projectName}` : '等待选择项目',
    },
  ]

  if (result.value || loading.value) {
    rows.push({
      key: 'generating',
      label: loading.value ? '运行中' : 'Provider 调用完成',
      status: loading.value ? 'GENERATING' : displayStatus.value,
      detail: `${result.value?.providerName || WORKBENCH_SAFE_FALLBACK.providerName} · ${formatDuration(result.value?.costTimeMs)}`,
      time: startedAt.value,
    })
  }

  if (result.value?.status === 'READY_FOR_REVIEW' || canSave.value || canConfirm.value || result.value?.status === 'CONFIRMED') {
    rows.push({
      key: 'review',
      label: '等待人工审核',
      status: 'READY_FOR_REVIEW',
      detail: '后端状态机要求先保存记录，再确认。',
      time: completedAt.value || '',
    })
  }

  if (result.value?.status === 'SAVED' || result.value?.status === 'CONFIRMED') {
    rows.push({
      key: 'saved',
      label: '生成记录已保存',
      status: 'SAVED',
      detail: `Generation Record #${result.value.recordId}`,
      time: completedAt.value,
    })
  }

  if (result.value?.status === 'CONFIRMED') {
    rows.push({
      key: 'confirmed',
      label: '人工审核已确认',
      status: 'CONFIRMED',
      detail: 'Artifact 已完成 Human Review 闭环。',
      time: completedAt.value,
    })
  }

  if (result.value?.status === 'FAILED') {
    rows.push({
      key: 'failed',
      label: '生成失败',
      status: 'FAILED',
      detail: result.value.errorMessage || 'Provider 返回失败状态。',
      time: completedAt.value,
    })
  }

  return rows
})

const logRows = computed<Array<{ key: string; level: string; message: string; time?: string }>>(() => {
  const errors: Array<{ key: string; level: string; message: string; time?: string }> = generationTraces.value
    .filter((trace) => trace.errorMessage)
    .map((trace) => ({
      key: `trace-error-${trace.id}`,
      level: 'ERROR',
      message: trace.errorMessage || '',
      time: trace.createdAt,
    }))

  if (result.value?.errorMessage) {
    errors.unshift({
      key: `result-error-${result.value.recordId}`,
      level: 'ERROR',
      message: result.value.errorMessage,
      time: completedAt.value,
    })
  }

  return errors
})

const toolSummary = computed(() => {
  const total = toolCalls.value.length
  const success = toolCalls.value.filter((tool) => ['SUCCESS', 'PASSED', 'CONFIRMED'].includes(normalizeStatus(tool.status))).length
  const failed = toolCalls.value.filter((tool) => ['FAILED', 'ERROR', 'REJECTED'].includes(normalizeStatus(tool.status))).length
  const latencyMs = toolCalls.value.reduce((sum, tool) => sum + (tool.latencyMs || 0), 0)
  return { total, success, failed, latencyMs }
})

const topTools = computed(() => {
  const counts = new Map<string, number>()
  toolCalls.value.forEach((tool) => counts.set(tool.toolName, (counts.get(tool.toolName) || 0) + 1))
  return Array.from(counts.entries())
    .map(([toolName, count]) => ({ toolName, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 4)
})

watch(
  () => form.type,
  () => selectDefaultTemplate(),
)

async function loadPageData() {
  try {
    const [projectData, recordData, promptData] = await Promise.all([fetchProjects(), fetchGenerations(), fetchPrompts()])
    projects.value = projectData
    prompts.value = promptData
    selectedProjectId.value = projectData[0]?.id
    recentRecords.value = recordData.slice(0, 5)
    selectDefaultTemplate()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
}

function selectDefaultTemplate() {
  const candidates = prompts.value.filter((item) => item.enabled && item.templateType === form.type)
  form.templateId = candidates.find((item) => item.isDefault)?.id || candidates[0]?.id
}

async function runGenerate() {
  const input = buildRequestInput()
  if (!input.trim()) {
    ElMessage.warning('请先输入任务标题或任务描述')
    return
  }
  loading.value = true
  result.value = undefined
  agentTrace.value = undefined
  generationTraces.value = []
  fetchedKnowledgeReferences.value = []
  activeResultTab.value = 'preview'
  try {
    result.value = await generateAi(form.type, {
      projectId: selectedProjectId.value,
      input,
      extraContext: form.extraContext,
      knowledgeQuery: form.knowledgeQuery,
      templateId: form.templateId,
    })
    ElMessage.success('生成完成，等待人工确认')
    await refreshExecutionDetails(result.value.recordId, result.value.agentRunId)
    recentRecords.value = (await fetchGenerations()).slice(0, 5)
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    loading.value = false
  }
}

async function saveCurrent() {
  if (!result.value?.recordId || !canSave.value) return
  try {
    const updated = await saveGeneration(result.value.recordId)
    result.value = { ...result.value, status: updated.status }
    await refreshExecutionDetails(result.value.recordId, result.value.agentRunId)
    recentRecords.value = (await fetchGenerations()).slice(0, 5)
    ElMessage.success('生成记录已保存')
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
}

async function confirmCurrent() {
  if (!result.value?.recordId || !canConfirm.value) return
  try {
    const updated = await confirmGeneration(result.value.recordId)
    result.value = { ...result.value, status: updated.status }
    await refreshExecutionDetails(result.value.recordId, result.value.agentRunId)
    recentRecords.value = (await fetchGenerations()).slice(0, 5)
    ElMessage.success('已完成人工确认')
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
}

async function refreshExecutionDetails(recordId?: number, knownAgentRunId?: number) {
  if (!recordId) return
  const [traceData, referenceData] = await Promise.all([
    fetchGenerationTraces({ generationRecordId: recordId }).catch(() => [] as GenerationTrace[]),
    fetchKnowledgeReferences(recordId).catch(() => [] as KnowledgeReference[]),
  ])
  generationTraces.value = traceData
  fetchedKnowledgeReferences.value = referenceData

  if (knownAgentRunId) {
    agentTrace.value = await fetchAgentRunTrace(knownAgentRunId).catch(() => undefined)
    return
  }

  const runs = await fetchAgentRuns({ generationRecordId: recordId }).catch(() => [])
  if (runs[0]) {
    agentTrace.value = await fetchAgentRunTrace(runs[0].id).catch(() => undefined)
  }
}

function saveDraft() {
  draftSnapshot.value = {
    title: form.title || '未命名任务',
    savedAt: new Date().toISOString(),
  }
  ElMessage.info('草稿已保留在当前页面，未写入后端')
}

async function copyResult() {
  const content = result.value?.outputContent || ''
  if (!content) {
    ElMessage.warning('暂无可复制的生成结果')
    return
  }
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('生成结果已复制')
  } catch {
    ElMessage.error('复制失败，请手动选择文本')
  }
}

function openTrace() {
  if (!result.value?.recordId) {
    ElMessage.info('运行工作流后可查看完整 Trace')
    return
  }
  window.location.hash = `#/agent-runs?generationRecordId=${result.value.recordId}`
}

function openPrompts() {
  window.location.hash = '#/prompts'
}

function openKnowledge() {
  window.location.hash = '#/knowledge'
}

function buildRequestInput() {
  const title = form.title.trim()
  const body = form.input.trim()
  return [title, body].filter(Boolean).join('\n\n')
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
}

function normalizeStatus(status?: string) {
  if (!status) return 'DRAFT'
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

function statusText(status: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    GENERATING: '运行中',
    READY_FOR_REVIEW: '待审核',
    SAVED: '已保存',
    CONFIRMED: '已确认',
    FAILED: '失败',
    SUCCESS: '成功',
  }
  return map[normalizeStatus(status)] || status
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

function formatDuration(value?: number) {
  const ms = Math.max(value || 0, 0)
  if (ms >= 1000) return `${(ms / 1000).toFixed(ms >= 10000 ? 1 : 2)}s`
  return `${ms}ms`
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '--'
}

function formatScore(value?: number) {
  return Number.isFinite(value) ? Number(value).toFixed(2) : '0.00'
}

function formatReviewStatus(status?: string) {
  const map: Record<string, string> = {
    PENDING: '待审核',
    SAVED: '已保存',
    CONFIRMED: '已确认',
    REJECTED: '已驳回',
  }
  return map[normalizeStatus(status)] || '待审核'
}

function parseVariables(raw?: string) {
  if (!raw?.trim()) return []
  try {
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) return parsed.map(String)
    if (parsed && typeof parsed === 'object') return Object.keys(parsed)
  } catch {
    // variables can also be stored as comma-separated text in demo data
  }
  return raw.split(/[,，\s]+/).map((item) => item.trim()).filter(Boolean)
}

function tryParseJson(text: string) {
  const trimmed = text.trim()
  if (!trimmed) return undefined
  const jsonBlock = trimmed.match(/```json\s*([\s\S]*?)```/i)?.[1]
  const candidates = [jsonBlock, trimmed].filter(Boolean) as string[]
  for (const candidate of candidates) {
    try {
      return JSON.parse(candidate)
    } catch {
      // continue trying other candidates
    }
  }
  return undefined
}

function buildStructuredArtifact(text: string): StructuredArtifact {
  const parsed = tryParseJson(text)
  const source = parsed ? 'response-json' : 'derived'
  return {
    status: String(readField(parsed, ['status']) || displayStatus.value),
    module: String(readField(parsed, ['module', 'package', 'domain']) || selectedProject.value?.projectName || '未返回'),
    features: toStringArray(readField(parsed, ['features', 'feature_list'])),
    files_created: toStringArray(readField(parsed, ['files_created', 'filesCreated', 'createdFiles'])),
    files_changed: toStringArray(readField(parsed, ['files_changed', 'filesChanged', 'changedFiles'])),
    tests_added: toStringArray(readField(parsed, ['tests_added', 'testsAdded', 'tests'])),
    dependencies: toStringArray(readField(parsed, ['dependencies', 'deps'])),
    summary: String(readField(parsed, ['summary', 'description']) || summarizeText(text)),
    source,
  }
}

function readField(source: unknown, keys: string[]) {
  if (!source || typeof source !== 'object') return undefined
  const record = source as Record<string, unknown>
  for (const key of keys) {
    if (record[key] !== undefined) return record[key]
  }
  return undefined
}

function toStringArray(value: unknown) {
  if (Array.isArray(value)) return value.map(String).filter(Boolean)
  if (typeof value === 'string' && value.trim()) return [value.trim()]
  return []
}

function summarizeText(text: string) {
  const compact = text.replace(/```[\s\S]*?```/g, '').replace(/\s+/g, ' ').trim()
  if (!compact) return '尚未生成结构化 summary。'
  return compact.slice(0, 120)
}

function extractDiff(text: string) {
  return text.match(/```diff\s*([\s\S]*?)```/i)?.[1]?.trim() || ''
}

function toolStatus(tool: ToolCallRecord) {
  return normalizeStatus(tool.status)
}

function stepStatus(step: AgentStep) {
  return normalizeStatus(step.status)
}

onMounted(loadPageData)
</script>

<template>
  <div class="workbench workbench-redesign" v-loading="loading">
    <header class="workbench-header">
      <div>
        <span class="mono header-kicker">DEVFLOW / WORKBENCH</span>
        <h1>DevFlow 工作台</h1>
        <p>输入 AI Coding 任务，选择项目上下文和 Prompt 模板，运行 Provider 调用并进入 Human Review。</p>
      </div>
      <div class="header-actions">
        <StatusBadge :status="displayStatus" :label="statusText(displayStatus)" />
        <ProviderBadge :provider="result?.providerName || WORKBENCH_SAFE_FALLBACK.providerName" :model="result?.modelName || WORKBENCH_SAFE_FALLBACK.modelName" />
      </div>
    </header>

    <main class="workbench-shell">
      <aside class="left-pane">
        <SectionCard title="任务输入" subtitle="真实生成请求仍由任务类型、项目、Prompt、输入文本组成" eyebrow="Task Config">
          <div class="form-grid">
            <label>
              <span>任务标题</span>
              <el-input v-model="form.title" placeholder="输入任务标题" />
            </label>
            <label>
              <span>任务描述</span>
              <el-input
                v-model="form.input"
                class="task-editor"
                type="textarea"
                :autosize="{ minRows: 7, maxRows: 11 }"
                placeholder="描述目标、约束、验收标准。"
                @keydown.ctrl.enter.prevent="runGenerate"
                @keydown.meta.enter.prevent="runGenerate"
              />
            </label>
            <div class="two-col">
              <label>
                <span>优先级</span>
                <el-select v-model="form.priority">
                  <el-option v-for="priority in priorityOptions" :key="priority" :label="priority" :value="priority" />
                </el-select>
              </label>
              <label>
                <span>任务类型</span>
                <el-select v-model="form.type">
                  <el-option v-for="item in generationTypes" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </label>
            </div>
          </div>
        </SectionCard>

        <SectionCard title="项目上下文" subtitle="项目与上下文字段用于配置工作台视图" eyebrow="Project Context" compact>
          <div class="context-stack">
            <label>
              <span>仓库 / 项目</span>
              <el-select v-model="selectedProjectId" placeholder="选择项目">
                <el-option v-for="project in projects" :key="project.id" :label="project.projectName" :value="project.id" />
              </el-select>
            </label>
            <div class="two-col">
              <label>
                <span>分支</span>
                <el-input v-model="form.branch" />
              </label>
              <label>
                <span>语言</span>
                <el-select v-model="form.language">
                  <el-option v-for="language in languageOptions" :key="language" :label="language" :value="language" />
                </el-select>
              </label>
            </div>
            <label>
              <span>上下文文件</span>
              <el-input v-model="form.contextFiles" />
            </label>
            <label>
              <span>关注区域</span>
              <el-input v-model="form.focusArea" />
            </label>
            <dl v-if="selectedProject" class="project-meta">
              <div><dt>技术栈</dt><dd>{{ selectedProject.techStack }}</dd></div>
              <div><dt>当前需求</dt><dd>{{ selectedProject.currentRequirement }}</dd></div>
              <div><dt>开发约束</dt><dd>{{ selectedProject.codingRules }}</dd></div>
            </dl>
          </div>
        </SectionCard>

        <SectionCard title="Prompt 模板选择" subtitle="模板来自真实 Prompt 接口" eyebrow="Prompt Template" compact>
          <template #actions>
            <button class="section-link" type="button" @click="openPrompts">
              查看模板
              <el-icon><ArrowRight /></el-icon>
            </button>
          </template>

          <div class="prompt-config">
            <label>
              <span>当前模板</span>
              <el-select v-model="form.templateId" placeholder="使用默认模板">
                <el-option
                  v-for="item in availableTemplates"
                  :key="item.id"
                  :label="`${item.templateName} · v${item.version}${item.isDefault ? ' · 默认' : ''}`"
                  :value="item.id"
                />
              </el-select>
            </label>
            <div class="variable-list">
              <span class="list-label">变量配置</span>
              <div v-if="!templateVariables.length" class="mini-empty">当前模板未返回变量配置。</div>
              <span v-for="variable in templateVariables" :key="variable" class="variable-chip mono">{{ variable }}</span>
            </div>
            <label>
              <span>约束条件</span>
              <el-input v-model="form.constraints" type="textarea" :autosize="{ minRows: 3, maxRows: 5 }" />
            </label>
          </div>
        </SectionCard>

        <section class="action-bar">
          <button class="primary-action" type="button" :disabled="loading" @click="runGenerate">
            <el-icon><MagicStick /></el-icon>
            运行工作流
          </button>
          <button class="ghost-action" type="button" @click="saveDraft">保存草稿</button>
          <span v-if="draftSnapshot" class="mono draft-note">本页草稿 {{ formatTime(draftSnapshot.savedAt).slice(11, 16) }}</span>
        </section>
      </aside>

      <section class="center-pane">
        <SectionCard class="result-card" title="生成结果" subtitle="预览、差异、原始响应和 JSON 结构化视图" eyebrow="Generated Artifact">
          <template #actions>
            <div class="result-actions">
              <StatusBadge :status="displayStatus" :label="statusText(displayStatus)" />
              <button class="icon-action" type="button" @click="copyResult">
                <el-icon><DocumentCopy /></el-icon>
                复制
              </button>
            </div>
          </template>

          <div class="result-tabs" role="tablist" aria-label="结果视图">
            <button type="button" :class="{ active: activeResultTab === 'preview' }" @click="activeResultTab = 'preview'">预览</button>
            <button type="button" :class="{ active: activeResultTab === 'diff' }" @click="activeResultTab = 'diff'">差异</button>
            <button type="button" :class="{ active: activeResultTab === 'raw' }" @click="activeResultTab = 'raw'">原始</button>
            <button type="button" :class="{ active: activeResultTab === 'json' }" @click="activeResultTab = 'json'">JSON</button>
          </div>

          <div class="result-panel">
            <CodeBlock
              v-if="activeResultTab === 'preview'"
              :code="result?.outputContent || '尚未运行工作流。生成结果会在这里显示，并在后端返回 READY_FOR_REVIEW 后进入人工审核。'"
              language="markdown"
              title="Preview"
            />
            <div v-else-if="activeResultTab === 'diff'" class="empty-diff">
              <CodeBlock v-if="diffContent" :code="diffContent" language="diff" title="Diff" />
              <div v-else class="empty-state">当前后端响应未返回真实 diff。已保留原始响应，可在“原始”或“JSON”视图检查。</div>
            </div>
            <CodeBlock
              v-else-if="activeResultTab === 'raw'"
              :code="result?.outputContent || '尚未生成原始响应。'"
              language="text"
              title="Raw Response"
            />
            <CodeBlock v-else :code="structuredJson" language="json" title="Structured JSON" />
          </div>
        </SectionCard>

        <SectionCard title="结构化响应" :subtitle="structuredArtifact.source === 'response-json' ? '来自响应 JSON' : '从响应文本安全派生，不伪装后端字段'" eyebrow="Structured Response" compact>
          <div class="structured-grid">
            <div><span>status</span><strong class="mono">{{ structuredArtifact.status }}</strong></div>
            <div><span>module</span><strong>{{ structuredArtifact.module }}</strong></div>
            <div><span>features</span><strong>{{ structuredArtifact.features.length || 0 }}</strong></div>
            <div><span>files_created</span><strong>{{ structuredArtifact.files_created.length }}</strong></div>
            <div><span>files_changed</span><strong>{{ structuredArtifact.files_changed.length }}</strong></div>
            <div><span>tests_added</span><strong>{{ structuredArtifact.tests_added.length }}</strong></div>
            <div><span>dependencies</span><strong>{{ structuredArtifact.dependencies.length }}</strong></div>
            <div class="wide"><span>summary</span><strong>{{ structuredArtifact.summary }}</strong></div>
          </div>
        </SectionCard>

        <SectionCard title="生成产物 / 建议文件" subtitle="仅展示响应中可解析出的文件，不构造虚假 diff" eyebrow="Artifacts" compact>
          <div class="file-list">
            <div v-if="!suggestedFiles.length" class="empty-state">当前响应未返回 files_created / files_changed。</div>
            <article v-for="file in suggestedFiles" :key="`${file.type}-${file.name}`">
              <span class="file-icon mono">{{ file.type === '新建' ? 'N' : 'M' }}</span>
              <strong>{{ file.name }}</strong>
              <span>{{ file.type }}</span>
              <StatusBadge :status="file.status" :label="statusText(file.status)" />
            </article>
          </div>
        </SectionCard>

        <section class="review-bar">
          <div>
            <strong>Human Review 停点</strong>
            <span>保存记录后才能标记已审核并确认，继续沿用后端状态机。</span>
          </div>
          <div class="review-actions">
            <button class="ghost-action" type="button" :disabled="loading" @click="runGenerate">
              <el-icon><RefreshRight /></el-icon>
              重新生成
            </button>
            <button class="ghost-action" type="button" :disabled="!canSave" @click="saveCurrent">保存记录</button>
            <button class="primary-action" type="button" :disabled="!canConfirm" @click="confirmCurrent">
              <el-icon><CircleCheck /></el-icon>
              标记已审核 / 确认
            </button>
          </div>
        </section>
      </section>

      <aside class="right-pane">
        <SectionCard title="执行详情" subtitle="来自生成响应、Generation Trace 与 Agent Run Trace" eyebrow="Execution">
          <div class="detail-grid">
            <span>Provider</span><ProviderBadge :provider="result?.providerName || WORKBENCH_SAFE_FALLBACK.providerName" :model="result?.modelName || WORKBENCH_SAFE_FALLBACK.modelName" />
            <span>Model</span><strong class="mono">{{ result?.modelName || WORKBENCH_SAFE_FALLBACK.modelName }}</strong>
            <span>降级策略</span><strong>{{ WORKBENCH_SAFE_FALLBACK.fallbackPolicy }}</strong>
            <span>耗时</span><strong class="mono">{{ formatDuration(result?.costTimeMs) }}</strong>
            <span>Trace ID</span><strong class="mono">{{ traceId }}</strong>
            <span>开始时间</span><strong class="mono">{{ formatTime(startedAt) }}</strong>
            <span>完成时间</span><strong class="mono">{{ formatTime(completedAt) }}</strong>
            <span>Token</span><strong class="mono">{{ totalTokenCount ?? '—' }}</strong>
          </div>
          <button class="wide-link" type="button" @click="openTrace">查看完整 Trace</button>
        </SectionCard>

        <SectionCard title="知识引用" :subtitle="`${knowledgeReferences.length} 条引用`" eyebrow="Knowledge">
          <template #actions>
            <button class="section-link" type="button" @click="openKnowledge">
              查看全部
              <el-icon><ArrowRight /></el-icon>
            </button>
          </template>
          <div class="reference-list">
            <div v-if="!knowledgeReferences.length" class="empty-state">当前生成未返回 Knowledge 引用。</div>
            <article v-for="reference in knowledgeReferences.slice(0, 4)" :key="reference.chunkId">
              <div>
                <strong>{{ reference.documentTitle }}</strong>
                <small>{{ reference.citationLabel }}</small>
              </div>
              <span class="mono">{{ formatScore(reference.score) }}</span>
            </article>
          </div>
        </SectionCard>

        <SectionCard title="人工审核" subtitle="来自 human_review 或当前状态派生" eyebrow="Human Review">
          <div class="review-detail">
            <div><span>状态</span><StatusBadge :status="humanReview?.reviewStatus || displayStatus" :label="formatReviewStatus(humanReview?.reviewStatus || displayStatus)" /></div>
            <div><span>审核人</span><strong>{{ humanReview?.reviewer || WORKBENCH_SAFE_FALLBACK.reviewer }}</strong></div>
            <div><span>审核结论</span><strong>{{ humanReview?.reviewStatus ? formatReviewStatus(humanReview.reviewStatus) : '等待审核' }}</strong></div>
            <div><span>审核意见</span><strong>{{ humanReview?.comment || '暂无审核意见' }}</strong></div>
          </div>
          <button class="wide-link" type="button" @click="openTrace">打开审核面板</button>
        </SectionCard>

        <SectionCard title="工具调用摘要" subtitle="来自 Agent Run Trace 的 tool_call_record" eyebrow="Tool Calls">
          <div class="tool-summary">
            <div><strong>{{ toolSummary.total }}</strong><span>总调用</span></div>
            <div><strong>{{ toolSummary.success }}</strong><span>成功</span></div>
            <div><strong>{{ toolSummary.failed }}</strong><span>失败</span></div>
            <div><strong>{{ formatDuration(toolSummary.latencyMs) }}</strong><span>耗时</span></div>
          </div>
          <div class="top-tools">
            <div v-if="!topTools.length" class="empty-state">暂无工具调用记录。</div>
            <span v-for="tool in topTools" :key="tool.toolName" class="tool-chip mono">{{ tool.toolName }} · {{ tool.count }}</span>
          </div>
        </SectionCard>
      </aside>
    </main>

    <section class="trace-bottom">
      <SectionCard title="生成追踪" subtitle="Agent Step / Generation Trace" eyebrow="Trace" compact>
        <div class="trace-list">
          <div v-if="!traceRows.length" class="empty-state">运行工作流后显示真实生成追踪。</div>
          <article v-for="row in traceRows" :key="row.key" class="trace-row">
            <span class="trace-node" :class="normalizeStatus(row.status).toLowerCase()"></span>
            <div>
              <strong>{{ row.name }}</strong>
              <small>{{ row.summary }}</small>
            </div>
            <StatusBadge :status="row.status" :label="statusText(row.status)" />
            <span class="mono">{{ formatDuration(row.latencyMs) }}</span>
          </article>
        </div>
      </SectionCard>

      <SectionCard title="工具调用" subtitle="Tool Call 明细" eyebrow="Tool Calls" compact>
        <div class="tool-table">
          <div v-if="!toolCalls.length" class="empty-state">暂无真实 Tool Call 记录。</div>
          <article v-for="tool in toolCalls" :key="tool.id">
            <strong>{{ tool.toolName }}</strong>
            <span>{{ tool.inputSummary || '无输入摘要' }}</span>
            <StatusBadge :status="toolStatus(tool)" :label="statusText(toolStatus(tool))" />
            <small class="mono">{{ formatDuration(tool.latencyMs) }}</small>
          </article>
        </div>
      </SectionCard>

      <SectionCard title="状态历史" subtitle="当前 Generation 状态机" eyebrow="State History" compact>
        <div class="history-list">
          <article v-for="row in statusHistory" :key="row.key">
            <span class="history-dot" :data-status="normalizeStatus(row.status)"></span>
            <div>
              <strong>{{ row.label }}</strong>
              <small>{{ row.detail }}</small>
            </div>
            <time class="mono">{{ formatTime(row.time).slice(11) || '--' }}</time>
          </article>
        </div>
      </SectionCard>

      <SectionCard title="日志" subtitle="仅展示真实错误或 Trace errorMessage" eyebrow="Logs" compact>
        <div class="log-list">
          <div v-if="!logRows.length" class="empty-state">当前运行未返回可展示日志。</div>
          <article v-for="row in logRows" :key="row.key">
            <span class="mono">{{ formatTime(row.time).slice(11) }}</span>
            <strong class="mono">[{{ row.level }}]</strong>
            <p>{{ row.message }}</p>
          </article>
        </div>
      </SectionCard>
    </section>
  </div>
</template>

<style scoped>
.workbench-redesign {
  display: grid;
  gap: 12px;
  width: 100%;
  min-width: 0;
}

.workbench-header {
  min-width: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: var(--border-subtle);
}

.header-kicker {
  display: block;
  color: var(--color-accent-cyan);
  font-size: 10px;
  letter-spacing: 0.08em;
}

.workbench-header h1,
.workbench-header p {
  margin: 0;
}

.workbench-header h1 {
  margin-top: 4px;
  color: var(--color-text-primary);
  font-size: 22px;
  line-height: 28px;
  font-weight: 700;
}

.workbench-header p {
  margin-top: 6px;
  color: var(--color-text-secondary);
  font-size: 12px;
  line-height: 18px;
}

.header-actions,
.result-actions,
.review-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.workbench-shell {
  display: grid;
  grid-template-columns: minmax(300px, 0.82fr) minmax(430px, 1.26fr) minmax(310px, 0.92fr);
  gap: 12px;
  min-width: 0;
  align-items: start;
}

.left-pane,
.center-pane,
.right-pane {
  display: grid;
  gap: 12px;
  min-width: 0;
}

.form-grid,
.context-stack,
.prompt-config {
  display: grid;
  gap: 10px;
  min-width: 0;
}

label {
  display: grid;
  gap: 5px;
  min-width: 0;
}

label > span,
.list-label {
  color: var(--color-text-disabled);
  font-size: 11px;
  line-height: 16px;
}

.two-col {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  min-width: 0;
}

.task-editor :deep(textarea) {
  font-family: var(--font-mono);
  line-height: 1.55;
}

.project-meta {
  display: grid;
  margin: 0;
  border: var(--border-subtle);
  border-radius: 6px;
  overflow: hidden;
}

.project-meta div {
  min-width: 0;
  padding: 8px;
  border-bottom: var(--border-subtle);
  background: var(--color-bg);
}

.project-meta div:last-child {
  border-bottom: 0;
}

.project-meta dt,
.project-meta dd {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.project-meta dt {
  color: var(--color-text-disabled);
  font-size: 10px;
}

.project-meta dd {
  margin: 4px 0 0;
  color: var(--color-text-secondary);
  font-size: 11px;
  line-height: 16px;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.section-link,
.icon-action,
.primary-action,
.ghost-action,
.wide-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 30px;
  border-radius: 5px;
  cursor: pointer;
  font-size: 12px;
  white-space: nowrap;
  transition: background-color 140ms ease, border-color 140ms ease, color 140ms ease, opacity 140ms ease;
}

.primary-action {
  padding: 0 12px;
  border: 1px solid var(--color-accent);
  background: var(--color-accent);
  color: #0f1714;
  font-weight: 650;
}

.ghost-action,
.icon-action,
.wide-link {
  padding: 0 10px;
  border: 1px solid var(--color-border);
  background: var(--color-card-soft);
  color: var(--color-text-secondary);
}

.section-link {
  height: 26px;
  padding: 0 8px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--color-running);
}

.primary-action:hover:not(:disabled) {
  border-color: var(--color-accent-hover);
  background: var(--color-accent-hover);
}

.ghost-action:hover:not(:disabled),
.icon-action:hover,
.wide-link:hover,
.section-link:hover {
  border-color: var(--color-accent-border);
  background: var(--color-accent-muted);
  color: var(--color-text-primary);
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.46;
}

.variable-list {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.variable-chip,
.tool-chip {
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  height: 22px;
  display: inline-flex;
  align-items: center;
  padding: 0 7px;
  border: var(--border-subtle);
  border-radius: 4px;
  background: var(--color-bg);
  color: var(--color-text-secondary);
  font-size: 10px;
}

.mini-empty {
  color: var(--color-text-disabled);
  font-size: 11px;
}

.action-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
  padding: 10px;
  border: var(--border-default);
  border-radius: var(--radius-card);
  background: var(--color-card);
}

.draft-note {
  min-width: 0;
  color: var(--color-text-disabled);
  font-size: 10px;
}

.result-card :deep(.section-card__body) {
  padding: 0;
}

.result-tabs {
  display: flex;
  min-width: 0;
  height: 34px;
  padding: 0 10px;
  border-bottom: var(--border-subtle);
  background: var(--color-bg);
}

.result-tabs button {
  height: 34px;
  padding: 0 10px;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: var(--color-text-disabled);
  cursor: pointer;
  font-size: 12px;
}

.result-tabs button.active {
  border-bottom-color: var(--color-accent);
  color: var(--color-text-primary);
}

.result-panel {
  min-width: 0;
  padding: 10px;
}

.result-panel :deep(.code-block) {
  min-height: 320px;
}

.empty-diff {
  min-width: 0;
}

.structured-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  background: var(--color-border-subtle);
}

.structured-grid div {
  min-width: 0;
  padding: 8px;
  background: var(--color-bg);
}

.structured-grid .wide {
  grid-column: span 4;
}

.structured-grid span,
.structured-grid strong {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.structured-grid span {
  color: var(--color-text-disabled);
  font-size: 10px;
}

.structured-grid strong {
  margin-top: 4px;
  color: var(--color-text-primary);
  font-size: 12px;
  white-space: nowrap;
}

.structured-grid .wide strong {
  white-space: normal;
  line-height: 18px;
}

.file-list,
.reference-list,
.trace-list,
.tool-table,
.history-list,
.log-list,
.top-tools {
  display: grid;
  min-width: 0;
}

.file-list article,
.reference-list article,
.trace-row,
.tool-table article,
.history-list article,
.log-list article {
  min-width: 0;
  border-bottom: var(--border-subtle);
}

.file-list article {
  min-height: 42px;
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) 46px 80px;
  align-items: center;
  gap: 8px;
  padding: 7px 0;
}

.file-list article:last-child,
.reference-list article:last-child,
.trace-row:last-child,
.tool-table article:last-child,
.history-list article:last-child,
.log-list article:last-child {
  border-bottom: 0;
}

.file-icon {
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  border: var(--border-subtle);
  border-radius: 4px;
  background: var(--color-bg);
  color: var(--color-accent-cyan);
  font-size: 10px;
}

.file-list strong,
.file-list span,
.reference-list strong,
.reference-list small,
.reference-list span,
.detail-grid strong,
.review-detail strong,
.tool-table strong,
.tool-table span,
.tool-table small,
.history-list strong,
.history-list small,
.history-list time,
.log-list span,
.log-list strong,
.log-list p {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-list strong,
.reference-list strong,
.tool-table strong,
.history-list strong {
  color: var(--color-text-primary);
  font-size: 12px;
}

.file-list span,
.reference-list small,
.tool-table span,
.tool-table small,
.history-list small,
.history-list time,
.log-list span,
.log-list p {
  color: var(--color-text-disabled);
  font-size: 10px;
}

.review-bar {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border: var(--border-default);
  border-radius: var(--radius-card);
  background: var(--color-card);
}

.review-bar strong,
.review-bar span {
  display: block;
}

.review-bar span {
  margin-top: 3px;
  color: var(--color-text-disabled);
  font-size: 10px;
}

.detail-grid,
.review-detail {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 8px 10px;
  min-width: 0;
}

.detail-grid > span,
.review-detail span {
  color: var(--color-text-disabled);
  font-size: 10px;
}

.detail-grid strong,
.review-detail strong {
  color: var(--color-text-primary);
  font-size: 11px;
}

.wide-link {
  width: 100%;
  margin-top: 10px;
}

.reference-list article {
  min-height: 42px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 44px;
  align-items: center;
  gap: 8px;
  padding: 7px 0;
}

.reference-list span {
  color: var(--color-success);
  text-align: right;
}

.review-detail {
  grid-template-columns: 72px minmax(0, 1fr);
}

.review-detail div {
  display: contents;
}

.tool-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  margin-bottom: 10px;
  background: var(--color-border-subtle);
}

.tool-summary div {
  min-width: 0;
  padding: 8px;
  background: var(--color-bg);
  text-align: center;
}

.tool-summary strong,
.tool-summary span {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tool-summary strong {
  color: var(--color-text-primary);
  font-family: var(--font-mono);
  font-size: 14px;
}

.tool-summary span {
  margin-top: 4px;
  color: var(--color-text-disabled);
  font-size: 10px;
}

.top-tools {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.trace-bottom {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr) minmax(260px, 0.74fr) minmax(280px, 0.88fr);
  gap: 12px;
  min-width: 0;
  align-items: start;
}

.trace-row {
  min-height: 48px;
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr) 78px 54px;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
}

.trace-node,
.history-dot {
  width: 8px;
  height: 8px;
  border-radius: 2px;
  background: var(--color-text-disabled);
}

.trace-node.confirmed,
.trace-node.saved,
.trace-node.success,
.history-dot[data-status="CONFIRMED"],
.history-dot[data-status="SAVED"] {
  background: var(--color-success);
}

.trace-node.ready_for_review,
.trace-node.pending,
.history-dot[data-status="READY_FOR_REVIEW"],
.history-dot[data-status="DRAFT"] {
  background: var(--color-warning);
}

.trace-node.generating,
.trace-node.running,
.history-dot[data-status="GENERATING"] {
  background: var(--color-running);
  animation: pulse 1.4s ease-in-out infinite;
}

.trace-node.failed,
.history-dot[data-status="FAILED"] {
  background: var(--color-error);
}

.trace-row div,
.history-list div {
  min-width: 0;
}

.trace-row strong,
.trace-row small {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trace-row strong {
  color: var(--color-text-primary);
  font-size: 12px;
}

.trace-row small {
  margin-top: 3px;
  color: var(--color-text-disabled);
  font-size: 10px;
}

.trace-row > .mono {
  color: var(--color-text-disabled);
  font-size: 10px;
  text-align: right;
}

.tool-table article {
  min-height: 42px;
  display: grid;
  grid-template-columns: minmax(92px, 0.7fr) minmax(0, 1fr) 74px 52px;
  align-items: center;
  gap: 8px;
  padding: 7px 0;
}

.history-list article {
  min-height: 42px;
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr) 58px;
  align-items: center;
  gap: 8px;
  padding: 7px 0;
}

.log-list article {
  min-height: 34px;
  display: grid;
  grid-template-columns: 58px 54px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  padding: 6px 0;
}

.log-list strong {
  color: var(--color-error);
  font-size: 10px;
}

.empty-state {
  min-height: 62px;
  display: grid;
  place-items: center;
  border: 1px dashed var(--color-border);
  border-radius: 6px;
  background: var(--color-bg);
  color: var(--color-text-disabled);
  text-align: center;
  font-size: 12px;
  line-height: 18px;
  padding: 8px;
}

.workbench-redesign > *,
.workbench-shell > *,
.left-pane > *,
.center-pane > *,
.right-pane > *,
.trace-bottom > *,
.workbench-header > *,
.header-actions > *,
.review-bar > *,
.review-actions > *,
.detail-grid > *,
.tool-table > *,
.structured-grid > *,
.file-list > *,
.trace-row > *,
.history-list > *,
.log-list > * {
  min-width: 0;
}

@media (max-width: 1360px) {
  .workbench-shell {
    grid-template-columns: minmax(280px, 0.82fr) minmax(0, 1fr);
  }

  .right-pane {
    grid-column: span 2;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .trace-bottom {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 980px) {
  .workbench-header,
  .review-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .workbench-shell,
  .right-pane,
  .trace-bottom,
  .two-col,
  .structured-grid,
  .tool-summary {
    grid-template-columns: minmax(0, 1fr);
  }

  .structured-grid .wide,
  .right-pane {
    grid-column: auto;
  }

  .tool-table article,
  .trace-row,
  .file-list article {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (prefers-reduced-motion: reduce) {
  .trace-node.generating,
  .trace-node.running,
  .history-dot[data-status="GENERATING"] {
    animation: none;
  }
}
</style>
