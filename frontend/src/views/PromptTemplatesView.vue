<script setup lang="ts">
import { computed, onMounted, reactive, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { CopyDocument, Edit, MagicStick, Plus, RefreshRight, Search, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  createPrompt,
  fetchAgentRunTrace,
  fetchAgentRuns,
  fetchGenerationTraces,
  fetchGenerations,
  fetchProjects,
  fetchPrompts,
  generateAi,
  updatePrompt,
} from '@/api/devflow'
import CodeBlock from '@/components/CodeBlock.vue'
import MetricCard from '@/components/MetricCard.vue'
import ProviderBadge from '@/components/ProviderBadge.vue'
import SectionCard from '@/components/SectionCard.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import type {
  AgentRun,
  AgentRunTrace,
  AiGenerateResponse,
  GenerationRecord,
  GenerationTrace,
  HumanReview,
  ProjectContext,
  PromptTemplate,
} from '@/types/domain'

type PreviewMode = 'raw' | 'rendered' | 'json'
type BottomTab = 'versions' | 'tests' | 'validation' | 'runs' | 'changes'
type ValidationTone = 'success' | 'warning' | 'danger'

interface VariableRow {
  name: string
  sample: string
  required: boolean
  source: '必填变量' | '占位符'
  description: string
}

interface ValidationRow {
  name: string
  status: string
  label: string
  detail: string
  tone: ValidationTone
}

const router = useRouter()

const projects = shallowRef<ProjectContext[]>([])
const prompts = shallowRef<PromptTemplate[]>([])
const generationRecords = shallowRef<GenerationRecord[]>([])
const generationTraces = shallowRef<GenerationTrace[]>([])
const relatedAgentRuns = shallowRef<AgentRun[]>([])
const agentTrace = shallowRef<AgentRunTrace>()

const loading = shallowRef(false)
const saving = shallowRef(false)
const testing = shallowRef(false)
const editingId = shallowRef<number>()
const filterType = shallowRef('')
const filterStatus = shallowRef('')
const searchKeyword = shallowRef('')
const activePreviewMode = shallowRef<PreviewMode>('rendered')
const activeBottomTab = shallowRef<BottomTab>('versions')
const testResult = shallowRef<AiGenerateResponse>()
const testError = shallowRef('')
const changeNote = shallowRef('当前后端以模板记录维护 version；保存更新会递增当前版本号。')

const testForm = reactive<{ projectId?: number; input: string; extraContext: string }>({
  projectId: undefined,
  input: '请基于当前项目边界生成一份可交付的实现计划，并说明需要人工确认的风险点。',
  extraContext: 'Prompt Studio 试运行：仅验证模板渲染、Provider 路径、Generation Trace 与 Human Review 链路。',
})

const variableSamples = reactive<Record<string, string>>({})

const form = reactive({
  templateKey: '',
  templateName: '',
  templateType: 'requirement-split',
  templateContent: '',
  variables: 'projectName,techStack,requirement,context',
  enabled: true,
  isDefault: false,
  version: 1,
})

const templateTypes = [
  { label: '全部类型', value: '' },
  { label: '需求拆解', value: 'requirement-split' },
  { label: '代码修改计划', value: 'code-plan' },
  { label: 'README', value: 'readme-generate' },
  { label: 'Commit Message', value: 'commit-message' },
  { label: '修复 Prompt', value: 'fix-prompt' },
  { label: '日志分析', value: 'log-analysis' },
]

const statusFilters = [
  { label: '全部状态', value: '' },
  { label: '默认模板', value: 'default' },
  { label: '已启用', value: 'enabled' },
  { label: '已停用', value: 'disabled' },
]

const previewModes: { label: string; value: PreviewMode }[] = [
  { label: '原始', value: 'raw' },
  { label: '渲染预览', value: 'rendered' },
  { label: 'JSON', value: 'json' },
]

const bottomTabs: { label: string; value: BottomTab; code: string }[] = [
  { label: '版本历史', value: 'versions', code: 'VERSION' },
  { label: '测试结果', value: 'tests', code: 'TEST' },
  { label: 'Prompt 校验', value: 'validation', code: 'CHECK' },
  { label: '最近运行', value: 'runs', code: 'RUN' },
  { label: '变更记录', value: 'changes', code: 'CHANGE' },
]

const variableHints: Record<string, string> = {
  projectName: '项目名称，由项目上下文提供。',
  techStack: '技术栈摘要，由项目上下文提供。',
  requirement: '本次生成输入或当前需求。',
  context: '补充上下文，来自请求或 README 摘要。',
  codingRules: '项目编码规则和协作边界。',
  directoryStructure: '项目目录结构摘要。',
  errorLog: '日志分析场景的原始异常片段。',
  input: '试运行输入文本。',
}

const testableTypes = new Set(['requirement-split', 'code-plan', 'readme-generate', 'commit-message', 'fix-prompt'])

const selectedTemplate = computed(() => prompts.value.find((item) => item.id === editingId.value))
const selectedProject = computed(() => projects.value.find((item) => item.id === testForm.projectId) || projects.value[0])
const isTestableTemplate = computed(() => testableTypes.has(form.templateType))

const filteredPrompts = computed(() => prompts.value.filter((item) => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  const matchesKeyword = !keyword
    || item.templateName.toLowerCase().includes(keyword)
    || item.templateKey.toLowerCase().includes(keyword)
    || item.templateType.toLowerCase().includes(keyword)
    || (item.variables || '').toLowerCase().includes(keyword)
  const matchesType = !filterType.value || item.templateType === filterType.value
  const matchesStatus = !filterStatus.value
    || (filterStatus.value === 'default' && item.isDefault)
    || (filterStatus.value === 'enabled' && item.enabled)
    || (filterStatus.value === 'disabled' && !item.enabled)
  return matchesKeyword && matchesType && matchesStatus
}))

const currentGenerationRecords = computed(() => {
  const records = editingId.value
    ? generationRecords.value.filter((record) => matchesTemplateRecord(record, {
      id: editingId.value,
      templateName: form.templateName,
      templateType: form.templateType,
      version: form.version,
    }))
    : generationRecords.value
  return records.slice().sort((a, b) => timeValue(b.createdAt) - timeValue(a.createdAt))
})

const lastGenerationRecord = computed(() => currentGenerationRecords.value[0])
const lastTrace = computed(() => generationTraces.value[0])
const lastAgentRun = computed(() => relatedAgentRuns.value[0] || agentTrace.value?.run)
const latestReview = computed<HumanReview | undefined>(() => agentTrace.value?.humanReviews?.[0])

const metricCards = computed(() => [
  { label: 'Prompt 模板', value: prompts.value.length, code: 'TOTAL', tone: 'accent' as const },
  { label: '启用模板', value: prompts.value.filter((item) => item.enabled).length, code: 'ENABLED', tone: 'success' as const },
  { label: '真实生成记录', value: generationRecords.value.filter((item) => item.promptTemplateId || item.promptTemplateName).length, code: 'RUNS', tone: 'running' as const },
  { label: '当前版本', value: editingId.value ? `v${form.version}` : '新建', code: 'VERSION', tone: 'warning' as const },
])

const requiredVariableNames = computed(() => splitVariables(form.variables))

const placeholderNames = computed(() => {
  const names = new Set<string>()
  const pattern = /\{\{\s*([a-zA-Z0-9_.-]+)\s*\}\}/g
  let match = pattern.exec(form.templateContent)
  while (match) {
    names.add(match[1])
    match = pattern.exec(form.templateContent)
  }
  return [...names]
})

const variablesRows = computed<VariableRow[]>(() => {
  const names = new Set([...requiredVariableNames.value, ...placeholderNames.value])
  const required = new Set(requiredVariableNames.value)
  return [...names].map((name) => ({
    name,
    sample: variableSamples[name] || sampleForVariable(name),
    required: required.has(name),
    source: required.has(name) ? '必填变量' : '占位符',
    description: variableHints[name] || '后端未记录变量说明，当前仅作为占位符展示。',
  }))
})

const sampleValues = computed(() => variablesRows.value.reduce<Record<string, string>>((acc, row) => {
  acc[row.name] = variableSamples[row.name] || row.sample
  return acc
}, {}))

const renderedPrompt = computed(() => {
  if (!form.templateContent.trim()) return '未填写模板内容。'
  return form.templateContent.replace(/\{\{\s*([a-zA-Z0-9_.-]+)\s*\}\}/g, (_, name: string) => sampleValues.value[name] || `{{${name}}}`)
})

const promptSegments = computed(() => splitPromptSegments(form.templateContent))

const previewJson = computed(() => JSON.stringify({
  templateKey: form.templateKey || null,
  templateType: form.templateType,
  version: form.version,
  variables: sampleValues.value,
  renderedPrompt: renderedPrompt.value,
  note: '前端仅用于预览；真实渲染仍在生成请求中由后端 PromptTemplateRenderService 完成。',
}, null, 2))

const previewCode = computed(() => {
  if (activePreviewMode.value === 'json') return previewJson.value
  if (activePreviewMode.value === 'raw') return form.templateContent || '未填写模板内容。'
  return renderedPrompt.value
})

const validationRows = computed<ValidationRow[]>(() => {
  const contentLength = form.templateContent.length
  const requiredMissing = requiredVariableNames.value.filter((name) => !placeholderNames.value.includes(name))
  const sensitiveVariables = variablesRows.value
    .map((row) => row.name)
    .filter((name) => /(^|[_.-])(apiKey|api_key|secret|password|accessToken|privateKey|token)([_.-]|$)/i.test(name))
  return [
    {
      name: '内容非空',
      status: form.templateContent.trim() ? 'SUCCESS' : 'FAILED',
      label: form.templateContent.trim() ? '通过' : '需处理',
      detail: form.templateContent.trim() ? '模板内容可保存。' : '模板内容为空，后端会拒绝不可用模板。',
      tone: form.templateContent.trim() ? 'success' : 'danger',
    },
    {
      name: '变量占位符',
      status: placeholderNames.value.length ? 'SUCCESS' : 'PENDING',
      label: placeholderNames.value.length ? '已识别' : '未识别',
      detail: placeholderNames.value.length ? `识别到 ${placeholderNames.value.length} 个 {{variable}} 占位符。` : '未发现占位符；仍可保存，但复用性较弱。',
      tone: placeholderNames.value.length ? 'success' : 'warning',
    },
    {
      name: '必填变量覆盖',
      status: requiredMissing.length ? 'PENDING' : 'SUCCESS',
      label: requiredMissing.length ? '需确认' : '通过',
      detail: requiredMissing.length ? `必填变量未出现在模板中：${requiredMissing.join(', ')}` : 'variables 字段中的必填项均已出现在模板内容中。',
      tone: requiredMissing.length ? 'warning' : 'success',
    },
    {
      name: '长度检查',
      status: contentLength > 20000 ? 'FAILED' : 'SUCCESS',
      label: contentLength > 20000 ? '过长' : '通过',
      detail: `当前长度 ${contentLength} 字符；此处仅做前端静态阈值检查。`,
      tone: contentLength > 20000 ? 'danger' : 'success',
    },
    {
      name: '敏感占位符',
      status: sensitiveVariables.length ? 'PENDING' : 'SUCCESS',
      label: sensitiveVariables.length ? '需人工确认' : '未发现',
      detail: sensitiveVariables.length ? `占位符疑似包含敏感字段：${sensitiveVariables.join(', ')}` : '未发现 API Key、secret、password 等敏感变量名。',
      tone: sensitiveVariables.length ? 'warning' : 'success',
    },
  ]
})

const versionRows = computed(() => {
  if (!editingId.value) return []
  return [{
    version: form.version,
    status: form.enabled ? 'SUCCESS' : 'DRAFT',
    label: form.enabled ? '当前启用' : '当前停用',
    note: changeNote.value || '当前版本记录',
    time: selectedTemplate.value?.updatedAt || selectedTemplate.value?.createdAt,
  }]
})

const changeRows = computed(() => {
  if (!selectedTemplate.value) return []
  const rows = [
    {
      action: '创建模板',
      detail: '来自 Prompt Templates 真实模板记录。',
      time: selectedTemplate.value.createdAt,
    },
  ]
  if (selectedTemplate.value.updatedAt && selectedTemplate.value.updatedAt !== selectedTemplate.value.createdAt) {
    rows.unshift({
      action: '更新当前版本',
      detail: '后端 updatePrompt 已保存并维护 version 字段。',
      time: selectedTemplate.value.updatedAt,
    })
  }
  return rows
})

async function loadPageData() {
  loading.value = true
  try {
    const [projectData, promptData, recordData] = await Promise.all([
      fetchProjects(),
      fetchPrompts(),
      fetchGenerations(),
    ])
    projects.value = projectData
    prompts.value = promptData
    generationRecords.value = recordData
    testForm.projectId = projectData[0]?.id
    const target = promptData.find((item) => item.id === editingId.value) || promptData[0]
    if (target) openEdit(target)
    else openCreate()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    loading.value = false
  }
}

async function refreshPrompts(selectId?: number) {
  const promptData = await fetchPrompts()
  prompts.value = promptData
  const target = promptData.find((item) => item.id === (selectId || editingId.value)) || promptData[0]
  if (target) openEdit(target)
}

async function refreshGenerationRecords() {
  generationRecords.value = await fetchGenerations()
}

function openCreate() {
  editingId.value = undefined
  testResult.value = undefined
  testError.value = ''
  generationTraces.value = []
  relatedAgentRuns.value = []
  agentTrace.value = undefined
  Object.assign(form, {
    templateKey: '',
    templateName: '',
    templateType: 'requirement-split',
    templateContent: '',
    variables: 'projectName,techStack,requirement,context',
    enabled: true,
    isDefault: false,
    version: 1,
  })
}

function openEdit(item: PromptTemplate) {
  editingId.value = item.id
  form.templateKey = item.templateKey || ''
  form.templateName = item.templateName || ''
  form.templateType = item.templateType || 'requirement-split'
  form.templateContent = item.templateContent || ''
  form.variables = item.variables || ''
  form.enabled = Boolean(item.enabled)
  form.isDefault = Boolean(item.isDefault)
  form.version = item.version || 1
  testError.value = ''
}

async function submitPrompt(mode: 'template' | 'version' = 'template') {
  if (!form.templateKey.trim() || !form.templateName.trim() || !form.templateContent.trim()) {
    ElMessage.warning('请填写模板 Key、名称和内容。')
    return
  }
  saving.value = true
  try {
    const payload: Partial<PromptTemplate> = {
      templateKey: form.templateKey.trim(),
      templateName: form.templateName.trim(),
      templateType: form.templateType,
      templateContent: form.templateContent,
      variables: form.variables,
      enabled: form.enabled,
      isDefault: form.isDefault,
      version: form.version,
    }
    const saved = editingId.value
      ? await updatePrompt(editingId.value, payload)
      : await createPrompt(payload)
    ElMessage.success(mode === 'version' ? `已保存当前模板版本 v${saved.version}` : 'Prompt 模板已保存。')
    await Promise.all([refreshPrompts(saved.id), refreshGenerationRecords()])
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    saving.value = false
  }
}

async function runTemplateTest() {
  if (!editingId.value) {
    ElMessage.warning('请先选择或创建一个 Prompt 模板。')
    return
  }
  if (!isTestableTemplate.value) {
    ElMessage.warning('当前模板类型暂无真实试运行接口，只能编辑保存。')
    return
  }
  if (!testForm.input.trim()) {
    ElMessage.warning('请输入试运行问题。')
    return
  }
  testing.value = true
  testError.value = ''
  try {
    testResult.value = await generateAi(form.templateType, {
      projectId: testForm.projectId,
      input: testForm.input,
      templateId: editingId.value,
      variables: sampleValues.value,
      extraContext: testForm.extraContext,
      knowledgeQuery: testForm.input,
    })
    await loadTraceContext(testResult.value.recordId, testResult.value.agentRunId)
    ElMessage.success('试运行完成，已回填真实生成记录与可用 Trace 信息。')
  } catch (error) {
    testResult.value = undefined
    testError.value = errorMessage(error)
    ElMessage.error(testError.value)
  } finally {
    testing.value = false
  }
}

async function loadTraceContext(recordId: number, agentRunId?: number) {
  const [records, traces, runs] = await Promise.all([
    fetchGenerations().catch(() => generationRecords.value),
    fetchGenerationTraces({ generationRecordId: recordId }).catch(() => [] as GenerationTrace[]),
    fetchAgentRuns({ generationRecordId: recordId }).catch(() => [] as AgentRun[]),
  ])
  generationRecords.value = records
  generationTraces.value = traces
  relatedAgentRuns.value = runs
  const targetRunId = agentRunId || runs[0]?.id
  agentTrace.value = targetRunId ? await fetchAgentRunTrace(targetRunId).catch(() => undefined) : undefined
}

function insertVariable(name: string) {
  const suffix = form.templateContent && !form.templateContent.endsWith(' ') && !form.templateContent.endsWith('\n') ? ' ' : ''
  form.templateContent = `${form.templateContent}${suffix}{{${name}}}`
  const variables = new Set(requiredVariableNames.value)
  variables.add(name)
  form.variables = [...variables].join(',')
}

async function copyPreview() {
  try {
    await navigator.clipboard.writeText(previewCode.value)
    ElMessage.success('Prompt 预览已复制。')
  } catch {
    ElMessage.warning('复制失败，请手动选择内容。')
  }
}

function openGenerationTrace() {
  const recordId = testResult.value?.recordId || lastGenerationRecord.value?.id
  if (!recordId) {
    ElMessage.info('暂无可打开的 Generation Trace。')
    return
  }
  router.push({ path: '/agent-runs', query: { generationRecordId: String(recordId) } })
}

function recordsForTemplate(item: PromptTemplate) {
  return generationRecords.value.filter((record) => matchesTemplateRecord(record, item))
}

function matchesTemplateRecord(record: GenerationRecord, item: Partial<PromptTemplate> & { id?: number }) {
  return Boolean(
    (item.id && record.promptTemplateId === item.id)
      || (item.templateName && record.promptTemplateName === item.templateName)
      || (!record.promptTemplateId && record.generationType === item.templateType),
  )
}

function typeLabel(value: string) {
  return templateTypes.find((item) => item.value === value)?.label || value || '未记录'
}

function templateStatus(item: PromptTemplate) {
  if (item.isDefault) return { status: 'CONFIRMED', label: '默认模板' }
  if (item.enabled) return { status: 'SUCCESS', label: '已启用' }
  return { status: 'DRAFT', label: '已停用' }
}

function recordStatusLabel(status?: string) {
  const normalized = (status || '').toUpperCase()
  const labels: Record<string, string> = {
    DRAFT: '草稿',
    GENERATING: '生成中',
    READY_FOR_REVIEW: '待审核',
    SAVED: '已保存',
    CONFIRMED: '已确认',
    FAILED: '失败',
    SUCCESS: '成功',
  }
  return labels[normalized] || status || '未记录'
}

function reviewStatusLabel(status?: string) {
  const normalized = (status || '').toUpperCase()
  const labels: Record<string, string> = {
    APPROVED: '已通过',
    REJECTED: '已驳回',
    PENDING: '待审核',
    CONFIRMED: '已确认',
    SAVED: '已保存',
  }
  return labels[normalized] || status || '未记录'
}

function splitVariables(value: string) {
  return value.split(',').map((item) => item.trim()).filter(Boolean)
}

function sampleForVariable(name: string) {
  const project = selectedProject.value
  const map: Record<string, string> = {
    projectName: project?.projectName || 'DevFlow Copilot',
    techStack: project?.techStack || 'Vue 3 / Spring Boot',
    requirement: testForm.input || project?.currentRequirement || '生成可交付实现计划',
    context: testForm.extraContext || project?.readmeContent?.slice(0, 160) || '当前项目上下文未记录',
    codingRules: project?.codingRules || '所有 AI 输出必须保持 review-only，由开发者确认后使用。',
    directoryStructure: project?.directoryStructure?.slice(0, 160) || '目录结构未记录',
    errorLog: '示例错误日志未提供',
    input: testForm.input || '示例输入',
  }
  return map[name] || `示例_${name}`
}

function splitPromptSegments(content: string) {
  const normalized = content.trim()
  if (!normalized) {
    return {
      system: '未填写 System Prompt；当前后端仅存储单字段 templateContent。',
      user: '未填写 User Prompt；请在模板内容中维护真实 Prompt。',
    }
  }
  const systemMatch = normalized.match(/(?:^|\n)\s*(?:#+\s*)?(?:System Prompt|系统 Prompt|系统提示词)\s*[:：]?\s*\n([\s\S]*?)(?=\n\s*(?:#+\s*)?(?:User Prompt|用户 Prompt|用户提示词)\s*[:：]?|\s*$)/i)
  const userMatch = normalized.match(/(?:^|\n)\s*(?:#+\s*)?(?:User Prompt|用户 Prompt|用户提示词)\s*[:：]?\s*\n([\s\S]*)/i)
  return {
    system: systemMatch?.[1]?.trim() || '未单独存储 System Prompt；当前模板使用后端单字段 templateContent。',
    user: userMatch?.[1]?.trim() || normalized,
  }
}

function validationToneStatus(row: ValidationRow) {
  if (row.tone === 'danger') return 'FAILED'
  if (row.tone === 'warning') return 'PENDING'
  return 'SUCCESS'
}

function templateUsageCount(item: PromptTemplate) {
  return recordsForTemplate(item).length
}

function templateRecentRun(item: PromptTemplate) {
  const record = recordsForTemplate(item).sort((a, b) => timeValue(b.createdAt) - timeValue(a.createdAt))[0]
  return record?.createdAt
}

function timeValue(value?: string) {
  return value ? new Date(value).getTime() || 0 : 0
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '未记录'
}

function formatDuration(value?: number) {
  return typeof value === 'number' ? `${value}ms` : '未记录'
}

function formatNumber(value?: number) {
  return typeof value === 'number' ? value : '未记录'
}

function shortText(value?: string, max = 96) {
  if (!value) return '未记录'
  return value.length > max ? `${value.slice(0, max)}...` : value
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : 'Prompt 模板操作失败'
}

onMounted(loadPageData)
</script>

<template>
  <div class="templates-page prompt-studio" v-loading="loading">
    <header class="studio-hero">
      <div class="hero-copy">
        <span class="mono">PROMPT STUDIO</span>
        <h2>提示词工作室</h2>
        <p>Prompt 模板 -> 变量配置 -> 渲染预览 -> 测试运行 -> Trace / Human Review</p>
      </div>
      <div class="hero-pipeline" aria-label="Prompt 工作流">
        <span>模板</span>
        <i></i>
        <span>变量</span>
        <i></i>
        <span>渲染</span>
        <i></i>
        <span>测试</span>
        <i></i>
        <span>Trace</span>
      </div>
    </header>

    <div class="metric-strip">
      <MetricCard
        v-for="item in metricCards"
        :key="item.code"
        :label="item.label"
        :value="item.value"
        :code="item.code"
        :tone="item.tone"
      />
    </div>

    <main class="studio-grid">
      <SectionCard class="template-directory" title="Prompt 模板列表" subtitle="模板来自真实 /api/prompts 接口" eyebrow="Template Library">
        <template #actions>
          <el-button type="primary" :icon="Plus" @click="openCreate">新建模板</el-button>
        </template>

        <div class="directory-tools">
          <el-input v-model="searchKeyword" :prefix-icon="Search" placeholder="搜索名称、Key、变量" clearable />
          <div class="filter-row">
            <el-select v-model="filterType" placeholder="类型筛选">
              <el-option v-for="item in templateTypes" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="filterStatus" placeholder="状态筛选">
              <el-option v-for="item in statusFilters" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>
        </div>

        <div class="template-list">
          <div v-if="!filteredPrompts.length" class="empty-state">暂无匹配的 Prompt 模板。</div>
          <button
            v-for="item in filteredPrompts"
            :key="item.id"
            class="template-row"
            :class="{ active: editingId === item.id }"
            type="button"
            @click="openEdit(item)"
          >
            <span class="row-spine" :data-enabled="item.enabled" aria-hidden="true"></span>
            <span class="template-main">
              <strong>{{ item.templateName }}</strong>
              <small class="mono">{{ item.templateKey }} / {{ typeLabel(item.templateType) }}</small>
              <em>{{ item.variables || '未记录变量' }}</em>
            </span>
            <span class="template-stats">
              <StatusBadge :status="templateStatus(item).status" :label="templateStatus(item).label" />
              <b class="mono">v{{ item.version }}</b>
              <small>{{ templateUsageCount(item) ? `${templateUsageCount(item)} 次运行` : '使用次数未记录' }}</small>
              <small class="mono">{{ formatTime(templateRecentRun(item) || item.updatedAt) }}</small>
            </span>
          </button>
        </div>
      </SectionCard>

      <section class="editor-column">
        <SectionCard class="editor-card" :title="editingId ? 'Prompt 编辑器' : '新建 Prompt 模板'" subtitle="保存与试运行沿用现有 Prompt Templates / AI Generate 接口" eyebrow="Editor">
          <template #actions>
            <div class="editor-actions">
              <el-dropdown @command="insertVariable">
                <el-button>插入变量</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="projectName">projectName</el-dropdown-item>
                    <el-dropdown-item command="techStack">techStack</el-dropdown-item>
                    <el-dropdown-item command="requirement">requirement</el-dropdown-item>
                    <el-dropdown-item command="context">context</el-dropdown-item>
                    <el-dropdown-item command="codingRules">codingRules</el-dropdown-item>
                    <el-dropdown-item command="errorLog">errorLog</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button :icon="RefreshRight" @click="loadPageData">刷新</el-button>
            </div>
          </template>

          <div class="template-meta-grid">
            <label>
              <span>模板名称</span>
              <el-input v-model="form.templateName" placeholder="需求拆解模板" />
            </label>
            <label>
              <span>模板 Key</span>
              <el-input v-model="form.templateKey" placeholder="requirement-split" />
            </label>
            <label>
              <span>类型</span>
              <el-select v-model="form.templateType">
                <el-option v-for="item in templateTypes.slice(1)" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </label>
            <label>
              <span>当前版本</span>
              <div class="readonly-field mono">v{{ form.version }}</div>
            </label>
            <label>
              <span>状态</span>
              <div class="switch-stack">
                <el-switch v-model="form.enabled" active-text="已启用" inactive-text="已停用" />
                <el-switch v-model="form.isDefault" active-text="默认" inactive-text="普通" />
              </div>
            </label>
            <label>
              <span>更新时间</span>
              <div class="readonly-field mono">{{ formatTime(selectedTemplate?.updatedAt) }}</div>
            </label>
          </div>

          <div class="prompt-edit-grid">
            <div class="prompt-side">
              <div class="mini-code">
                <div>
                  <strong>System Prompt</strong>
                  <small>后端未拆分字段时显示单字段说明</small>
                </div>
                <CodeBlock :code="promptSegments.system" language="text" title="System Prompt" />
              </div>
              <div class="mini-code">
                <div>
                  <strong>User Prompt</strong>
                  <small>由 templateContent 推导预览</small>
                </div>
                <CodeBlock :code="promptSegments.user" language="text" title="User Prompt" />
              </div>
            </div>

            <label class="template-content-field">
              <span>模板内容</span>
              <el-input
                v-model="form.templateContent"
                class="template-textarea mono"
                type="textarea"
                :autosize="{ minRows: 16, maxRows: 24 }"
                placeholder="请基于 {{projectName}} 和 {{techStack}} 处理需求：{{requirement}}"
              />
            </label>
          </div>

          <div class="variables-card">
            <div class="variables-head">
              <div>
                <strong>变量配置</strong>
                <span>variables 字段为后端必填变量来源，占位符从模板内容派生。</span>
              </div>
              <el-input v-model="form.variables" class="mono" placeholder="projectName,techStack,requirement,context" />
            </div>
            <div class="variable-table">
              <div class="table-head">
                <span>变量名</span>
                <span>示例值</span>
                <span>必填</span>
                <span>说明</span>
              </div>
              <div v-if="!variablesRows.length" class="empty-state">暂未识别变量占位符。</div>
              <div v-for="row in variablesRows" :key="row.name" class="variable-row">
                <strong class="mono">{{ row.name }}</strong>
                <el-input v-model="variableSamples[row.name]" :placeholder="row.sample" />
                <StatusBadge :status="row.required ? 'SUCCESS' : 'PENDING'" :label="row.required ? '必填' : '可选'" />
                <small>{{ row.description }}</small>
              </div>
            </div>
          </div>

          <div class="preview-card">
            <div class="preview-toolbar">
              <div class="segmented">
                <button
                  v-for="mode in previewModes"
                  :key="mode.value"
                  type="button"
                  :class="{ active: activePreviewMode === mode.value }"
                  @click="activePreviewMode = mode.value"
                >
                  {{ mode.label }}
                </button>
              </div>
              <el-button :icon="CopyDocument" @click="copyPreview">复制</el-button>
            </div>
            <CodeBlock :code="previewCode" :language="activePreviewMode === 'json' ? 'json' : 'text'" title="Rendered Prompt Preview" />
          </div>

          <div class="editor-footer">
            <label class="change-note">
              <span>变更说明</span>
              <el-input v-model="changeNote" placeholder="当前版本变更说明，仅前端展示，不写入后端版本表。" />
            </label>
            <div class="footer-actions">
              <el-button :icon="Edit" :loading="saving" @click="submitPrompt('template')">保存模板</el-button>
              <el-button type="primary" :icon="Edit" :loading="saving" @click="submitPrompt('version')">保存版本</el-button>
              <el-button :icon="MagicStick" :loading="testing" :disabled="!isTestableTemplate" @click="runTemplateTest">运行测试</el-button>
            </div>
          </div>
        </SectionCard>
      </section>

      <aside class="run-inspector">
        <SectionCard title="测试运行预览" subtitle="真实运行结果优先展示；缺失字段保持空状态" eyebrow="Run Preview">
          <div class="run-status-panel">
            <div class="status-line">
              <StatusBadge
                :status="testResult?.status || (testError ? 'FAILED' : 'PENDING')"
                :label="testResult ? recordStatusLabel(testResult.status) : testError ? '运行失败' : '未运行'"
              />
              <ProviderBadge :provider="testResult?.providerName || lastGenerationRecord?.providerName || 'local-rule fallback'" :model="testResult?.modelName || lastGenerationRecord?.modelName || '未记录'" />
            </div>
            <div class="runtime-grid">
              <div><span>耗时</span><strong class="mono">{{ formatDuration(testResult?.costTimeMs || lastTrace?.latencyMs || lastAgentRun?.latencyMs) }}</strong></div>
              <div><span>Trace ID</span><strong class="mono">{{ lastTrace ? `TRACE-${lastTrace.id}` : '未关联 Trace' }}</strong></div>
              <div><span>Model</span><strong>{{ testResult?.modelName || lastTrace?.modelName || '未记录' }}</strong></div>
              <div><span>降级策略</span><strong>后端 Provider Router</strong></div>
              <div><span>Prompt Token</span><strong class="mono">{{ formatNumber(testResult?.promptTokens || lastGenerationRecord?.promptTokens) }}</strong></div>
              <div><span>Total Token</span><strong class="mono">{{ formatNumber(testResult?.totalTokens || lastGenerationRecord?.totalTokens) }}</strong></div>
            </div>
          </div>

          <div class="test-form">
            <label>
              <span>测试项目</span>
              <el-select v-model="testForm.projectId" placeholder="选择项目">
                <el-option v-for="project in projects" :key="project.id" :label="project.projectName" :value="project.id" />
              </el-select>
            </label>
            <label>
              <span>检索 / 生成问题</span>
              <el-input v-model="testForm.input" type="textarea" :autosize="{ minRows: 4, maxRows: 7 }" />
            </label>
            <label>
              <span>额外上下文</span>
              <el-input v-model="testForm.extraContext" type="textarea" :autosize="{ minRows: 3, maxRows: 5 }" />
            </label>
            <p v-if="!isTestableTemplate" class="inline-warning">当前类型暂无真实生成测试端点，可编辑保存但不执行假测试。</p>
          </div>

          <div v-if="testResult" class="test-result">
            <div class="result-head">
              <strong>最近运行结果</strong>
              <button type="button" @click="openGenerationTrace">
                <View />
                查看 Generation Trace
              </button>
            </div>
            <p>{{ shortText(testResult.outputContent, 180) }}</p>
            <CodeBlock :code="testResult.outputContent || '未返回原始响应。'" language="markdown" title="Raw Response" />
          </div>
          <div v-else-if="testError" class="test-result error-state">
            <strong>运行失败</strong>
            <p>{{ testError }}</p>
          </div>
          <div v-else class="empty-state">暂无真实测试结果。运行测试后会显示 Provider、Model、Token 与 Trace 信息。</div>
        </SectionCard>

        <SectionCard title="Human Review" subtitle="来自 Agent Run Trace；没有记录时显示空状态" eyebrow="Review">
          <div v-if="latestReview" class="review-card">
            <StatusBadge :status="latestReview.reviewStatus" :label="reviewStatusLabel(latestReview.reviewStatus)" />
            <p>{{ latestReview.comment || '未记录审核意见。' }}</p>
            <small class="mono">{{ latestReview.reviewer || '未记录审核人' }} / {{ formatTime(latestReview.updatedAt || latestReview.createdAt) }}</small>
          </div>
          <div v-else class="empty-state">暂无 Human Review 记录。</div>
        </SectionCard>

        <SectionCard title="Provider 配置提示" subtitle="前端不展示 API Key" eyebrow="Provider">
          <div class="provider-notes">
            <div>
              <strong>OpenAI-compatible</strong>
              <span>真实调用由后端环境变量和 Provider Router 决定。</span>
            </div>
            <div>
              <strong>local-rule fallback</strong>
              <span>缺少外部 Provider 配置时，后端可使用本地规则回退。</span>
            </div>
            <div>
              <strong>API Key</strong>
              <span>不在前端读取、展示或提交。</span>
            </div>
          </div>
        </SectionCard>
      </aside>
    </main>

    <section class="observability">
      <div class="bottom-tabs" role="tablist" aria-label="Prompt Studio 观测标签">
        <button
          v-for="tab in bottomTabs"
          :key="tab.value"
          type="button"
          :class="{ active: activeBottomTab === tab.value }"
          @click="activeBottomTab = tab.value"
        >
          <span class="mono">{{ tab.code }}</span>
          {{ tab.label }}
        </button>
      </div>

      <SectionCard v-if="activeBottomTab === 'versions'" title="版本历史" subtitle="当前后端没有独立版本历史表，因此只展示当前版本记录" eyebrow="Version">
        <div class="audit-table version-table">
          <div class="table-head">
            <span>版本</span>
            <span>状态</span>
            <span>变更说明</span>
            <span>时间</span>
          </div>
          <div v-if="!versionRows.length" class="empty-state">新建模板尚未形成真实版本记录。</div>
          <div v-for="row in versionRows" :key="row.version" class="audit-row">
            <strong class="mono">v{{ row.version }}</strong>
            <StatusBadge :status="row.status" :label="row.label" />
            <span>{{ row.note }}</span>
            <small class="mono">{{ formatTime(row.time) }}</small>
          </div>
        </div>
      </SectionCard>

      <SectionCard v-if="activeBottomTab === 'tests'" title="测试结果" subtitle="来自本页真实试运行或最近生成记录" eyebrow="Test Results">
        <div class="audit-table test-table">
          <div class="table-head">
            <span>记录</span>
            <span>状态</span>
            <span>Provider</span>
            <span>Token</span>
            <span>耗时</span>
          </div>
          <div v-if="!testResult && !currentGenerationRecords.length" class="empty-state">暂无测试结果。</div>
          <div v-if="testResult" class="audit-row">
            <strong class="mono">GEN-{{ testResult.recordId }}</strong>
            <StatusBadge :status="testResult.status" :label="recordStatusLabel(testResult.status)" />
            <ProviderBadge :provider="testResult.providerName" :model="testResult.modelName" />
            <span class="mono">{{ formatNumber(testResult.totalTokens) }}</span>
            <small class="mono">{{ formatDuration(testResult.costTimeMs) }}</small>
          </div>
          <div v-for="record in currentGenerationRecords.slice(0, 4)" :key="record.id" class="audit-row">
            <strong class="mono">GEN-{{ record.id }}</strong>
            <StatusBadge :status="record.status" :label="recordStatusLabel(record.status)" />
            <ProviderBadge :provider="record.providerName || '未记录'" :model="record.modelName" />
            <span class="mono">{{ formatNumber(record.totalTokens) }}</span>
            <small class="mono">{{ formatDuration(record.costTimeMs) }}</small>
          </div>
        </div>
      </SectionCard>

      <SectionCard v-if="activeBottomTab === 'validation'" title="Prompt 校验" subtitle="仅前端静态校验，不包装为 LLM-as-Judge 或自动评测平台" eyebrow="Validation">
        <div class="validation-grid">
          <article v-for="row in validationRows" :key="row.name" class="validation-row" :data-tone="row.tone">
            <StatusBadge :status="validationToneStatus(row)" :label="row.label" />
            <strong>{{ row.name }}</strong>
            <p>{{ row.detail }}</p>
          </article>
        </div>
      </SectionCard>

      <SectionCard v-if="activeBottomTab === 'runs'" title="最近运行" subtitle="从真实 /api/generations 记录中按当前模板关联" eyebrow="Recent Runs">
        <div class="audit-table run-table">
          <div class="table-head">
            <span>查询 / 输入</span>
            <span>结果</span>
            <span>Trace</span>
            <span>状态</span>
            <span>时间</span>
          </div>
          <div v-if="!currentGenerationRecords.length" class="empty-state">暂无与当前模板关联的生成记录。</div>
          <div v-for="record in currentGenerationRecords.slice(0, 6)" :key="record.id" class="audit-row">
            <strong>{{ shortText(record.inputSummary || record.inputContent, 64) }}</strong>
            <span>{{ shortText(record.outputContent, 72) }}</span>
            <small class="mono">{{ record.id === lastTrace?.generationRecordId ? `TRACE-${lastTrace.id}` : '未关联 Trace' }}</small>
            <StatusBadge :status="record.status" :label="recordStatusLabel(record.status)" />
            <small class="mono">{{ formatTime(record.createdAt) }}</small>
          </div>
        </div>
      </SectionCard>

      <SectionCard v-if="activeBottomTab === 'changes'" title="变更记录" subtitle="来自模板 createdAt / updatedAt 字段；无独立审计日志时不造假" eyebrow="Change Log">
        <div class="audit-table change-table">
          <div class="table-head">
            <span>动作</span>
            <span>说明</span>
            <span>时间</span>
          </div>
          <div v-if="!changeRows.length" class="empty-state">暂无真实变更记录。</div>
          <div v-for="row in changeRows" :key="`${row.action}-${row.time}`" class="audit-row">
            <strong>{{ row.action }}</strong>
            <span>{{ row.detail }}</span>
            <small class="mono">{{ formatTime(row.time) }}</small>
          </div>
        </div>
      </SectionCard>
    </section>
  </div>
</template>

<style scoped>
.prompt-studio {
  display: grid;
  gap: var(--space-3);
  min-width: 0;
}

.prompt-studio > *,
.studio-grid > *,
.template-row > *,
.audit-row > *,
.variable-row > *,
.runtime-grid > * {
  min-width: 0;
}

.studio-hero {
  min-height: 112px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(460px, 0.9fr);
  align-items: end;
  gap: var(--space-4);
  padding: var(--space-4);
  border: var(--border-default);
  border-radius: var(--radius-card);
  background:
    linear-gradient(135deg, rgba(124, 92, 255, 0.2), transparent 38%),
    linear-gradient(90deg, rgba(34, 211, 238, 0.08), transparent 54%),
    var(--color-card);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.hero-copy span,
.hero-copy h2,
.hero-copy p {
  display: block;
  margin: 0;
  min-width: 0;
}

.hero-copy span {
  color: var(--color-accent-cyan);
  font-size: 10px;
  letter-spacing: 0.08em;
}

.hero-copy h2 {
  margin-top: 8px;
  color: var(--color-text-primary);
  font-size: 28px;
  font-weight: 720;
  line-height: 1.05;
}

.hero-copy p {
  margin-top: 10px;
  color: var(--color-text-secondary);
  font-size: 12px;
}

.hero-pipeline {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0;
  border: var(--border-subtle);
  border-radius: var(--radius-md);
  background: rgba(8, 11, 18, 0.72);
  overflow: hidden;
}

.hero-pipeline span {
  min-height: 48px;
  display: grid;
  place-items: center;
  padding: 0 var(--space-2);
  color: var(--color-text-secondary);
  font-size: 11px;
  text-align: center;
  white-space: nowrap;
}

.hero-pipeline i {
  display: none;
}

.hero-pipeline span + i + span,
.hero-pipeline span + span {
  border-left: var(--border-subtle);
}

.metric-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-3);
}

.studio-grid {
  display: grid;
  grid-template-columns: minmax(280px, 330px) minmax(560px, 1fr) minmax(300px, 360px);
  gap: var(--space-3);
  align-items: start;
}

.directory-tools,
.template-list,
.editor-column,
.run-inspector,
.test-form,
.provider-notes {
  display: grid;
  gap: var(--space-2);
}

.filter-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-2);
}

.template-list {
  margin-top: var(--space-3);
}

.template-row {
  width: 100%;
  min-height: 96px;
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) minmax(108px, 126px);
  gap: var(--space-2);
  align-items: center;
  padding: var(--space-3);
  border: 1px solid transparent;
  border-bottom: var(--border-subtle);
  border-radius: var(--radius-md);
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.template-row:hover,
.template-row.active {
  border-color: var(--color-border);
  background: var(--color-active-row);
}

.template-row.active {
  box-shadow: inset 2px 0 var(--color-accent);
}

.row-spine {
  width: 7px;
  height: 44px;
  border-radius: var(--radius-sm);
  background: var(--color-border);
}

.row-spine[data-enabled="true"] {
  background: linear-gradient(180deg, var(--color-accent), var(--color-accent-cyan));
}

.template-main strong,
.template-main small,
.template-main em,
.template-stats b,
.template-stats small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.template-main strong {
  color: var(--color-text-primary);
  font-size: 12px;
  font-weight: 620;
}

.template-main small {
  margin-top: 5px;
  color: var(--color-text-secondary);
  font-size: 9px;
}

.template-main em {
  margin-top: 7px;
  color: var(--color-text-disabled);
  font-size: 10px;
  font-style: normal;
}

.template-stats {
  display: grid;
  justify-items: end;
  gap: 5px;
  text-align: right;
}

.template-stats b {
  color: var(--color-accent-cyan);
  font-size: 11px;
}

.template-stats small {
  color: var(--color-text-disabled);
  font-size: 9px;
}

.editor-actions,
.footer-actions,
.preview-toolbar,
.switch-stack,
.status-line,
.result-head {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.template-meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-2);
}

.template-meta-grid label,
.test-form label,
.change-note {
  display: grid;
  gap: 6px;
}

.template-meta-grid label > span,
.test-form label > span,
.template-content-field > span,
.change-note > span {
  color: var(--color-text-secondary);
  font-size: 11px;
}

.readonly-field {
  min-height: 32px;
  display: flex;
  align-items: center;
  padding: 0 var(--space-3);
  border: var(--border-default);
  border-radius: var(--radius-md);
  background: var(--color-bg);
  color: var(--color-text-secondary);
  font-size: 11px;
}

.prompt-edit-grid {
  display: grid;
  grid-template-columns: minmax(220px, 0.58fr) minmax(0, 1fr);
  gap: var(--space-3);
  margin-top: var(--space-3);
}

.prompt-side,
.mini-code {
  display: grid;
  gap: var(--space-2);
}

.mini-code > div strong,
.mini-code > div small,
.variables-head strong,
.variables-head span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mini-code > div strong,
.variables-head strong {
  color: var(--color-text-primary);
  font-size: 12px;
}

.mini-code > div small,
.variables-head span {
  margin-top: 3px;
  color: var(--color-text-disabled);
  font-size: 10px;
}

.template-content-field {
  display: grid;
  gap: 6px;
}

.template-textarea :deep(textarea) {
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: var(--leading-code);
}

.variables-card,
.preview-card,
.run-status-panel {
  margin-top: var(--space-3);
  padding: var(--space-3);
  border: var(--border-default);
  border-radius: var(--radius-card);
  background: var(--color-bg);
}

.variables-head {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(260px, 1fr);
  gap: var(--space-3);
  align-items: start;
}

.variable-table,
.audit-table {
  display: grid;
  margin-top: var(--space-3);
  border: var(--border-subtle);
  border-radius: var(--radius-md);
  background: var(--color-border-subtle);
  overflow: hidden;
}

.table-head,
.variable-row,
.audit-row {
  display: grid;
  gap: 1px;
  background: var(--color-border-subtle);
}

.variable-table .table-head,
.variable-row {
  grid-template-columns: minmax(100px, 0.7fr) minmax(160px, 1fr) 72px minmax(160px, 1.1fr);
}

.table-head span,
.variable-row > *,
.audit-row > * {
  min-width: 0;
  padding: 9px 10px;
  background: var(--color-card-soft);
}

.table-head span {
  color: var(--color-text-disabled);
  font-size: 10px;
  white-space: nowrap;
}

.variable-row {
  align-items: center;
}

.variable-row strong,
.variable-row small,
.audit-row strong,
.audit-row span,
.audit-row small {
  overflow: hidden;
  text-overflow: ellipsis;
}

.variable-row strong,
.audit-row strong {
  color: var(--color-text-primary);
  font-size: 11px;
  white-space: nowrap;
}

.variable-row small,
.audit-row span,
.audit-row small {
  color: var(--color-text-secondary);
  font-size: 11px;
  line-height: 1.45;
}

.preview-toolbar {
  justify-content: space-between;
  margin-bottom: var(--space-2);
}

.segmented {
  display: inline-flex;
  gap: 2px;
  padding: 2px;
  border: var(--border-subtle);
  border-radius: var(--radius-md);
  background: var(--color-card-soft);
}

.segmented button,
.bottom-tabs button,
.result-head button {
  border: 0;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
}

.segmented button {
  min-height: 26px;
  padding: 0 10px;
  border-radius: var(--radius-sm);
  font-size: 11px;
}

.segmented button.active {
  background: var(--color-accent-muted);
  color: var(--color-text-primary);
}

.editor-footer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--space-3);
  align-items: end;
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: var(--border-subtle);
}

.run-inspector {
  align-self: stretch;
}

.status-line {
  justify-content: space-between;
  margin-bottom: var(--space-3);
}

.runtime-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1px;
  border: var(--border-subtle);
  border-radius: var(--radius-md);
  background: var(--color-border-subtle);
  overflow: hidden;
}

.runtime-grid div {
  padding: 9px 10px;
  background: var(--color-card-soft);
}

.runtime-grid span,
.runtime-grid strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.runtime-grid span {
  color: var(--color-text-disabled);
  font-size: 10px;
}

.runtime-grid strong {
  margin-top: 5px;
  color: var(--color-text-primary);
  font-size: 11px;
  font-weight: 600;
}

.test-form {
  margin-top: var(--space-3);
}

.inline-warning {
  margin: 0;
  padding: var(--space-2);
  border: 1px solid var(--color-warning-border);
  border-radius: var(--radius-md);
  background: var(--color-warning-muted);
  color: var(--color-warning);
  font-size: 11px;
}

.test-result {
  display: grid;
  gap: var(--space-2);
  margin-top: var(--space-3);
  padding: var(--space-3);
  border: var(--border-default);
  border-radius: var(--radius-card);
  background: var(--color-bg);
}

.test-result p,
.review-card p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.result-head {
  justify-content: space-between;
}

.result-head button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 28px;
  padding: 0 8px;
  border: var(--border-subtle);
  border-radius: var(--radius-md);
  font-size: 11px;
}

.result-head svg {
  width: 14px;
  height: 14px;
}

.error-state {
  border-color: var(--color-error-border);
  background: var(--color-error-muted);
}

.review-card {
  display: grid;
  gap: var(--space-2);
}

.review-card small {
  color: var(--color-text-disabled);
  font-size: 10px;
}

.provider-notes > div {
  padding: var(--space-3);
  border: var(--border-subtle);
  border-radius: var(--radius-md);
  background: var(--color-bg);
}

.provider-notes strong,
.provider-notes span {
  display: block;
}

.provider-notes strong {
  color: var(--color-text-primary);
  font-size: 12px;
}

.provider-notes span {
  margin-top: 5px;
  color: var(--color-text-secondary);
  font-size: 11px;
  line-height: 1.5;
}

.observability {
  display: grid;
  gap: var(--space-3);
}

.bottom-tabs {
  display: flex;
  gap: var(--space-2);
  padding: var(--space-2);
  border: var(--border-default);
  border-radius: var(--radius-card);
  background: var(--color-card);
  overflow-x: auto;
}

.bottom-tabs button {
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 var(--space-3);
  border: var(--border-subtle);
  border-radius: var(--radius-md);
  font-size: 12px;
  white-space: nowrap;
}

.bottom-tabs button.active {
  border-color: var(--color-accent-border);
  background: var(--color-accent-muted);
  color: var(--color-text-primary);
}

.bottom-tabs button span {
  color: var(--color-accent-cyan);
  font-size: 9px;
}

.version-table .table-head,
.version-table .audit-row {
  grid-template-columns: 90px 120px minmax(0, 1fr) 150px;
}

.test-table .table-head,
.test-table .audit-row {
  grid-template-columns: 110px 120px minmax(190px, 0.9fr) 100px 100px;
}

.run-table .table-head,
.run-table .audit-row {
  grid-template-columns: minmax(160px, 1fr) minmax(180px, 1fr) 120px 120px 140px;
}

.change-table .table-head,
.change-table .audit-row {
  grid-template-columns: 150px minmax(0, 1fr) 160px;
}

.validation-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--space-2);
}

.validation-row {
  min-height: 116px;
  display: grid;
  align-content: start;
  gap: var(--space-2);
  padding: var(--space-3);
  border: var(--border-subtle);
  border-radius: var(--radius-card);
  background: var(--color-bg);
}

.validation-row[data-tone="warning"] {
  border-color: var(--color-warning-border);
  background: var(--color-warning-muted);
}

.validation-row[data-tone="danger"] {
  border-color: var(--color-error-border);
  background: var(--color-error-muted);
}

.validation-row strong {
  color: var(--color-text-primary);
  font-size: 12px;
}

.validation-row p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 11px;
  line-height: 1.55;
}

.prompt-studio :deep(.section-card__body) {
  min-width: 0;
}

.prompt-studio :deep(.code-block pre) {
  max-height: 320px;
}

.prompt-studio :deep(.el-button:not(.el-button--primary)) {
  --el-button-bg-color: var(--color-bg);
  --el-button-border-color: var(--color-border);
  --el-button-text-color: var(--color-text-secondary);
  --el-button-hover-bg-color: var(--color-active-row);
  --el-button-hover-border-color: var(--color-accent-border);
  --el-button-hover-text-color: var(--color-text-primary);
}

.prompt-studio :deep(.el-switch__label) {
  color: var(--color-text-secondary);
}

.prompt-studio :deep(.el-input__wrapper),
.prompt-studio :deep(.el-textarea__inner),
.prompt-studio :deep(.el-select__wrapper) {
  background: var(--color-bg) !important;
}

@media (max-width: 1360px) {
  .studio-grid,
  .studio-hero {
    grid-template-columns: 1fr;
  }

  .validation-grid,
  .metric-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .template-meta-grid,
  .prompt-edit-grid,
  .variables-head,
  .editor-footer {
    grid-template-columns: 1fr;
  }

  .variable-table .table-head,
  .variable-row,
  .version-table .table-head,
  .version-table .audit-row,
  .test-table .table-head,
  .test-table .audit-row,
  .run-table .table-head,
  .run-table .audit-row,
  .change-table .table-head,
  .change-table .audit-row {
    grid-template-columns: 1fr;
  }

  .validation-grid,
  .metric-strip {
    grid-template-columns: 1fr;
  }
}
</style>
