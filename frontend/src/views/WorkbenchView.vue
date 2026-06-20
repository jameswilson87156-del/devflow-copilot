<script setup lang="ts">
import { computed, onMounted, reactive, shallowRef, watch } from 'vue'
import { ArrowLeft, ArrowRight, Check, CircleCheck, DocumentCopy, MagicStick, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  confirmGeneration,
  fetchGenerations,
  fetchProjects,
  fetchPrompts,
  generateAi,
  saveGeneration,
} from '@/api/devflow'
import MarkdownPanel from '@/components/MarkdownPanel.vue'
import StatusTag from '@/components/StatusTag.vue'
import type { AiGenerateResponse, GenerationRecord, ProjectContext, PromptTemplate } from '@/types/domain'

const projects = shallowRef<ProjectContext[]>([])
const prompts = shallowRef<PromptTemplate[]>([])
const recentRecords = shallowRef<GenerationRecord[]>([])
const selectedProjectId = shallowRef<number>()
const loading = shallowRef(false)
const result = shallowRef<AiGenerateResponse>()
const aiPanelCollapsed = shallowRef(new URLSearchParams(window.location.search).get('ai') === 'collapsed')

const form = reactive<{ type: string; input: string; extraContext: string; templateId?: number }>({
  type: 'requirement-split',
  input: '为 DevFlow Copilot 增加日志分析历史筛选，并保证所有输出需要人工确认。',
  extraContext: '',
  templateId: undefined,
})

const generationTypes = [
  { label: '需求拆解', value: 'requirement-split' },
  { label: '代码变更计划', value: 'code-plan' },
  { label: 'README 内容', value: 'readme-generate' },
  { label: 'Commit Message', value: 'commit-message' },
  { label: '修复 Prompt', value: 'fix-prompt' },
]

const selectedProject = computed(() => projects.value.find((project) => project.id === selectedProjectId.value))
const availableTemplates = computed(() => prompts.value.filter((item) => item.enabled && item.templateType === form.type))
const selectedTemplate = computed(() => prompts.value.find((item) => item.id === form.templateId))
const displayStatus = computed(() => (loading.value ? 'GENERATING' : result.value?.status || 'DRAFT'))
const canSave = computed(() => result.value?.status === 'READY_FOR_REVIEW')
const canConfirm = computed(() => result.value?.status === 'SAVED')
const currentTrace = computed(() => [
  { label: 'Context', value: selectedProject.value?.projectName || '未选择项目' },
  { label: 'Prompt', value: selectedTemplate.value ? `${selectedTemplate.value.templateName} v${selectedTemplate.value.version}` : '默认模板' },
  { label: 'Provider', value: result.value?.providerName || 'local-rule' },
  { label: 'Review', value: statusText(displayStatus.value) },
])

const taskSteps = computed(() => {
  const status = displayStatus.value
  const order = ['DRAFT', 'GENERATING', 'READY_FOR_REVIEW', 'SAVED', 'CONFIRMED']
  const index = status === 'FAILED' ? 1 : order.indexOf(status)
  return order.map((value, stepIndex) => ({
    value,
    label: { DRAFT: '草稿', GENERATING: '生成中', READY_FOR_REVIEW: '待人工确认', SAVED: '已保存', CONFIRMED: '已确认' }[value],
    done: index > stepIndex,
    active: status === value,
  }))
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
  if (!form.input.trim()) {
    ElMessage.warning('请先输入需求或问题')
    return
  }
  loading.value = true
  result.value = undefined
  try {
    result.value = await generateAi(form.type, {
      projectId: selectedProjectId.value,
      input: form.input,
      extraContext: form.extraContext,
      templateId: form.templateId,
    })
    ElMessage.success('生成完成，等待人工确认')
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
    recentRecords.value = (await fetchGenerations()).slice(0, 5)
    ElMessage.success('已完成人工确认')
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
}

function statusText(status: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    GENERATING: '正在生成',
    READY_FOR_REVIEW: '待人工确认',
    SAVED: '已保存',
    CONFIRMED: '已确认',
    FAILED: '生成失败',
  }
  return map[status] || status
}

onMounted(loadPageData)
</script>

<template>
  <div class="workbench" :class="{ 'ai-collapsed': aiPanelCollapsed }">
    <aside class="context-column panel">
      <div class="panel-header">
        <div>
          <h3 class="panel-title">项目上下文</h3>
          <p class="panel-subtitle">每次生成前载入真实项目约束</p>
        </div>
      </div>
      <div class="panel-body context-body">
        <el-select v-model="selectedProjectId" placeholder="选择项目" class="full">
          <el-option v-for="project in projects" :key="project.id" :label="project.projectName" :value="project.id" />
        </el-select>

        <div v-if="selectedProject" class="context-head">
          <span class="file-dot" aria-hidden="true"></span>
          <div>
            <strong>{{ selectedProject.projectName }}</strong>
            <span>Spring Boot 3 · Vue 3 · 人工确认</span>
          </div>
        </div>

        <dl v-if="selectedProject" class="context-details">
          <div><dt>技术栈</dt><dd>{{ selectedProject.techStack }}</dd></div>
          <div><dt>当前需求</dt><dd>{{ selectedProject.currentRequirement }}</dd></div>
          <div><dt>开发约束</dt><dd>{{ selectedProject.codingRules }}</dd></div>
        </dl>

        <div class="recent-block">
          <h4>最近 Artifact</h4>
          <article v-for="record in recentRecords" :key="record.id">
            <strong>{{ record.inputSummary }}</strong>
            <span class="mono">{{ record.providerName || 'local-rule' }} · {{ statusText(record.status) }}</span>
          </article>
        </div>
      </div>
    </aside>

    <section class="composer-column panel">
      <div class="workbench-toolbar">
        <div>
          <h3 class="panel-title">Workflow 编排器</h3>
          <p class="panel-subtitle">模板渲染 → Provider 生成 → 人工确认</p>
        </div>
        <div class="toolbar-actions">
          <StatusTag :status="displayStatus" />
          <el-button :icon="RefreshRight" @click="form.input = ''">清空输入</el-button>
          <el-button type="primary" :icon="MagicStick" :loading="loading" @click="runGenerate">运行工作流</el-button>
        </div>
      </div>

      <div class="panel-body composer-body">
        <div class="editor-shell">
          <div class="editor-bar"><span class="mono">prompt.devflow</span><em>{{ form.input.length }} 字符</em></div>
          <el-input
            v-model="form.input"
            class="prompt-editor mono"
            type="textarea"
            :autosize="{ minRows: 10, maxRows: 16 }"
            placeholder="描述需求、Bug、README 片段或提交意图。"
            @keydown.ctrl.enter.prevent="runGenerate"
            @keydown.meta.enter.prevent="runGenerate"
          />
        </div>

        <div class="config-grid">
          <label><span>任务类型</span><el-select v-model="form.type" class="full"><el-option v-for="item in generationTypes" :key="item.value" :label="item.label" :value="item.value" /></el-select></label>
          <label><span>Prompt 模板</span><el-select v-model="form.templateId" class="full" placeholder="使用默认模板"><el-option v-for="item in availableTemplates" :key="item.id" :label="`${item.templateName} · v${item.version}${item.isDefault ? ' · 默认' : ''}`" :value="item.id" /></el-select></label>
          <label><span>补充上下文</span><el-input v-model="form.extraContext" type="textarea" :autosize="{ minRows: 3, maxRows: 6 }" placeholder="相关文件、接口边界、验收标准、限制条件。" /></label>
        </div>

        <p v-if="selectedTemplate" class="template-hint">
          当前模板：<strong>{{ selectedTemplate.templateName }} v{{ selectedTemplate.version }}</strong>
          · 必填变量 {{ selectedTemplate.variables || '无' }}
        </p>

        <div class="task-flow" :class="{ failed: displayStatus === 'FAILED' }">
          <div v-for="step in taskSteps" :key="step.value" class="task-step" :class="{ active: step.active, done: step.done }">
            <span class="step-dot"><el-icon v-if="step.done"><Check /></el-icon></span><span>{{ step.label }}</span>
          </div>
          <span v-if="displayStatus === 'FAILED'" class="failed-label">生成失败</span>
        </div>
      </div>
    </section>

    <aside class="result-column">
      <button class="collapse-rail" :aria-label="aiPanelCollapsed ? '展开 AI 输出面板' : '折叠 AI 输出面板'" @click="aiPanelCollapsed = !aiPanelCollapsed">
        <el-icon><ArrowLeft v-if="aiPanelCollapsed" /><ArrowRight v-else /></el-icon>
      </button>
      <div v-if="!aiPanelCollapsed" class="result-content">
        <div class="artifact-meta panel">
          <div><span>Trace ID</span><strong>{{ result?.recordId ? `#${result.recordId}` : '待生成' }}</strong></div>
          <div><span>状态</span><StatusTag :status="displayStatus" /></div>
          <div><span>Provider</span><strong>{{ result?.providerName || 'local-rule' }}</strong></div>
          <div><span>模型</span><strong>{{ result?.modelName || 'local-rule-mvp' }}</strong></div>
          <div><span>延迟</span><strong>{{ result?.costTimeMs ?? 0 }}ms</strong></div>
          <div><span>Token</span><strong>{{ result?.totalTokens ?? '—' }}</strong></div>
          <div><span>模板</span><strong>{{ result?.promptTemplateName ? `${result.promptTemplateName} v${result.promptTemplateVersion}` : '—' }}</strong></div>
        </div>
        <div class="trace-card panel">
          <header>
            <strong>运行 Trace</strong>
            <span class="mono">{{ displayStatus }}</span>
          </header>
          <div class="trace-grid">
            <div v-for="item in currentTrace" :key="item.label">
              <span class="mono">{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </div>
        <MarkdownPanel :content="result?.outputContent || ''" title="生成 Artifact" copy-label="复制结果" />
        <div class="review-bar panel">
          <div><strong>人工 Review 状态机</strong><span>先保存生成记录，再标记已确认，避免 Artifact 直接进入交付。</span></div>
          <div class="review-actions">
            <el-button :icon="RefreshRight" :disabled="loading" @click="runGenerate">重新生成</el-button>
            <el-button :icon="DocumentCopy" :disabled="!canSave" @click="saveCurrent">保存记录</el-button>
            <el-button type="success" :icon="CircleCheck" :disabled="!canConfirm" @click="confirmCurrent">标记已确认</el-button>
          </div>
        </div>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.workbench { display: grid; grid-template-columns: minmax(220px, 260px) minmax(360px, 1fr) minmax(330px, 390px); gap: 12px; min-width: 0; align-items: start; }
.workbench.ai-collapsed { grid-template-columns: minmax(220px, 280px) minmax(420px, 1fr) 40px; }
.workbench > *, .panel-header > *, .workbench-toolbar > *, .toolbar-actions > *, .config-grid label > *, .task-flow > *, .result-column > *, .review-bar > *, .trace-card > * { min-width: 0; }
.context-body, .composer-body { display: grid; gap: 12px; }
.full { width: 100%; }
.context-head { display: flex; align-items: center; gap: 9px; padding: 10px; border: var(--border-default); border-left: 2px solid var(--color-accent); border-radius: 4px; background: var(--color-active-row); }
.context-head strong, .context-head span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.context-head span { margin-top: 3px; color: var(--muted); font-size: 10px; }
.file-dot { width: 9px; height: 9px; flex: 0 0 auto; border-radius: 2px; background: var(--color-accent); }
.context-details { display: grid; margin: 0; border: var(--border-default); border-radius: 4px; overflow: hidden; }
.context-details div { padding: 9px 10px; border-bottom: var(--border-subtle); }
.context-details div:last-child { border-bottom: 0; }
.context-details dt { color: var(--muted); font-size: 10px; }
.context-details dd { margin: 4px 0 0; overflow: hidden; font-size: 11px; line-height: 17px; text-overflow: ellipsis; }
.recent-block { display: grid; gap: 1px; background: var(--color-border-subtle); border: var(--border-subtle); }
.recent-block h4 { margin: 0; padding: 8px 9px; background: var(--color-surface-raised); color: var(--muted); font-size: 10px; }
.recent-block article { min-width: 0; padding: 8px 9px; background: var(--color-surface); }
.recent-block strong, .recent-block span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.recent-block strong { font-size: 11px; }.recent-block span { margin-top: 3px; color: var(--muted); font-size: 9px; }
.workbench-toolbar { min-height: 48px; padding: 8px 12px; border-bottom: var(--border-subtle); display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.toolbar-actions, .review-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.editor-shell { border: var(--border-default); border-radius: 4px; background: var(--color-bg); overflow: hidden; }
.editor-bar { height: 30px; padding: 0 10px; border-bottom: var(--border-subtle); display: flex; align-items: center; justify-content: space-between; color: var(--muted); font-size: 10px; }
.editor-bar em { font-style: normal; font-family: var(--font-mono); }.prompt-editor :deep(textarea) { border-radius: 0 !important; box-shadow: none !important; font-family: var(--font-mono); line-height: 1.6; }
.config-grid { display: grid; gap: 8px; }.config-grid label { display: grid; grid-template-columns: 108px minmax(0, 1fr); align-items: start; gap: 10px; }.config-grid label > span { height: 32px; display: flex; align-items: center; color: var(--muted); font-size: 11px; }
.template-hint { margin: 0; padding: 8px 10px; border-left: 2px solid var(--color-info); background: var(--color-surface-raised); color: var(--muted); font-size: 10px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.task-flow { display: flex; align-items: center; gap: 12px; padding: 10px; border: var(--border-default); background: var(--color-bg); overflow-x: auto; }
.task-step { position: relative; display: flex; align-items: center; gap: 6px; color: var(--muted); font-size: 10px; white-space: nowrap; }.task-step:not(:last-child)::after { content: ''; width: 16px; height: 1px; margin-left: 6px; background: var(--color-border); }.task-step.active { color: var(--text); }.task-step.done { color: var(--color-accent); }
.step-dot { width: 16px; height: 16px; display: grid; place-items: center; border: var(--border-default); border-radius: 2px; }.task-step.active .step-dot { border-color: var(--color-accent); background: var(--color-accent-muted); }.failed-label { color: var(--color-error); font-size: 10px; }
.result-column { display: grid; grid-template-columns: 32px minmax(0, 1fr); gap: 8px; min-width: 0; }.ai-collapsed .result-column { grid-template-columns: 40px; }.collapse-rail { width: 32px; min-height: 280px; border: var(--border-default); border-radius: 4px; background: var(--color-surface); color: var(--muted); cursor: pointer; }.collapse-rail:hover { color: var(--text); border-color: var(--color-accent); }
.result-content { min-width: 0; display: grid; gap: 8px; align-content: start; }.artifact-meta { padding: 8px; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1px; background: var(--color-border-subtle); }.artifact-meta div { min-width: 0; padding: 8px; background: var(--color-bg); }.artifact-meta span, .artifact-meta strong { display: block; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.artifact-meta span { color: var(--muted); font-size: 9px; }.artifact-meta strong { margin-top: 4px; font-size: 10px; }
.trace-card { padding: 10px; display: grid; gap: 9px; }.trace-card header { display: flex; align-items: center; justify-content: space-between; gap: 8px; }.trace-card header strong { font-size: 12px; }.trace-card header span { color: var(--color-text-disabled); font-size: 9px; }.trace-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1px; background: var(--color-border-subtle); }.trace-grid div { min-width: 0; padding: 8px; background: var(--color-bg); }.trace-grid span, .trace-grid strong { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.trace-grid span { color: var(--color-text-disabled); font-size: 9px; }.trace-grid strong { margin-top: 4px; font-size: 10px; }
.review-bar { padding: 10px; display: flex; align-items: center; justify-content: space-between; gap: 10px; }.review-bar strong, .review-bar span { display: block; }.review-bar span { margin-top: 3px; color: var(--muted); font-size: 9px; }
@media (max-width: 1100px) { .workbench, .workbench.ai-collapsed { grid-template-columns: 1fr; }.result-column { grid-template-columns: 1fr; }.collapse-rail { display: none; }.result-content { display: grid; } }
</style>
