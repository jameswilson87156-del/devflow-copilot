<script setup lang="ts">
import { computed, nextTick, ref, type ComponentPublicInstance } from 'vue'
import type { EvidenceEvent, RunEvidenceViewModel } from '../runEvidenceViewModel'
import ReviewDecisionPanel from './ReviewDecisionPanel.vue'

type InspectorTab = 'context' | 'evidence' | 'tools' | 'snapshot' | 'review'

const props = defineProps<{
  model: RunEvidenceViewModel
  selectedEvent?: EvidenceEvent
  open: boolean
  drawer: boolean
  busy?: boolean
}>()

const emit = defineEmits<{ close: []; save: []; confirm: [] }>()
const activeTab = ref<InspectorTab>('context')
const copied = ref(false)
const inspectorRef = ref<HTMLElement>()
const tabRefs = ref<HTMLButtonElement[]>([])
const snapshot = computed(() => JSON.stringify(props.model.normalizedSnapshot, null, 2))
const evidenceCount = computed(() => Number(Boolean(props.model.record)) + Number(Boolean(props.model.generationTrace)) + props.model.references.length + props.model.toolCalls.length)
const missingEvidenceCount = computed(() => Number(!props.model.record) + Number(!props.model.generationTrace) + Number(props.model.knowledgeState === 'evidence-gap'))
const selectedToolCount = computed(() => props.selectedEvent?.toolCalls.length ?? 0)
const runLevelEvidenceCount = computed(() => props.model.unlinkedToolCalls.length)
const dataCompleteness = computed(() => props.selectedEvent?.missingFields.length ? `未记录字段：${props.selectedEvent.missingFields.join('、')}` : '数据完整')
const tabs: Array<{ key: InspectorTab; label: string; technical: string }> = [
  { key: 'context', label: '上下文', technical: 'Context' },
  { key: 'evidence', label: '证据', technical: 'Evidence' },
  { key: 'tools', label: '工具调用', technical: 'Tools' },
  { key: 'snapshot', label: '数据快照', technical: 'Snapshot' },
  { key: 'review', label: '人工复核', technical: 'Review' },
]

defineExpose({
  focus: () => inspectorRef.value?.focus(),
  activate: (tab: InspectorTab) => { activeTab.value = tab },
})

async function copySnapshot() {
  await navigator.clipboard.writeText(snapshot.value)
  copied.value = true
  window.setTimeout(() => { copied.value = false }, 1200)
}

function formatDuration(value?: number) {
  return typeof value === 'number' ? (value >= 1000 ? `${(value / 1000).toFixed(2)}s` : `${value}ms`) : '未记录'
}

function formatTimestamp(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '未记录'
}

function selectTab(key: InspectorTab) {
  activeTab.value = key
}

function setTabRef(element: Element | ComponentPublicInstance | null) {
  if (!(element instanceof HTMLButtonElement)) return
  const id = element.id
  const existingIndex = tabRefs.value.findIndex((item) => item.id === id)
  if (existingIndex >= 0) tabRefs.value[existingIndex] = element
  else tabRefs.value.push(element)
}

function focusTab(index: number) {
  const ordered = tabs.map((tab) => tabRefs.value.find((element) => element.id === `inspector-tab-${tab.key}`)).filter(Boolean) as HTMLButtonElement[]
  const target = ordered[index]
  target?.focus()
}

function handleTabKeydown(event: KeyboardEvent, index: number) {
  if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return
  event.preventDefault()
  let nextIndex = index
  if (event.key === 'ArrowLeft') nextIndex = index === 0 ? tabs.length - 1 : index - 1
  if (event.key === 'ArrowRight') nextIndex = index === tabs.length - 1 ? 0 : index + 1
  if (event.key === 'Home') nextIndex = 0
  if (event.key === 'End') nextIndex = tabs.length - 1
  activeTab.value = tabs[nextIndex].key
  nextTick(() => focusTab(nextIndex))
}

function focusableElements() {
  if (!inspectorRef.value) return []
  const selector = 'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
  return [...inspectorRef.value.querySelectorAll<HTMLElement>(selector)].filter((element) => element.offsetParent !== null || element === document.activeElement)
}

function handleInspectorKeydown(event: KeyboardEvent) {
  if (!props.drawer || event.key !== 'Tab') return
  const focusables = focusableElements()
  if (!focusables.length) {
    event.preventDefault()
    inspectorRef.value?.focus()
    return
  }
  const first = focusables[0]
  const last = focusables[focusables.length - 1]
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}
</script>

<template>
  <div v-if="drawer && open" class="inspector-backdrop" aria-hidden="true" @click="emit('close')"></div>
  <aside
    ref="inspectorRef"
    class="inspector-pane workspace-pane"
    :class="{ open, 'is-drawer': drawer }"
    :aria-hidden="!open ? 'true' : undefined"
    :aria-modal="drawer && open ? 'true' : undefined"
    :role="drawer ? 'dialog' : 'complementary'"
    aria-label="上下文与决策"
    tabindex="-1"
    @keydown="handleInspectorKeydown"
  >
    <header class="inspector-header">
      <div>
        <p class="pane-kicker">RUN EVIDENCE</p>
        <h2>上下文与决策</h2>
      </div>
      <button v-if="drawer" type="button" class="inspector-close" aria-label="关闭上下文与决策" @click="emit('close')">×</button>
    </header>

    <nav class="inspector-tabs" aria-label="上下文与决策页签" role="tablist">
      <button
        v-for="(tab, index) in tabs"
        :id="`inspector-tab-${tab.key}`"
        :key="tab.key"
        :ref="setTabRef"
        type="button"
        :class="{ active: activeTab === tab.key }"
        role="tab"
        :tabindex="activeTab === tab.key ? 0 : -1"
        :aria-selected="activeTab === tab.key"
        :aria-controls="`inspector-panel-${tab.key}`"
        @click="selectTab(tab.key)"
        @keydown="handleTabKeydown($event, index)"
      >
        <span>{{ tab.label }}</span>
        <small>{{ tab.technical }}</small>
      </button>
    </nav>

    <div class="inspector-content">
      <section v-if="activeTab === 'context'" id="inspector-panel-context" class="inspector-section" role="tabpanel" aria-labelledby="inspector-tab-context">
        <div class="inspector-selection">
          <span class="mono">{{ selectedEvent ? String(selectedEvent.order).padStart(2, '0') : '—' }}</span>
          <div>
            <p class="pane-kicker">{{ selectedEvent?.type || 'No selection' }}</p>
            <h3>{{ selectedEvent?.name || '未选择事件' }}</h3>
          </div>
        </div>
        <p class="inspector-summary">{{ selectedEvent?.summary || '请选择一个实际 AgentStep。' }}</p>

        <section class="evidence-brief" aria-label="紧凑证据摘要">
          <div><span>关联 Tool</span><strong>{{ selectedToolCount }}</strong></div>
          <div><span>Run-level Evidence</span><strong>{{ runLevelEvidenceCount }}</strong></div>
          <div><span>数据完整性</span><strong>{{ dataCompleteness }}</strong></div>
          <div><span>Review</span><strong>{{ model.review.statusLabel }}</strong></div>
        </section>

        <div class="linked-evidence-list">
          <h4>Linked Evidence</h4>
          <ul>
            <li v-for="tool in selectedEvent?.toolCalls || []" :key="tool.id">
              <strong>{{ tool.toolName }}</strong>
              <span>{{ tool.outputSummary || tool.inputSummary || '未记录' }}</span>
            </li>
            <li v-if="!selectedEvent?.toolCalls.length"><strong>Tool Call</strong><span>当前步骤没有关联 Tool Call。</span></li>
            <li v-if="model.references.length"><strong>Knowledge Reference</strong><span>{{ model.references.length }} 条引用可用。</span></li>
            <li v-else><strong>Knowledge Reference</strong><span>{{ model.knowledgeState === 'unused' ? '本次 Run 未使用知识引用。' : '证据缺口：知识检索步骤未返回引用。' }}</span></li>
          </ul>
        </div>

        <dl class="inspector-definition-list">
          <div><dt>状态</dt><dd><span v-if="selectedEvent" class="evidence-status" :data-tone="selectedEvent.tone"><i aria-hidden="true"></i>{{ selectedEvent.statusLabel }}</span><span v-else>未记录</span></dd></div>
          <div><dt>完整时间</dt><dd class="mono">{{ formatTimestamp(selectedEvent?.startedAt) }} → {{ formatTimestamp(selectedEvent?.completedAt) }}</dd></div>
          <div><dt>Provenance</dt><dd class="mono">{{ selectedEvent?.provenance.value || '未记录' }}</dd></div>
          <div><dt>Missing Fields</dt><dd>{{ dataCompleteness }}</dd></div>
          <div><dt>Latency</dt><dd class="mono">{{ formatDuration(selectedEvent?.latencyMs) }}</dd></div>
          <div><dt>Provider</dt><dd><span class="mono">{{ model.provider.value }}</span><small>{{ model.provider.source || 'Unavailable' }} · {{ model.provider.availability }}</small></dd></div>
          <div><dt>Model</dt><dd><span class="mono">{{ model.model.value }}</span><small>{{ model.model.source || 'Unavailable' }} · {{ model.model.availability }}</small></dd></div>
        </dl>

        <div class="context-jump-list" aria-label="快速进入">
          <button type="button" @click="selectTab('evidence')">前往证据</button>
          <button type="button" @click="selectTab('tools')">前往工具</button>
          <button type="button" @click="selectTab('snapshot')">前往数据快照</button>
          <button type="button" @click="selectTab('review')">前往人工复核</button>
        </div>
        <div v-if="model.derivedAttention.length" class="attention-list"><span v-for="item in model.derivedAttention" :key="item">{{ item }}</span></div>
      </section>

      <section v-else-if="activeTab === 'evidence'" id="inspector-panel-evidence" class="inspector-section evidence-stack" role="tabpanel" aria-labelledby="inspector-tab-evidence">
        <article class="linked-evidence-card"><header><strong>Generation Record</strong><span :data-availability="model.record ? 'direct' : 'unavailable'">{{ model.record ? 'Direct' : 'Unavailable' }}</span></header><p>{{ model.record?.inputSummary || '未记录' }}</p><dl><div><dt>Status</dt><dd>{{ model.record?.status || '未记录' }}</dd></div><div><dt>Provider</dt><dd class="mono">{{ model.provider.value }}</dd></div><div><dt>Model</dt><dd class="mono">{{ model.model.value }}</dd></div></dl></article>
        <article v-if="model.errorEvidence.availability === 'direct'" class="linked-evidence-card"><header><strong>Error Evidence</strong><span>Direct</span></header><p>{{ model.errorEvidence.value }}</p></article>
        <article v-if="model.fallbackEvidence.availability === 'direct'" class="linked-evidence-card"><header><strong>Fallback Evidence</strong><span>Direct</span></header><p>{{ model.fallbackEvidence.value }}</p></article>
        <article v-if="model.routeEvidence.availability === 'direct'" class="linked-evidence-card"><header><strong>Route Evidence</strong><span>Direct</span></header><p>{{ model.routeEvidence.value }}</p></article>
        <article class="linked-evidence-card"><header><strong>Generated Output</strong><span :data-availability="model.generatedOutput.availability">{{ model.generatedOutput.availability }}</span></header><p class="generated-output">{{ model.generatedOutput.value }}</p></article>
        <article class="linked-evidence-card"><header><strong>Knowledge Reference</strong><span>{{ model.references.length }} direct</span></header><ul v-if="model.references.length" class="reference-list"><li v-for="reference in model.references" :key="reference.chunkId"><strong>{{ reference.citationLabel }} · {{ reference.documentTitle }}</strong><span>{{ reference.snippet }}</span></li></ul><p v-else>{{ model.knowledgeState === 'unused' ? '本次 Run 未使用知识引用。' : '证据缺口：知识检索步骤未返回引用。' }}</p></article>
      </section>

      <section v-else-if="activeTab === 'tools'" id="inspector-panel-tools" class="inspector-section evidence-stack" role="tabpanel" aria-labelledby="inspector-tab-tools">
        <div><p class="pane-kicker">Selected Step / exact stepId match</p><article v-for="tool in selectedEvent?.toolCalls || []" :key="tool.id" class="tool-call-card"><header><strong>{{ tool.toolName }}</strong><span>{{ tool.status }}</span></header><dl><div><dt>Input</dt><dd>{{ tool.inputSummary || '未记录' }}</dd></div><div><dt>Output</dt><dd>{{ tool.outputSummary || '未记录' }}</dd></div><div><dt>Latency</dt><dd class="mono">{{ formatDuration(tool.latencyMs) }}</dd></div><div><dt>Step ID</dt><dd class="mono">{{ tool.stepId ?? '未记录' }}</dd></div></dl></article><p v-if="!selectedEvent?.toolCalls.length" class="inline-empty">当前步骤没有关联 Tool Call。</p></div>
        <div><p class="pane-kicker">Run-level Linked Evidence</p><article v-for="tool in model.unlinkedToolCalls" :key="tool.id" class="tool-call-card unlinked"><header><strong>{{ tool.toolName }}</strong><span>Unlinked</span></header><p>{{ tool.outputSummary || tool.inputSummary || '未记录' }}</p></article><p v-if="!model.unlinkedToolCalls.length" class="inline-empty">没有 Run-level 未关联 Tool Call。</p></div>
      </section>

      <section v-else-if="activeTab === 'snapshot'" id="inspector-panel-snapshot" class="inspector-section snapshot-section" role="tabpanel" aria-labelledby="inspector-tab-snapshot">
        <div class="snapshot-toolbar"><span class="pane-kicker">API 字段 / Normalized View Model</span><button type="button" @click="copySnapshot">{{ copied ? '已复制' : '复制 JSON' }}</button></div>
        <pre>{{ snapshot }}</pre>
      </section>

      <section v-else id="inspector-panel-review" class="inspector-section" role="tabpanel" aria-labelledby="inspector-tab-review">
        <ReviewDecisionPanel :review="model.review" :evidence-count="evidenceCount" :missing-evidence-count="missingEvidenceCount" :busy="busy" @save="$emit('save')" @confirm="$emit('confirm')" />
      </section>
    </div>
  </aside>
</template>
