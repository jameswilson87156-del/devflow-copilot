<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, shallowRef, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import {
  confirmGeneration,
  fetchAgentRunTrace,
  fetchAgentRuns,
  fetchGenerationTraces,
  fetchGenerations,
  fetchKnowledgeReferences,
  fetchProjects,
  saveGeneration,
} from '@/api/devflow'
import EvidenceInspector from '@/features/trace/components/EvidenceInspector.vue'
import EvidenceStream from '@/features/trace/components/EvidenceStream.vue'
import RunEvidenceHeader from '@/features/trace/components/RunEvidenceHeader.vue'
import RunLedger from '@/features/trace/components/RunLedger.vue'
import { createRunEvidenceViewModel, statusLabel, type EvidenceEvent } from '@/features/trace/runEvidenceViewModel'
import type { AgentRun, AgentRunTrace, GenerationRecord, GenerationTrace, KnowledgeReference } from '@/types/domain'

const route = useRoute()
const runs = shallowRef<AgentRun[]>([])
const records = shallowRef<GenerationRecord[]>([])
const generationTraces = shallowRef<GenerationTrace[]>([])
const references = shallowRef<KnowledgeReference[]>([])
const trace = shallowRef<AgentRunTrace>()
const selectedProjectId = shallowRef<number>()
const selectedRunId = shallowRef<number>()
const selectedEventKey = shallowRef('')
const loading = shallowRef(false)
const actionBusy = shallowRef(false)
const loadError = shallowRef('')
const isDrawer = shallowRef(false)
const drawerOpen = shallowRef(false)
const desktopInspectorVisible = shallowRef(true)
const viewportInitialized = shallowRef(false)
const lastFocusedElement = shallowRef<HTMLElement>()
const inspector = shallowRef<InstanceType<typeof EvidenceInspector>>()

const model = computed(() => trace.value ? createRunEvidenceViewModel({ trace: trace.value, records: records.value, generationTraces: generationTraces.value, references: references.value }) : undefined)
const selectedEvent = computed(() => model.value?.events.find((event) => event.key === selectedEventKey.value) ?? model.value?.events[0])
const selectedRun = computed(() => runs.value.find((run) => run.id === selectedRunId.value))
const selectedRunValue = computed({
  get: () => selectedRunId.value ? String(selectedRunId.value) : '',
  set: (value: string) => {
    const id = Number(value)
    if (id) void selectRun(id)
  },
})
const inspectorVisible = computed(() => isDrawer.value ? drawerOpen.value : desktopInspectorVisible.value)
const backgroundInert = computed(() => isDrawer.value && drawerOpen.value)
const dataBoundary = computed(() => model.value?.routeEvidence.availability === 'direct' ? model.value.routeEvidence.value : 'API 数据来源')

async function loadPageData() {
  loading.value = true
  loadError.value = ''
  try {
    const projects = await fetchProjects()
    selectedProjectId.value = projects.find((project) => project.projectName === 'DevFlow Copilot')?.id ?? projects[0]?.id
    await loadRuns()
  } catch (error) {
    loadError.value = errorMessage(error)
  } finally {
    loading.value = false
  }
}

async function loadRuns() {
  loadError.value = ''
  try {
    const queryGenerationId = Number(route.query.generationRecordId) || undefined
    const [recordRows, runRows] = await Promise.all([
      fetchGenerations(),
      fetchAgentRuns({ projectId: selectedProjectId.value }),
    ])
    records.value = recordRows
    runs.value = [...runRows].sort((a, b) => b.id - a.id)
    const queryRun = queryGenerationId ? runs.value.find((run) => run.generationRecordId === queryGenerationId) : undefined
    const previousRun = runs.value.find((run) => run.id === selectedRunId.value)
    selectedRunId.value = queryRun?.id ?? previousRun?.id ?? runs.value[0]?.id
    await loadTrace()
  } catch (error) {
    loadError.value = errorMessage(error)
  }
}

async function loadTrace() {
  if (!selectedRunId.value) {
    trace.value = undefined
    generationTraces.value = []
    references.value = []
    selectedEventKey.value = ''
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const traceRow = await fetchAgentRunTrace(selectedRunId.value)
    const recordId = traceRow.run.generationRecordId
    const [generationRows, referenceRows] = await Promise.all([
      recordId ? fetchGenerationTraces({ generationRecordId: recordId }).catch(() => [] as GenerationTrace[]) : Promise.resolve([]),
      recordId ? fetchKnowledgeReferences(recordId).catch(() => [] as KnowledgeReference[]) : Promise.resolve([]),
    ])
    trace.value = traceRow
    generationTraces.value = generationRows
    references.value = referenceRows
    const currentStillExists = model.value?.events.some((event) => event.key === selectedEventKey.value)
    if (!currentStillExists) selectedEventKey.value = model.value?.events[0]?.key ?? ''
  } catch (error) {
    loadError.value = errorMessage(error)
  } finally {
    loading.value = false
  }
}

async function selectRun(id: number) {
  if (selectedRunId.value === id) return
  selectedRunId.value = id
  selectedEventKey.value = ''
  await loadTrace()
}

function selectEvent(event: EvidenceEvent) {
  selectedEventKey.value = event.key
}

function openInspector() {
  lastFocusedElement.value = document.activeElement instanceof HTMLElement ? document.activeElement : undefined
  if (isDrawer.value) drawerOpen.value = true
  else desktopInspectorVisible.value = true
  nextTick(() => inspector.value?.focus())
}

function closeInspector() {
  if (!isDrawer.value) return
  drawerOpen.value = false
  nextTick(() => {
    const fallback = document.getElementById(`evidence-event-${selectedEvent.value?.key}`)
    ;(lastFocusedElement.value?.isConnected ? lastFocusedElement.value : fallback)?.focus()
  })
}

async function saveReviewRecord() {
  const recordId = model.value?.record?.id
  if (!recordId) return
  actionBusy.value = true
  try {
    await saveGeneration(recordId)
    ElMessage.success('Generation Record 已保存')
    await loadRuns()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    actionBusy.value = false
  }
}

async function confirmReview() {
  const recordId = model.value?.record?.id
  if (!recordId) return
  actionBusy.value = true
  try {
    await confirmGeneration(recordId)
    ElMessage.success('Generation Record 已确认')
    await loadRuns()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    actionBusy.value = false
  }
}

function updateViewport() {
  isDrawer.value = window.matchMedia('(max-width: 1180px)').matches
  if (!viewportInitialized.value) {
    drawerOpen.value = false
    desktopInspectorVisible.value = true
    viewportInitialized.value = true
  }
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && isDrawer.value && drawerOpen.value) {
    event.preventDefault()
    closeInspector()
  }
}

function applyShellInert(active: boolean) {
  for (const selector of ['.sidebar', '.topbar']) {
    const element = document.querySelector<HTMLElement>(selector)
    if (!element) continue
    if (active) {
      element.setAttribute('inert', '')
      element.setAttribute('aria-hidden', 'true')
    } else {
      element.removeAttribute('inert')
      element.removeAttribute('aria-hidden')
    }
  }
  document.body.classList.toggle('drawer-lock', active)
}

function formatRunTime(value?: string) {
  return value ? value.replace('T', ' ').slice(5, 16) : '未记录'
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : '加载运行记录失败'
}

onMounted(() => {
  updateViewport()
  window.addEventListener('resize', updateViewport)
  window.addEventListener('keydown', handleKeydown)
  watch(backgroundInert, applyShellInert, { immediate: true })
  loadPageData()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewport)
  window.removeEventListener('keydown', handleKeydown)
  applyShellInert(false)
})
</script>

<template>
  <div class="evidence-workspace">
    <RunEvidenceHeader :inert="backgroundInert ? true : undefined" :model="model" :data-boundary="dataBoundary" @open-inspector="openInspector" />

    <section v-if="runs.length || selectedRun" class="mobile-run-selector" :inert="backgroundInert ? true : undefined" aria-label="移动端 Run 切换">
      <div>
        <p class="pane-kicker">RUN SELECTOR</p>
        <strong>{{ selectedRun?.title || '未选择 Run' }}</strong>
        <span>{{ selectedRun ? `${statusLabel(selectedRun.status)} · ${formatRunTime(selectedRun.updatedAt || selectedRun.createdAt)}` : '暂无运行记录' }}</span>
      </div>
      <select v-if="runs.length > 1" v-model="selectedRunValue" aria-label="切换运行记录">
        <option v-for="run in runs" :key="run.id" :value="String(run.id)">
          run_{{ run.id }} · {{ run.title || '未命名 Run' }}
        </option>
      </select>
    </section>

    <div v-if="loading" class="workspace-loading" role="status"><span aria-hidden="true"></span>正在读取 Agent Run Trace…</div>

    <section v-if="loadError" class="workspace-state workspace-error" role="alert">
      <div><p class="pane-kicker">Trace detail unavailable</p><h2>证据读取失败</h2><p>{{ loadError }}</p></div>
      <button type="button" class="primary-quiet-button" @click="loadPageData">重试</button>
    </section>

    <main v-else-if="model" class="evidence-workspace-grid">
      <RunLedger :inert="backgroundInert ? true : undefined" :runs="runs" :selected-run-id="selectedRunId" :loading="loading" @select="selectRun" @refresh="loadRuns" />
      <EvidenceStream :inert="backgroundInert ? true : undefined" :events="model.events" :selected-key="selectedEvent?.key || ''" :unlinked-count="model.unlinkedToolCalls.length" :derived-attention="model.derivedAttention" @select="selectEvent" @open-inspector="openInspector" />
      <EvidenceInspector ref="inspector" :model="model" :selected-event="selectedEvent" :open="inspectorVisible" :drawer="isDrawer" :busy="actionBusy" @close="closeInspector" @save="saveReviewRecord" @confirm="confirmReview" />
    </main>

    <section v-else-if="!loading" class="workspace-state workspace-empty-page">
      <div><p class="pane-kicker">No Agent Run</p><h2>暂无可展示的运行记录</h2><p>页面不会生成固定七步。请先在 Workbench 创建一次真实 Run。</p></div>
      <button type="button" class="primary-quiet-button" @click="loadRuns">刷新运行记录</button>
    </section>
  </div>
</template>
