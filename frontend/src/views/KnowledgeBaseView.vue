<script setup lang="ts">
import { computed, onMounted, reactive, shallowRef } from 'vue'
import { Plus, RefreshRight, Search, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  createKnowledgeDocument,
  fetchAgentRuns,
  fetchGenerations,
  fetchKnowledgeChunks,
  fetchKnowledgeDocuments,
  fetchKnowledgeReferences,
  searchKnowledge,
} from '@/api/devflow'
import CodeBlock from '@/components/CodeBlock.vue'
import MetricCard from '@/components/MetricCard.vue'
import ProviderBadge from '@/components/ProviderBadge.vue'
import SectionCard from '@/components/SectionCard.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import type {
  AgentRun,
  GenerationRecord,
  KnowledgeChunk,
  KnowledgeDocument,
  KnowledgeReference,
} from '@/types/domain'

type DetailTab = 'chunks' | 'hits' | 'timeline' | 'info' | 'access'
type RetrievalStatus = 'SUCCESS' | 'FAILED'

interface RetrievalHistoryRow {
  id: string
  query: string
  resultCount: number
  scope: string
  status: RetrievalStatus
  durationMs: number
  createdAt: string
}

interface ReferenceGroup {
  record: GenerationRecord
  references: KnowledgeReference[]
  agentRun?: AgentRun
}

interface ReferenceHistoryRow {
  key: string
  reference: KnowledgeReference
  record: GenerationRecord
  agentRun?: AgentRun
}

const router = useRouter()

const SOURCE_FILTERS = ['全部来源', 'Markdown', 'API 文档', 'GitBook', 'Notion', 'Confluence', 'Local'] as const
const STATUS_FILTERS = ['全部状态', '已索引', '待审核', '草稿', '已发布', '失败'] as const
const DETAIL_TABS: Array<{ key: DetailTab; label: string }> = [
  { key: 'chunks', label: 'Chunk 切片' },
  { key: 'hits', label: '命中结果' },
  { key: 'timeline', label: '检索时间线' },
  { key: 'info', label: '文档信息' },
  { key: 'access', label: '访问控制' },
]

const documents = shallowRef<KnowledgeDocument[]>([])
const chunks = shallowRef<KnowledgeChunk[]>([])
const results = shallowRef<KnowledgeReference[]>([])
const selectedDocumentId = shallowRef<number>()
const selectedResultKey = shallowRef('')
const generationReferenceGroups = shallowRef<ReferenceGroup[]>([])
const retrievalHistory = shallowRef<RetrievalHistoryRow[]>([])
const activeDetailTab = shallowRef<DetailTab>('chunks')
const loading = shallowRef(false)
const searching = shallowRef(false)
const showCreateForm = shallowRef(false)
const documentKeyword = shallowRef('')
const sourceFilter = shallowRef('全部来源')
const statusFilter = shallowRef('全部状态')

const form = reactive({
  title: 'DevFlow Copilot 项目边界说明',
  sourceType: 'manual',
  sourceUri: 'docs/local-note.md',
  content: [
    'DevFlow Copilot 默认使用 local-rule 本地规则生成，不需要提交 API Key。',
    'Generation Trace 记录 Prompt 版本、Provider、模型、耗时和错误信息。',
    'Agent Run Trace 记录步骤、工具调用和 Human Review 状态。',
    'Knowledge Base 当前使用关键词 / 简单相似度检索，embedding 字段仅作为后续扩展点。',
  ].join('\n\n'),
  embeddingModel: '',
  metadata: 'source=manual;status=indexed',
})

const searchForm = reactive({
  query: 'local-rule Provider API Key Generation Trace',
  topK: 5,
})

const selectedDocument = computed(() => documents.value.find((item) => item.id === selectedDocumentId.value))
const totalChunks = computed(() => documents.value.reduce((sum, item) => sum + (item.chunkCount || 0), 0))
const referenceHistoryRows = computed<ReferenceHistoryRow[]>(() => {
  return generationReferenceGroups.value.flatMap((group) => {
    return group.references.map((reference) => ({
      key: `${group.record.id}-${reference.chunkId}-${reference.chunkIndex}`,
      reference,
      record: group.record,
      agentRun: group.agentRun,
    }))
  })
})
const totalReferenceCount = computed(() => referenceHistoryRows.value.length)
const selectedSearchResult = computed(() => {
  if (!results.value.length) return undefined
  return results.value.find((item) => referenceKey(item) === selectedResultKey.value) || results.value[0]
})
const selectedReferenceUsage = computed(() => {
  const result = selectedSearchResult.value
  if (!result) return []
  return referenceHistoryRows.value.filter((item) => item.reference.chunkId === result.chunkId)
})
const selectedUsageRecord = computed(() => selectedReferenceUsage.value[0])
const firstUsedAt = computed(() => {
  const rows = selectedReferenceUsage.value
  if (!rows.length) return undefined
  return rows.slice().sort((a, b) => toTime(a.record.createdAt) - toTime(b.record.createdAt))[0]?.record.createdAt
})
const recentUsedAt = computed(() => {
  const rows = selectedReferenceUsage.value
  if (!rows.length) return undefined
  return rows.slice().sort((a, b) => toTime(b.record.updatedAt || b.record.createdAt) - toTime(a.record.updatedAt || a.record.createdAt))[0]?.record.updatedAt
    || rows[0]?.record.createdAt
})
const scoreByChunkId = computed(() => {
  const scores = new Map<number, number>()
  results.value.forEach((item) => scores.set(item.chunkId, item.score))
  return scores
})
const filteredDocuments = computed(() => {
  const keyword = documentKeyword.value.trim().toLowerCase()
  return documents.value.filter((document) => {
    const titleMatch = !keyword
      || document.title.toLowerCase().includes(keyword)
      || (document.sourceUri || '').toLowerCase().includes(keyword)
      || sourceLabel(document.sourceType).toLowerCase().includes(keyword)
    const sourceMatch = sourceFilter.value === '全部来源' || sourceLabel(document.sourceType) === sourceFilter.value
    const statusMatch = statusFilter.value === '全部状态' || documentStatusLabel(document) === statusFilter.value
    return titleMatch && sourceMatch && statusMatch
  })
})
const sourceOptions = computed(() => {
  const actual = documents.value.map((document) => sourceLabel(document.sourceType))
  return Array.from(new Set([...SOURCE_FILTERS, ...actual]))
})
const documentMetrics = computed(() => [
  { label: '知识文档', value: documents.value.length, code: 'Documents', tone: 'accent' as const },
  { label: 'Chunk 切片', value: totalChunks.value, code: 'Chunks', tone: 'running' as const },
  { label: '当前命中', value: results.value.length, code: 'Keyword Hits', tone: 'success' as const },
  { label: '生成引用', value: totalReferenceCount.value, code: 'References', tone: 'warning' as const },
])
const activeScopeLabel = computed(() => selectedDocument.value?.title || '全部文档')
const retrievalTimelineRows = computed(() => {
  const searchRows = retrievalHistory.value.map((item) => ({
    key: item.id,
    title: item.query,
    detail: `${item.scope} / ${item.resultCount} 个结果 / ${formatDuration(item.durationMs)}`,
    status: item.status,
    time: item.createdAt,
  }))
  const referenceRows = referenceHistoryRows.value.slice(0, 8).map((item) => ({
    key: item.key,
    title: item.reference.citationLabel,
    detail: `Generation Record #${item.record.id} / score ${formatScore(item.reference.score)}`,
    status: normalizeStatus(item.record.status),
    time: item.record.updatedAt || item.record.createdAt,
  }))
  return [...searchRows, ...referenceRows]
    .sort((a, b) => toTime(b.time) - toTime(a.time))
    .slice(0, 10)
})
const indexLogRows = computed(() => documents.value.slice(0, 8).map((document) => ({
  key: document.id,
  document: document.title,
  status: documentStatusCode(document),
  statusLabel: documentStatusLabel(document),
  chunkCount: document.chunkCount || 0,
  duration: '未记录',
  time: document.updatedAt,
})))

async function loadPage() {
  loading.value = true
  try {
    documents.value = await fetchKnowledgeDocuments()
    selectedDocumentId.value = selectedDocumentId.value || documents.value[0]?.id
    await Promise.all([
      loadChunks(),
      loadGenerationReferenceHistory(),
    ])
    if (searchForm.query.trim()) {
      await runSearch()
    }
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    loading.value = false
  }
}

async function loadChunks() {
  chunks.value = selectedDocumentId.value ? await fetchKnowledgeChunks(selectedDocumentId.value) : []
}

async function loadGenerationReferenceHistory() {
  const generations = await fetchGenerations()
  const recent = generations
    .slice()
    .sort((a, b) => toTime(b.updatedAt || b.createdAt) - toTime(a.updatedAt || a.createdAt))
    .slice(0, 8)

  const groups = await Promise.all(recent.map(async (record) => {
    const [references, runs] = await Promise.all([
      fetchKnowledgeReferences(record.id).catch(() => [] as KnowledgeReference[]),
      fetchAgentRuns({ generationRecordId: record.id }).catch(() => [] as AgentRun[]),
    ])
    return {
      record,
      references,
      agentRun: runs[0],
    }
  }))
  generationReferenceGroups.value = groups.filter((group) => group.references.length > 0)
}

async function selectDocument(id: number) {
  selectedDocumentId.value = id
  await loadChunks()
  if (searchForm.query.trim()) {
    await runSearch()
  }
}

async function submitDocument() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('请填写文档名称和正文内容')
    return
  }
  try {
    const saved = await createKnowledgeDocument({ ...form })
    ElMessage.success(`知识文档已创建，并切为 ${saved.chunkCount} 个 Chunk`)
    showCreateForm.value = false
    selectedDocumentId.value = saved.id
    await loadPage()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
}

async function runSearch() {
  if (!searchForm.query.trim()) {
    ElMessage.warning('请输入检索问题或关键词')
    return
  }
  const started = performance.now()
  searching.value = true
  try {
    const data = await searchKnowledge({
      query: searchForm.query,
      documentIds: selectedDocumentId.value ? [selectedDocumentId.value] : undefined,
      topK: searchForm.topK,
    })
    results.value = data
    selectedResultKey.value = data[0] ? referenceKey(data[0]) : ''
    activeDetailTab.value = data.length ? 'hits' : activeDetailTab.value
    retrievalHistory.value = [
      {
        id: `search-${Date.now()}`,
        query: searchForm.query,
        resultCount: data.length,
        scope: activeScopeLabel.value,
        status: 'SUCCESS' as RetrievalStatus,
        durationMs: Math.round(performance.now() - started),
        createdAt: new Date().toISOString(),
      },
      ...retrievalHistory.value,
    ].slice(0, 8)
  } catch (error) {
    retrievalHistory.value = [
      {
        id: `search-${Date.now()}`,
        query: searchForm.query,
        resultCount: 0,
        scope: activeScopeLabel.value,
        status: 'FAILED' as RetrievalStatus,
        durationMs: Math.round(performance.now() - started),
        createdAt: new Date().toISOString(),
      },
      ...retrievalHistory.value,
    ].slice(0, 8)
    ElMessage.error(errorMessage(error))
  } finally {
    searching.value = false
  }
}

function openCreateForm() {
  showCreateForm.value = true
  activeDetailTab.value = 'info'
}

function viewReview() {
  const record = selectedUsageRecord.value?.record
  if (!record) return
  router.push({ path: '/agent-runs', query: { generationRecordId: String(record.id) } })
}

function referenceKey(reference: KnowledgeReference) {
  return `${reference.documentId}-${reference.chunkId}-${reference.chunkIndex}`
}

function sourceLabel(sourceType?: string) {
  const raw = (sourceType || 'manual').toLowerCase()
  if (raw.includes('api')) return 'API 文档'
  if (raw.includes('gitbook')) return 'GitBook'
  if (raw.includes('notion')) return 'Notion'
  if (raw.includes('confluence')) return 'Confluence'
  if (raw.includes('local') || raw.includes('demo') || raw.includes('portfolio')) return 'Local'
  if (raw.includes('markdown') || raw.includes('manual') || raw === 'md') return 'Markdown'
  return sourceType || '未记录'
}

function documentStatusLabel(document: KnowledgeDocument) {
  const metaStatus = metadataStatus(document.metadata)
  if (metaStatus) return metaStatus
  if ((document.chunkCount || 0) > 0) return '已索引'
  if (document.content?.trim()) return '草稿'
  return '待审核'
}

function documentStatusCode(document: KnowledgeDocument) {
  const label = documentStatusLabel(document)
  const codeMap: Record<string, string> = {
    已索引: 'SUCCESS',
    待审核: 'PENDING',
    草稿: 'DRAFT',
    已发布: 'CONFIRMED',
    失败: 'FAILED',
  }
  return codeMap[label] || 'DRAFT'
}

function metadataStatus(metadata?: string) {
  if (!metadata) return ''
  const match = metadata.toLowerCase().match(/status\s*[:=]\s*([\w-]+)/)
  const value = match?.[1]
  const map: Record<string, string> = {
    indexed: '已索引',
    ready: '已索引',
    pending: '待审核',
    review: '待审核',
    draft: '草稿',
    published: '已发布',
    failed: '失败',
  }
  return value ? (map[value] || '') : ''
}

function normalizeStatus(status: string) {
  const map: Record<string, string> = {
    Draft: 'DRAFT',
    Generating: 'GENERATING',
    'Ready for Review': 'READY_FOR_REVIEW',
    Saved: 'SAVED',
    Confirmed: 'CONFIRMED',
    Failed: 'FAILED',
  }
  return map[status] || status.toUpperCase().replace(/\s+/g, '_')
}

function statusText(status: string) {
  const labels: Record<string, string> = {
    DRAFT: '草稿',
    GENERATING: '运行中',
    RUNNING: '运行中',
    READY_FOR_REVIEW: '待审核',
    PENDING: '待审核',
    SAVED: '已保存',
    CONFIRMED: '已确认',
    SUCCESS: '成功',
    FAILED: '失败',
    REJECTED: '已驳回',
  }
  return labels[normalizeStatus(status)] || status
}

function chunkCode(chunk: KnowledgeChunk | KnowledgeReference) {
  if ('chunkIndex' in chunk && chunk.chunkIndex !== undefined && chunk.chunkIndex !== null) {
    return `chunk_${String(chunk.chunkIndex).padStart(4, '0')}`
  }
  return 'chunk_未记录'
}

function chunkSummary(chunk: KnowledgeChunk) {
  return chunk.contentSummary || summaryText(chunk.content, 118) || '未记录摘要'
}

function chunkScore(chunk: KnowledgeChunk) {
  const score = scoreByChunkId.value.get(chunk.id)
  return typeof score === 'number' ? formatScore(score) : results.value.length ? '未命中' : '未检索'
}

function chunkLastUsed(chunk: KnowledgeChunk) {
  const rows = referenceHistoryRows.value.filter((item) => item.reference.chunkId === chunk.id)
  if (!rows.length) return '未记录'
  const latest = rows.sort((a, b) => toTime(b.record.updatedAt || b.record.createdAt) - toTime(a.record.updatedAt || a.record.createdAt))[0]
  return formatTime(latest.record.updatedAt || latest.record.createdAt)
}

function summaryText(value?: string, limit = 140) {
  const clean = (value || '').replace(/\s+/g, ' ').trim()
  return clean.length > limit ? `${clean.slice(0, limit)}...` : clean
}

function formatScore(value?: number) {
  return typeof value === 'number' ? value.toFixed(2) : '未记录'
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '未记录'
}

function formatDuration(value?: number) {
  if (typeof value !== 'number') return '未记录'
  if (value < 1000) return `${value}ms`
  return `${(value / 1000).toFixed(1)}s`
}

function toTime(value?: string) {
  return value ? new Date(value).getTime() || 0 : 0
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : 'Knowledge Base 操作失败'
}

onMounted(loadPage)
</script>

<template>
  <div class="knowledge-page" v-loading="loading">
    <header class="knowledge-header">
      <div>
        <p class="eyebrow mono">Knowledge / RAG</p>
        <h2>知识库</h2>
        <p>管理知识文档、Chunk 切片、关键词检索结果与生成引用，把文档到 Trace 的引用链路留在同一张工作台。</p>
      </div>
      <div class="pipeline-strip" aria-label="知识引用链路">
        <span>文档</span>
        <span>Chunk</span>
        <span>检索</span>
        <span>引用</span>
        <span>生成</span>
        <span>Trace</span>
      </div>
    </header>

    <div class="metric-strip">
      <MetricCard
        v-for="metric in documentMetrics"
        :key="metric.code"
        :label="metric.label"
        :value="metric.value"
        :code="metric.code"
        :tone="metric.tone"
      />
    </div>

    <aside class="knowledge-left">
      <SectionCard title="文档列表" subtitle="优先展示真实知识文档" eyebrow="Documents" compact>
        <template #actions>
          <el-button type="primary" :icon="Plus" @click="openCreateForm">新增文档</el-button>
        </template>

        <div class="document-tools">
          <el-input v-model="documentKeyword" :prefix-icon="Search" placeholder="搜索文档、来源或路径" clearable />
          <div class="filter-row">
            <el-select v-model="sourceFilter" size="default" aria-label="来源筛选">
              <el-option v-for="source in sourceOptions" :key="source" :label="source" :value="source" />
            </el-select>
            <el-select v-model="statusFilter" size="default" aria-label="状态筛选">
              <el-option v-for="status in STATUS_FILTERS" :key="status" :label="status" :value="status" />
            </el-select>
          </div>
        </div>

        <div class="document-list">
          <button
            v-for="document in filteredDocuments"
            :key="document.id"
            type="button"
            class="document-row"
            :class="{ active: selectedDocumentId === document.id }"
            @click="selectDocument(document.id)"
          >
            <span class="doc-mark" aria-hidden="true"></span>
            <span class="doc-main">
              <strong>{{ document.title }}</strong>
              <small>{{ sourceLabel(document.sourceType) }} / {{ document.chunkCount || 0 }} Chunks / {{ formatTime(document.updatedAt) }}</small>
            </span>
            <StatusBadge :status="documentStatusCode(document)" :label="documentStatusLabel(document)" />
          </button>
          <div v-if="!filteredDocuments.length" class="empty-state">暂无符合筛选条件的知识文档。</div>
        </div>
      </SectionCard>
    </aside>

    <main class="knowledge-center">
      <SectionCard
        class="document-detail-card"
        :title="selectedDocument?.title || '未选择文档'"
        subtitle="文档详情、Chunk 切片与关键词检索"
        eyebrow="Document Detail"
      >
        <template #actions>
          <ProviderBadge provider="keyword-search" model="RAG 引用" />
        </template>

        <div v-if="selectedDocument" class="document-overview">
          <div>
            <span>来源</span>
            <strong>{{ sourceLabel(selectedDocument.sourceType) }}</strong>
          </div>
          <div>
            <span>状态</span>
            <StatusBadge :status="documentStatusCode(selectedDocument)" :label="documentStatusLabel(selectedDocument)" />
          </div>
          <div>
            <span>Chunk 数</span>
            <strong class="mono">{{ selectedDocument.chunkCount || 0 }}</strong>
          </div>
          <div>
            <span>更新时间</span>
            <strong class="mono">{{ formatTime(selectedDocument.updatedAt) }}</strong>
          </div>
          <div class="wide">
            <span>文档路径 / 来源链接</span>
            <strong class="mono">{{ selectedDocument.sourceUri || '未记录' }}</strong>
          </div>
        </div>

        <div class="detail-tabs" role="tablist" aria-label="文档详情分区">
          <button
            v-for="tab in DETAIL_TABS"
            :key="tab.key"
            type="button"
            :class="{ active: activeDetailTab === tab.key }"
            @click="activeDetailTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>

        <div class="search-dock">
          <label>
            <span>检索问题</span>
            <el-input v-model="searchForm.query" placeholder="例如：Agent 如何处理 Tool 调用失败？" />
          </label>
          <label>
            <span>Top K</span>
            <el-input-number v-model="searchForm.topK" :min="1" :max="10" />
          </label>
          <el-button type="primary" :icon="Search" :loading="searching" @click="runSearch">关键词检索</el-button>
        </div>

        <div v-if="activeDetailTab === 'chunks'" class="chunk-table">
          <div class="chunk-table-head">
            <span>Chunk ID</span>
            <span>摘要</span>
            <span>关键词</span>
            <span>Token 数</span>
            <span>最近使用</span>
            <span>相关度</span>
          </div>
          <article v-for="chunk in chunks" :key="chunk.id" class="chunk-row">
            <strong class="mono">{{ chunkCode(chunk) }}</strong>
            <span>{{ chunkSummary(chunk) }}</span>
            <small class="mono">{{ chunk.keywords || '未记录' }}</small>
            <small>未记录</small>
            <small class="mono">{{ chunkLastUsed(chunk) }}</small>
            <strong class="mono score-cell">{{ chunkScore(chunk) }}</strong>
          </article>
          <div v-if="!chunks.length" class="empty-state">选择知识文档后查看真实 Chunk 切片。</div>
        </div>

        <div v-else-if="activeDetailTab === 'hits'" class="hit-list">
          <article
            v-for="item in results"
            :key="referenceKey(item)"
            class="hit-row"
            :class="{ active: referenceKey(item) === referenceKey(selectedSearchResult || item) }"
            @click="selectedResultKey = referenceKey(item)"
          >
            <div>
              <strong>{{ item.citationLabel }}</strong>
              <small>{{ item.documentTitle }} / {{ chunkCode(item) }}</small>
            </div>
            <span class="mono">score {{ formatScore(item.score) }}</span>
            <p>{{ item.snippet }}</p>
          </article>
          <div v-if="!results.length" class="empty-state">暂无检索命中。当前后端使用关键词 / 简单相似度检索。</div>
        </div>

        <div v-else-if="activeDetailTab === 'timeline'" class="timeline-list">
          <article v-for="item in retrievalTimelineRows" :key="item.key" class="timeline-row">
            <span class="timeline-dot" :data-status="normalizeStatus(item.status)"></span>
            <div>
              <strong>{{ item.title }}</strong>
              <small>{{ item.detail }}</small>
            </div>
            <time class="mono">{{ formatTime(item.time) }}</time>
          </article>
          <div v-if="!retrievalTimelineRows.length" class="empty-state">暂无检索时间线或生成引用记录。</div>
        </div>

        <div v-else-if="activeDetailTab === 'info'" class="document-info-grid">
          <div class="info-panel">
            <span>Source Type</span>
            <strong>{{ selectedDocument?.sourceType || '未记录' }}</strong>
          </div>
          <div class="info-panel">
            <span>Embedding 扩展点</span>
            <strong>{{ selectedDocument?.embeddingModel || '待接入' }}</strong>
          </div>
          <div class="info-panel wide">
            <span>Metadata</span>
            <strong class="mono">{{ selectedDocument?.metadata || '未记录' }}</strong>
          </div>
          <CodeBlock class="document-code" title="文档正文" language="markdown" :code="selectedDocument?.content || '暂无正文内容。'" />
          <form v-if="showCreateForm" class="create-form" @submit.prevent="submitDocument">
            <h3>新增知识文档</h3>
            <label><span>文档名称</span><el-input v-model="form.title" /></label>
            <label><span>来源类型</span><el-input v-model="form.sourceType" placeholder="manual / api / local" /></label>
            <label><span>来源路径</span><el-input v-model="form.sourceUri" /></label>
            <label><span>Embedding</span><el-input v-model="form.embeddingModel" placeholder="预留字段，可留空" /></label>
            <label class="wide"><span>正文</span><el-input v-model="form.content" type="textarea" :autosize="{ minRows: 5, maxRows: 8 }" /></label>
            <label class="wide"><span>Metadata</span><el-input v-model="form.metadata" /></label>
            <div class="form-actions">
              <el-button @click="showCreateForm = false">取消</el-button>
              <el-button type="primary" native-type="submit" :icon="Plus">创建并切片</el-button>
            </div>
          </form>
        </div>

        <div v-else class="access-grid">
          <div>
            <span>访问范围</span>
            <strong>未记录</strong>
            <small>后端当前未提供文档级权限字段。</small>
          </div>
          <div>
            <span>权限模型</span>
            <strong>待接入</strong>
            <small>不展示为已完成能力。</small>
          </div>
          <div>
            <span>发布审核</span>
            <strong>{{ selectedDocument ? documentStatusLabel(selectedDocument) : '未记录' }}</strong>
            <small>仅从 metadata/status 或 Chunk 状态安全派生。</small>
          </div>
        </div>
      </SectionCard>
    </main>

    <aside class="knowledge-right">
      <SectionCard title="检索结果详情" subtitle="当前选中命中 Chunk" eyebrow="Retrieval Detail">
        <div v-if="selectedSearchResult" class="result-inspector">
          <div><span>查询</span><strong>{{ searchForm.query }}</strong></div>
          <div><span>命中 Chunk</span><strong class="mono">{{ chunkCode(selectedSearchResult) }}</strong></div>
          <div><span>来源文档</span><strong>{{ selectedSearchResult.documentTitle }}</strong></div>
          <div><span>相关度评分</span><strong class="mono score-cell">{{ formatScore(selectedSearchResult.score) }}</strong></div>
          <div><span>已用于生成</span><strong>{{ selectedReferenceUsage.length ? '是' : '否' }}</strong></div>
          <div><span>关联 Trace ID</span><strong class="mono">{{ selectedUsageRecord?.agentRun ? `Agent Run #${selectedUsageRecord.agentRun.id}` : '未关联 Trace' }}</strong></div>
          <div><span>首次使用时间</span><strong class="mono">{{ formatTime(firstUsedAt) }}</strong></div>
          <div><span>最近使用时间</span><strong class="mono">{{ formatTime(recentUsedAt) }}</strong></div>
          <div><span>使用次数</span><strong class="mono">{{ selectedReferenceUsage.length || '未记录' }}</strong></div>
          <CodeBlock title="引用预览" language="text" :code="selectedSearchResult.snippet || '未记录引用片段。'" />
        </div>
        <div v-else class="empty-state">运行关键词检索后查看命中 Chunk 详情。</div>
      </SectionCard>

      <SectionCard title="知识发布人工审核" subtitle="没有真实记录时保持空状态" eyebrow="Review">
        <div class="review-inspector">
          <div><span>审核状态</span><strong>暂无知识发布审核记录</strong></div>
          <div><span>审核人</span><strong>未记录</strong></div>
          <div><span>审核时间</span><strong class="mono">未记录</strong></div>
          <div><span>审核意见</span><strong>未记录</strong></div>
          <el-button class="wide-button" :disabled="!selectedUsageRecord" :icon="View" @click="viewReview">查看审核</el-button>
        </div>
      </SectionCard>
    </aside>

    <section class="knowledge-bottom">
      <SectionCard title="检索历史" subtitle="本页会话内真实检索记录" eyebrow="Search Log" compact>
        <div class="ledger-list">
          <article v-for="item in retrievalHistory" :key="item.id">
            <div>
              <strong>{{ item.query }}</strong>
              <small>{{ item.resultCount }} 个结果 / {{ item.scope }}</small>
            </div>
            <time class="mono">{{ formatDuration(item.durationMs) }}</time>
          </article>
          <div v-if="!retrievalHistory.length" class="empty-state">暂无检索历史。</div>
        </div>
      </SectionCard>

      <SectionCard title="引用历史" subtitle="来自 generation_knowledge_reference" eyebrow="References" compact>
        <div class="ledger-list">
          <article v-for="item in referenceHistoryRows.slice(0, 5)" :key="item.key">
            <div>
              <strong>{{ item.reference.documentTitle }}</strong>
              <small>{{ chunkCode(item.reference) }} / score {{ formatScore(item.reference.score) }}</small>
            </div>
            <time class="mono">{{ formatTime(item.record.updatedAt || item.record.createdAt) }}</time>
          </article>
          <div v-if="!referenceHistoryRows.length" class="empty-state">暂无生成引用记录。</div>
        </div>
      </SectionCard>

      <SectionCard title="生成知识引用" subtitle="按 Generation Record 聚合" eyebrow="Generation" compact>
        <div class="ledger-list">
          <article v-for="group in generationReferenceGroups.slice(0, 5)" :key="group.record.id">
            <div>
              <strong class="mono">run_{{ group.agentRun?.id || group.record.id }}</strong>
              <small>{{ group.references.length }} 条引用 / {{ statusText(group.record.status) }}</small>
            </div>
            <time class="mono">{{ formatTime(group.record.updatedAt || group.record.createdAt) }}</time>
          </article>
          <div v-if="!generationReferenceGroups.length" class="empty-state">暂无可展示的生成知识引用。</div>
        </div>
      </SectionCard>

      <SectionCard title="索引日志" subtitle="由文档与 Chunk 状态派生" eyebrow="Index" compact>
        <template #actions>
          <el-button :icon="RefreshRight" @click="loadPage">刷新</el-button>
        </template>
        <div class="ledger-list">
          <article v-for="item in indexLogRows" :key="item.key">
            <div>
              <strong>{{ item.document }}</strong>
              <small>{{ item.statusLabel }} / {{ item.chunkCount }} Chunks / 耗时 {{ item.duration }}</small>
            </div>
            <time class="mono">{{ formatTime(item.time) }}</time>
          </article>
          <div v-if="!indexLogRows.length" class="empty-state">暂无索引记录。</div>
        </div>
      </SectionCard>
    </section>
  </div>
</template>

<style scoped>
.knowledge-page {
  display: grid;
  grid-template-columns: minmax(280px, 0.82fr) minmax(500px, 1.55fr) minmax(330px, 0.92fr);
  grid-template-areas:
    "header header header"
    "metrics metrics metrics"
    "left center right"
    "bottom bottom bottom";
  gap: 12px;
  min-width: 0;
  align-items: start;
}

.knowledge-header {
  grid-area: header;
  min-width: 0;
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 16px;
  padding: 4px 2px 2px;
}

.knowledge-header h2,
.knowledge-header p {
  margin: 0;
}

.knowledge-header h2 {
  margin-top: 4px;
  color: var(--color-text-primary);
  font-size: 20px;
  font-weight: 680;
}

.knowledge-header p:not(.eyebrow) {
  max-width: 760px;
  margin-top: 6px;
  color: var(--color-text-secondary);
  font-size: 12px;
  line-height: 18px;
}

.pipeline-strip {
  min-width: 480px;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 1px;
  padding: 1px;
  border: var(--border-default);
  border-radius: var(--radius-card);
  background: var(--color-border-subtle);
}

.pipeline-strip span {
  position: relative;
  min-width: 0;
  padding: 9px 10px;
  background: var(--color-card-soft);
  color: var(--color-text-secondary);
  font-size: 11px;
  text-align: center;
}

.pipeline-strip span::before {
  content: "";
  width: 7px;
  height: 7px;
  display: inline-block;
  margin-right: 6px;
  border-radius: 2px;
  background: var(--color-accent);
  vertical-align: 1px;
}

.metric-strip {
  grid-area: metrics;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.knowledge-left {
  grid-area: left;
  min-width: 0;
}

.knowledge-center {
  grid-area: center;
  min-width: 0;
}

.knowledge-right {
  grid-area: right;
  min-width: 0;
  display: grid;
  gap: 12px;
}

.knowledge-bottom {
  grid-area: bottom;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  min-width: 0;
}

.document-tools {
  display: grid;
  gap: 8px;
  padding-bottom: 10px;
  border-bottom: var(--border-subtle);
}

.filter-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.document-list,
.ledger-list,
.timeline-list,
.hit-list {
  display: grid;
  min-width: 0;
}

.document-row {
  width: 100%;
  min-height: 66px;
  display: grid;
  grid-template-columns: 8px minmax(0, 1fr) auto;
  gap: 9px;
  align-items: center;
  padding: 10px 2px 10px 0;
  border: 0;
  border-bottom: var(--border-subtle);
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.document-row:hover,
.document-row.active {
  background: var(--color-active-row);
}

.document-row.active {
  box-shadow: inset 2px 0 var(--color-accent);
}

.doc-mark {
  width: 7px;
  height: 22px;
  border-radius: 2px;
  background: var(--color-accent);
}

.doc-main strong,
.doc-main small,
.ledger-list strong,
.ledger-list small,
.timeline-row strong,
.timeline-row small,
.timeline-row time,
.hit-row strong,
.hit-row small,
.result-inspector strong,
.review-inspector strong,
.document-overview strong,
.info-panel strong,
.access-grid strong,
.access-grid small {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-main strong,
.ledger-list strong,
.timeline-row strong,
.hit-row strong {
  color: var(--color-text-primary);
  font-size: 12px;
}

.doc-main small,
.ledger-list small,
.timeline-row small,
.hit-row small {
  margin-top: 4px;
  color: var(--color-text-disabled);
  font-size: 10px;
}

.document-overview {
  display: grid;
  grid-template-columns: minmax(92px, 0.6fr) minmax(108px, 0.7fr) minmax(92px, 0.55fr) minmax(132px, 0.75fr);
  gap: 1px;
  padding: 1px;
  margin-bottom: 12px;
  background: var(--color-border-subtle);
}

.document-overview div,
.info-panel,
.access-grid div {
  min-width: 0;
  padding: 10px;
  background: var(--color-bg);
}

.document-overview .wide,
.info-panel.wide {
  grid-column: 1 / -1;
}

.document-overview span,
.info-panel span,
.access-grid span,
.result-inspector span,
.review-inspector span,
.search-dock label span,
.create-form label span {
  display: block;
  color: var(--color-text-disabled);
  font-size: 10px;
}

.document-overview strong,
.info-panel strong,
.access-grid strong {
  margin-top: 5px;
  color: var(--color-text-primary);
  font-size: 12px;
}

.detail-tabs {
  height: 34px;
  display: flex;
  gap: 16px;
  border-bottom: var(--border-subtle);
  margin: 0 -12px 12px;
  padding: 0 12px;
}

.detail-tabs button {
  height: 34px;
  padding: 0;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: var(--color-text-disabled);
  font-size: 12px;
  cursor: pointer;
}

.detail-tabs button.active {
  border-bottom-color: var(--color-accent);
  color: var(--color-text-primary);
}

.search-dock {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 112px auto;
  gap: 8px;
  align-items: end;
  padding: 10px;
  margin-bottom: 12px;
  border: var(--border-subtle);
  border-radius: var(--radius-md);
  background: var(--color-card-soft);
}

.search-dock label {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.chunk-table {
  min-width: 0;
  overflow: hidden;
  border: var(--border-subtle);
  border-radius: var(--radius-md);
}

.chunk-table-head,
.chunk-row {
  display: grid;
  grid-template-columns: 86px minmax(0, 1.25fr) minmax(120px, 0.86fr) 66px 92px 66px;
  gap: 8px;
  align-items: center;
}

.chunk-table-head {
  min-height: 30px;
  padding: 0 10px;
  background: var(--color-card-soft);
  color: var(--color-text-disabled);
  font-size: 10px;
}

.chunk-row {
  min-height: 54px;
  padding: 8px 10px;
  border-top: var(--border-subtle);
}

.chunk-row > * {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chunk-row strong {
  color: var(--color-text-primary);
  font-size: 11px;
}

.chunk-row span {
  color: var(--color-text-secondary);
  font-size: 11px;
  line-height: 16px;
}

.chunk-row small {
  color: var(--color-text-disabled);
  font-size: 10px;
  white-space: nowrap;
}

.score-cell {
  color: var(--color-success) !important;
}

.hit-row {
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 72px;
  gap: 8px;
  padding: 10px;
  border-bottom: var(--border-subtle);
  cursor: pointer;
}

.hit-row:hover,
.hit-row.active {
  background: var(--color-active-row);
}

.hit-row p {
  grid-column: 1 / -1;
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 11px;
  line-height: 17px;
}

.hit-row > span {
  color: var(--color-success);
  font-size: 10px;
  text-align: right;
}

.timeline-row,
.ledger-list article {
  min-height: 44px;
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) 78px;
  gap: 8px;
  align-items: center;
  padding: 7px 0;
  border-bottom: var(--border-subtle);
}

.ledger-list article {
  grid-template-columns: minmax(0, 1fr) 76px;
}

.timeline-row:last-child,
.ledger-list article:last-child {
  border-bottom: 0;
}

.timeline-dot {
  width: 8px;
  height: 8px;
  border-radius: 2px;
  background: var(--color-text-disabled);
}

.timeline-dot[data-status="SUCCESS"],
.timeline-dot[data-status="CONFIRMED"],
.timeline-dot[data-status="SAVED"] {
  background: var(--color-success);
}

.timeline-dot[data-status="READY_FOR_REVIEW"],
.timeline-dot[data-status="PENDING"],
.timeline-dot[data-status="DRAFT"] {
  background: var(--color-warning);
}

.timeline-dot[data-status="FAILED"] {
  background: var(--color-error);
}

.timeline-dot[data-status="GENERATING"],
.timeline-dot[data-status="RUNNING"] {
  background: var(--color-running);
  animation: pulse 1.4s ease-in-out infinite;
}

.timeline-row time,
.ledger-list time {
  color: var(--color-text-disabled);
  font-size: 10px;
  text-align: right;
}

.document-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.document-code,
.create-form {
  grid-column: 1 / -1;
}

.create-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 9px;
  padding: 12px;
  border: var(--border-subtle);
  border-radius: var(--radius-md);
  background: var(--color-card-soft);
}

.create-form h3,
.create-form .wide,
.form-actions {
  grid-column: 1 / -1;
}

.create-form h3 {
  margin: 0;
  font-size: 13px;
}

.create-form label {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.access-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.access-grid small {
  margin-top: 6px;
  white-space: normal;
  color: var(--color-text-disabled);
  font-size: 10px;
  line-height: 16px;
}

.result-inspector,
.review-inspector {
  display: grid;
  gap: 9px;
}

.result-inspector > div,
.review-inspector > div {
  min-width: 0;
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}

.result-inspector strong,
.review-inspector strong {
  color: var(--color-text-primary);
  font-size: 11px;
}

.wide-button {
  width: 100%;
}

.knowledge-page :deep(.el-button:not(.el-button--primary)) {
  border-color: var(--color-border);
  background: var(--color-bg);
  color: var(--color-text-secondary);
}

.knowledge-page :deep(.el-button:not(.el-button--primary):hover) {
  border-color: var(--color-accent-border);
  background: var(--color-active-row);
  color: var(--color-text-primary);
}

.knowledge-page :deep(.el-button.is-disabled),
.knowledge-page :deep(.el-button.is-disabled:hover) {
  border-color: var(--color-border-subtle);
  background: var(--color-bg) !important;
  color: var(--color-text-disabled) !important;
  opacity: 0.7;
}

.knowledge-page :deep(.el-input-number__decrease),
.knowledge-page :deep(.el-input-number__increase) {
  border-color: var(--color-border);
  background: var(--color-card-soft);
  color: var(--color-text-secondary);
}

.knowledge-page :deep(.el-input-number__decrease:hover),
.knowledge-page :deep(.el-input-number__increase:hover) {
  color: var(--color-text-primary);
}

.empty-state {
  min-height: 62px;
  display: grid;
  place-items: center;
  padding: 10px;
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg);
  color: var(--color-text-disabled);
  text-align: center;
  font-size: 12px;
  line-height: 18px;
}

.knowledge-page > *,
.knowledge-header > *,
.metric-strip > *,
.document-row > *,
.document-overview > *,
.search-dock > *,
.chunk-table-head > *,
.chunk-row > *,
.hit-row > *,
.timeline-row > *,
.ledger-list article > *,
.result-inspector > *,
.review-inspector > *,
.knowledge-bottom > * {
  min-width: 0;
}

@media (max-width: 1360px) {
  .knowledge-page {
    grid-template-columns: minmax(260px, 0.78fr) minmax(0, 1.3fr);
    grid-template-areas:
      "header header"
      "metrics metrics"
      "left center"
      "right right"
      "bottom bottom";
  }

  .knowledge-right {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .knowledge-bottom {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .pipeline-strip {
    min-width: 360px;
  }
}

@media (max-width: 980px) {
  .knowledge-page,
  .metric-strip,
  .knowledge-right,
  .knowledge-bottom,
  .document-overview,
  .search-dock,
  .document-info-grid,
  .access-grid,
  .create-form {
    grid-template-columns: minmax(0, 1fr);
  }

  .knowledge-page {
    grid-template-areas:
      "header"
      "metrics"
      "left"
      "center"
      "right"
      "bottom";
  }

  .knowledge-header {
    align-items: stretch;
    flex-direction: column;
  }

  .pipeline-strip {
    min-width: 0;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .chunk-table-head {
    display: none;
  }

  .chunk-row {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (prefers-reduced-motion: reduce) {
  .timeline-dot[data-status="GENERATING"],
  .timeline-dot[data-status="RUNNING"] {
    animation: none;
  }
}
</style>
