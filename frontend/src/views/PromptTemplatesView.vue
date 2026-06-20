<script setup lang="ts">
import { computed, onMounted, reactive, shallowRef } from 'vue'
import { Edit, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { createPrompt, fetchPrompts, updatePrompt } from '@/api/devflow'
import type { PromptTemplate } from '@/types/domain'

const prompts = shallowRef<PromptTemplate[]>([])
const loading = shallowRef(false)
const editingId = shallowRef<number>()
const filterType = shallowRef('')
const searchKeyword = shallowRef('')

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

const filteredPrompts = computed(() => prompts.value.filter((item) => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  return (!filterType.value || item.templateType === filterType.value)
    && (!keyword
      || item.templateName.toLowerCase().includes(keyword)
      || item.templateKey.toLowerCase().includes(keyword)
      || (item.variables || '').toLowerCase().includes(keyword))
}))

const promptSummary = computed(() => [
  { label: '模板总数', value: prompts.value.length },
  { label: '启用模板', value: prompts.value.filter((item) => item.enabled).length },
  { label: '默认模板', value: prompts.value.filter((item) => item.isDefault).length },
  { label: '当前筛选', value: filteredPrompts.value.length },
])

const highlightedTemplate = computed(() => escapeHtml(form.templateContent)
  .replace(/\{\{([^}]+)\}\}/g, '<mark>{{$1}}</mark>'))

async function loadPrompts(selectId?: number) {
  loading.value = true
  try {
    prompts.value = await fetchPrompts()
    const target = prompts.value.find((item) => item.id === (selectId || editingId.value)) || prompts.value[0]
    if (target) openEdit(target)
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = undefined
  Object.assign(form, {
    templateKey: '', templateName: '', templateType: 'requirement-split', templateContent: '',
    variables: 'projectName,techStack,requirement,context', enabled: true, isDefault: false, version: 1,
  })
}

function openEdit(item: PromptTemplate) {
  editingId.value = item.id
  Object.assign(form, item)
}

async function submitPrompt() {
  if (!form.templateName.trim() || !form.templateKey.trim() || !form.templateContent.trim()) {
    ElMessage.warning('请填写模板 Key、名称和内容')
    return
  }
  try {
    const saved = editingId.value
      ? await updatePrompt(editingId.value, form)
      : await createPrompt(form)
    ElMessage.success(editingId.value ? `模板已保存为 v${saved.version}` : '模板已创建并可参与生成')
    await loadPrompts(saved.id)
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
}

function insertVariable(name: string) {
  form.templateContent = `${form.templateContent}${form.templateContent.endsWith(' ') ? '' : ' '}{{${name}}}`
  const variables = new Set(form.variables.split(',').map((item) => item.trim()).filter(Boolean))
  variables.add(name)
  form.variables = [...variables].join(',')
}

function typeLabel(value: string) {
  return templateTypes.find((item) => item.value === value)?.label || value
}

function formatTime(value: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '—'
}

function escapeHtml(value: string) {
  return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#039;')
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : '模板操作失败'
}

onMounted(() => loadPrompts())
</script>

<template>
  <div class="templates-page" v-loading="loading">
    <section class="template-list-panel panel">
      <div class="panel-header">
        <div><h3 class="panel-title">Prompt 模板库</h3><p class="panel-subtitle">可复用 Prompt 编排，启用模板会参与 AI 工作台生成</p></div>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建模板</el-button>
      </div>

      <div class="template-filters">
        <div class="template-summary" aria-label="Prompt 模板统计">
          <div v-for="item in promptSummary" :key="item.label">
            <strong class="mono">{{ item.value }}</strong>
            <span>{{ item.label }}</span>
          </div>
        </div>
        <el-input v-model="searchKeyword" placeholder="搜索名称、变量或 Key" />
        <div class="filter-chips">
          <button v-for="item in templateTypes" :key="item.value" :class="{ active: filterType === item.value }" @click="filterType = item.value">{{ item.label }}</button>
        </div>
      </div>

      <div class="template-list">
        <div v-if="!filteredPrompts.length" class="empty-state">暂无匹配模板</div>
        <button v-for="item in filteredPrompts" :key="item.id" class="template-row" :class="{ active: editingId === item.id }" @click="openEdit(item)">
          <span class="template-state" :class="{ enabled: item.enabled }" aria-hidden="true"></span>
          <span class="template-main">
            <strong>{{ item.templateName }}</strong>
            <span class="mono">{{ item.templateKey }} · {{ typeLabel(item.templateType) }}</span>
            <small>{{ item.variables || '无变量' }}</small>
          </span>
          <span class="template-meta">
            <b class="mono">v{{ item.version }}</b>
            <em :class="{ default: item.isDefault }">{{ item.isDefault ? '默认' : item.enabled ? '启用' : '停用' }}</em>
            <small class="mono">{{ formatTime(item.updatedAt) }}</small>
          </span>
        </button>
      </div>
    </section>

    <section class="template-editor panel">
      <div class="panel-header">
        <div>
          <h3 class="panel-title">{{ editingId ? '模板编辑器' : '新建 Prompt 模板' }}</h3>
          <p class="panel-subtitle">保存模板会递增版本，生成历史保留当时渲染内容</p>
        </div>
        <div class="editor-actions">
          <el-dropdown @command="insertVariable">
            <el-button>插入变量</el-button>
            <template #dropdown><el-dropdown-menu>
              <el-dropdown-item command="projectName">projectName</el-dropdown-item>
              <el-dropdown-item command="techStack">techStack</el-dropdown-item>
              <el-dropdown-item command="requirement">requirement</el-dropdown-item>
              <el-dropdown-item command="context">context</el-dropdown-item>
              <el-dropdown-item command="codingRules">codingRules</el-dropdown-item>
              <el-dropdown-item command="errorLog">errorLog</el-dropdown-item>
            </el-dropdown-menu></template>
          </el-dropdown>
          <el-button type="primary" :icon="Edit" @click="submitPrompt">{{ editingId ? '保存新版本' : '创建模板' }}</el-button>
        </div>
      </div>

      <div class="editor-form">
        <label><span>模板 Key</span><el-input v-model="form.templateKey" placeholder="requirement-split" /></label>
        <label><span>模板名称</span><el-input v-model="form.templateName" placeholder="需求拆解模板" /></label>
        <label><span>生成类型</span><el-select v-model="form.templateType"><el-option v-for="item in templateTypes.slice(1)" :key="item.value" :label="item.label" :value="item.value" /></el-select></label>
        <label><span>必填变量</span><el-input v-model="form.variables" placeholder="projectName,requirement,context" /></label>
        <label><span>运行状态</span><div class="status-controls"><el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" /><el-switch v-model="form.isDefault" active-text="默认模板" inactive-text="普通模板" /><b class="mono">v{{ form.version }}</b></div></label>
        <label class="content-field"><span>Prompt 内容</span><el-input v-model="form.templateContent" class="template-textarea mono" type="textarea" :autosize="{ minRows: 11, maxRows: 18 }" placeholder="请基于 {{projectName}} 和 {{techStack}} 处理需求：{{requirement}}" /></label>
        <div class="variables-preview"><span>渲染预览</span><pre v-html="highlightedTemplate"></pre></div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.templates-page { display: grid; grid-template-columns: minmax(360px, 5fr) minmax(0, 7fr); gap: 12px; min-width: 0; align-items: start; }
.templates-page > *, .panel-header > *, .template-row > *, .editor-form label > *, .variables-preview > * { min-width: 0; }
.template-filters { padding: 10px; display: grid; gap: 8px; border-bottom: var(--border-subtle); }
.template-summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1px; border: var(--border-subtle); border-radius: 4px; background: var(--color-border-subtle); overflow: hidden; }.template-summary div { min-width: 0; padding: 8px; background: var(--color-bg); }.template-summary strong, .template-summary span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.template-summary strong { color: var(--color-accent); font-size: 14px; }.template-summary span { margin-top: 4px; color: var(--muted); font-size: 9px; }
.filter-chips { display: flex; gap: 4px; flex-wrap: wrap; }.filter-chips button { height: 25px; padding: 0 7px; border: 1px solid transparent; border-radius: 3px; background: transparent; color: var(--muted); cursor: pointer; font-size: 10px; }.filter-chips button.active, .filter-chips button:hover { border-color: var(--color-border); background: var(--color-active-row); color: var(--text); }
.template-list { display: grid; }.template-row { width: 100%; min-height: 72px; display: grid; grid-template-columns: 8px minmax(0, 1fr) 96px; align-items: center; gap: 9px; padding: 9px 10px; border: 0; border-bottom: var(--border-subtle); background: transparent; color: inherit; cursor: pointer; text-align: left; }.template-row:last-child { border-bottom: 0; }.template-row:hover, .template-row.active { background: var(--color-active-row); }.template-row.active { box-shadow: inset 2px 0 var(--color-accent); }
.template-state { width: 7px; height: 7px; border: 1px solid var(--color-text-ghost); border-radius: 2px; }.template-state.enabled { border-color: var(--color-accent); background: var(--color-accent); }
.template-main strong, .template-main span, .template-main small, .template-meta * { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.template-main strong { font-size: 12px; }.template-main span { margin-top: 3px; color: var(--muted); font-size: 9px; }.template-main small { margin-top: 5px; color: var(--color-text-disabled); font-size: 9px; }
.template-meta { text-align: right; }.template-meta b { font-size: 11px; }.template-meta em { margin-top: 4px; color: var(--muted); font-size: 9px; font-style: normal; }.template-meta em.default { color: var(--color-warning); }.template-meta small { margin-top: 5px; color: var(--color-text-disabled); font-size: 8px; }
.editor-actions, .status-controls { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }.editor-form { padding: 12px; display: grid; gap: 9px; }.editor-form label, .variables-preview { display: grid; grid-template-columns: 112px minmax(0, 1fr); align-items: start; gap: 10px; }.editor-form label > span, .variables-preview > span { min-height: 32px; display: flex; align-items: center; color: var(--muted); font-size: 11px; }.status-controls { min-height: 32px; }.status-controls b { margin-left: auto; color: var(--color-accent); font-size: 11px; }
.template-textarea :deep(textarea) { font-family: var(--font-mono); line-height: 1.6; }.variables-preview pre { margin: 0; min-height: 132px; max-height: 260px; overflow: auto; padding: 10px; border: var(--border-default); border-radius: 4px; background: var(--color-bg); color: var(--muted); font: 10px/1.65 var(--font-mono); white-space: pre-wrap; }.variables-preview :deep(mark) { padding: 0 2px; border-radius: 2px; background: rgba(199, 164, 90, .12); color: var(--color-warning); }
@media (max-width: 1050px) { .templates-page { grid-template-columns: 1fr; } }
</style>
