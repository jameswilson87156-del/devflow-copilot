<script setup lang="ts">
import { computed, onMounted, reactive, shallowRef } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  createKnowledgeDocument,
  fetchKnowledgeChunks,
  fetchKnowledgeDocuments,
  searchKnowledge,
} from '@/api/devflow'
import type { KnowledgeChunk, KnowledgeDocument, KnowledgeReference } from '@/types/domain'

const documents = shallowRef<KnowledgeDocument[]>([])
const chunks = shallowRef<KnowledgeChunk[]>([])
const results = shallowRef<KnowledgeReference[]>([])
const selectedDocumentId = shallowRef<number>()
const loading = shallowRef(false)

const form = reactive({
  title: 'DevFlow Copilot 演示补充说明',
  sourceType: 'manual',
  sourceUri: 'docs/local-note.md',
  content: 'DevFlow Copilot 默认使用 local-rule 本地规则生成，真实模型调用只通过环境变量配置 API Key。Generation Trace 记录 Prompt 版本、Provider、模型、耗时和错误信息。Agent Run Trace 记录步骤、工具调用和人工确认状态。',
  embeddingModel: '',
  metadata: 'demo=true',
})

const searchForm = reactive({
  query: 'local-rule Provider API Key Generation Trace',
  topK: 5,
})

const selectedDocument = computed(() => documents.value.find((item) => item.id === selectedDocumentId.value))
const totalChunks = computed(() => documents.value.reduce((sum, item) => sum + (item.chunkCount || 0), 0))

async function loadDocuments(selectId?: number) {
  loading.value = true
  try {
    documents.value = await fetchKnowledgeDocuments()
    selectedDocumentId.value = selectId || selectedDocumentId.value || documents.value[0]?.id
    await loadChunks()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    loading.value = false
  }
}

async function loadChunks() {
  chunks.value = selectedDocumentId.value ? await fetchKnowledgeChunks(selectedDocumentId.value) : []
}

async function submitDocument() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('请填写标题和正文')
    return
  }
  try {
    const saved = await createKnowledgeDocument({ ...form })
    ElMessage.success(`知识文档已创建并切为 ${saved.chunkCount} 个 chunks`)
    await loadDocuments(saved.id)
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
}

async function runSearch() {
  if (!searchForm.query.trim()) {
    ElMessage.warning('请输入检索关键词')
    return
  }
  try {
    results.value = await searchKnowledge({
      query: searchForm.query,
      documentIds: selectedDocumentId.value ? [selectedDocumentId.value] : undefined,
      topK: searchForm.topK,
    })
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '—'
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : 'Knowledge Base 操作失败'
}

onMounted(async () => {
  await loadDocuments()
  await runSearch()
})
</script>

<template>
  <div class="knowledge-page" v-loading="loading">
    <section class="document-panel panel">
      <div class="panel-header">
        <div>
          <h3 class="panel-title">Knowledge Base</h3>
          <p class="panel-subtitle">轻量 RAG：文档、chunks、关键词检索与生成引用</p>
        </div>
      </div>

      <div class="kb-summary">
        <div><strong class="mono">{{ documents.length }}</strong><span>Documents</span></div>
        <div><strong class="mono">{{ totalChunks }}</strong><span>Chunks</span></div>
        <div><strong class="mono">keyword</strong><span>当前检索模式</span></div>
      </div>

      <div class="document-list">
        <button
          v-for="document in documents"
          :key="document.id"
          type="button"
          class="document-row"
          :class="{ active: selectedDocumentId === document.id }"
          @click="selectedDocumentId = document.id; loadChunks()"
        >
          <span class="doc-mark" aria-hidden="true"></span>
          <span>
            <strong>{{ document.title }}</strong>
            <small class="mono">{{ document.sourceType }} · {{ document.chunkCount }} chunks · {{ formatTime(document.updatedAt) }}</small>
          </span>
        </button>
      </div>
    </section>

    <section class="workspace">
      <div class="top-grid">
        <section class="create-panel panel">
          <div class="panel-header">
            <div>
              <h3 class="panel-title">创建知识文档</h3>
              <p class="panel-subtitle">保存后自动切片；embedding 字段仅为后续向量库预留</p>
            </div>
            <el-button type="primary" :icon="Plus" @click="submitDocument">创建并切片</el-button>
          </div>
          <div class="create-form">
            <label><span>标题</span><el-input v-model="form.title" /></label>
            <label><span>来源</span><el-input v-model="form.sourceUri" /></label>
            <label><span>Source Type</span><el-input v-model="form.sourceType" /></label>
            <label><span>Embedding</span><el-input v-model="form.embeddingModel" placeholder="预留字段，当前可留空" /></label>
            <label class="wide"><span>正文</span><el-input v-model="form.content" type="textarea" :autosize="{ minRows: 5, maxRows: 9 }" /></label>
          </div>
        </section>

        <section class="search-panel panel">
          <div class="panel-header">
            <div>
              <h3 class="panel-title">检索与引用</h3>
              <p class="panel-subtitle">返回 chunk 引用，生成接口会把命中结果写入响应</p>
            </div>
            <el-button :icon="Search" @click="runSearch">检索</el-button>
          </div>
          <div class="search-form">
            <el-input v-model="searchForm.query" placeholder="输入关键词或需求" />
            <el-input-number v-model="searchForm.topK" :min="1" :max="10" />
          </div>
          <div class="search-results">
            <div v-if="!results.length" class="empty-state">暂无检索结果</div>
            <article v-for="item in results" :key="item.chunkId">
              <div class="result-head">
                <strong>{{ item.citationLabel }}</strong>
                <span class="mono">score {{ item.score.toFixed(1) }}</span>
              </div>
              <p>{{ item.snippet }}</p>
            </article>
          </div>
        </section>
      </div>

      <section class="chunks-panel panel">
        <div class="panel-header">
          <div>
            <h3 class="panel-title">文档切片</h3>
            <p class="panel-subtitle">{{ selectedDocument?.title || '未选择文档' }}</p>
          </div>
          <span class="mono chunk-count">{{ chunks.length }} chunks</span>
        </div>
        <div class="chunk-grid">
          <div v-if="!chunks.length" class="empty-state">选择知识文档后查看 chunks</div>
          <article v-for="chunk in chunks" :key="chunk.id">
            <header>
              <strong class="mono">#{{ chunk.chunkIndex }}</strong>
              <span>{{ chunk.embeddingModel || 'embedding 待接入' }}</span>
            </header>
            <p>{{ chunk.contentSummary || chunk.content }}</p>
            <small class="mono">{{ chunk.keywords || 'keywords pending' }}</small>
          </article>
        </div>
      </section>
    </section>
  </div>
</template>

<style scoped>
.knowledge-page { display: grid; grid-template-columns: minmax(300px, 340px) minmax(0, 1fr); gap: 12px; min-width: 0; align-items: start; }
.knowledge-page > *, .document-row > *, .top-grid > *, .create-form label > *, .result-head > * { min-width: 0; }
.kb-summary { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 1px; padding: 1px; border-bottom: var(--border-subtle); background: var(--color-border-subtle); }
.kb-summary div { min-width: 0; padding: 10px 8px; background: var(--color-bg); text-align: center; }
.kb-summary strong, .kb-summary span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.kb-summary strong { color: var(--color-accent); font-size: 14px; }
.kb-summary span { margin-top: 5px; color: var(--color-text-secondary); font-size: 9px; }
.document-list { display: grid; }
.document-row { width: 100%; min-height: 62px; display: grid; grid-template-columns: 8px minmax(0, 1fr); gap: 9px; align-items: center; padding: 9px 10px; border: 0; border-bottom: var(--border-subtle); background: transparent; color: inherit; text-align: left; cursor: pointer; }
.document-row:hover, .document-row.active { background: var(--color-active-row); }
.document-row.active { box-shadow: inset 2px 0 var(--color-accent); }
.doc-mark { width: 7px; height: 18px; border-radius: 2px; background: var(--color-accent); }
.document-row strong, .document-row small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.document-row strong { font-size: 12px; }
.document-row small { margin-top: 5px; color: var(--color-text-disabled); font-size: 9px; }
.workspace { display: grid; gap: 12px; min-width: 0; }
.top-grid { display: grid; grid-template-columns: minmax(0, 1.1fr) minmax(320px, .9fr); gap: 12px; align-items: start; }
.create-form { padding: 12px; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 9px; }
.create-form label { display: grid; grid-template-columns: 88px minmax(0, 1fr); gap: 8px; align-items: start; }
.create-form label.wide { grid-column: 1 / -1; }
.create-form label span { min-height: 32px; display: flex; align-items: center; color: var(--color-text-secondary); font-size: 11px; }
.search-form { padding: 12px; display: grid; grid-template-columns: minmax(0, 1fr) 96px; gap: 8px; border-bottom: var(--border-subtle); }
.search-results { display: grid; }
.search-results article { padding: 11px 12px; border-bottom: var(--border-subtle); }
.search-results article:last-child { border-bottom: 0; }
.result-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.result-head strong { overflow: hidden; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.result-head span { color: var(--color-accent); font-size: 9px; }
.search-results p { margin: 7px 0 0; color: var(--color-text-secondary); font-size: 11px; line-height: 17px; }
.chunk-count { color: var(--color-text-disabled); font-size: 9px; }
.chunk-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1px; padding: 1px; background: var(--color-border-subtle); }
.chunk-grid article { min-width: 0; padding: 12px; background: var(--color-surface); }
.chunk-grid header { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.chunk-grid header strong { color: var(--color-accent); font-size: 11px; }
.chunk-grid header span { overflow: hidden; color: var(--color-text-disabled); font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }
.chunk-grid p { margin: 8px 0 0; color: var(--color-text-secondary); font-size: 11px; line-height: 18px; }
.chunk-grid small { display: block; margin-top: 8px; overflow: hidden; color: var(--color-text-disabled); font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }
@media (max-width: 1120px) { .knowledge-page, .top-grid, .chunk-grid { grid-template-columns: 1fr; } }
</style>
