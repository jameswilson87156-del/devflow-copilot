import type {
  AgentRunTrace,
  AgentStep,
  GenerationRecord,
  GenerationTrace,
  HumanReview,
  KnowledgeReference,
  ToolCallRecord,
} from '@/types/domain'

export type EvidenceAvailability = 'direct' | 'derived' | 'unavailable'
export type EvidenceTone = 'success' | 'running' | 'pending' | 'attention' | 'error' | 'unavailable'
export type EvidenceSource = 'AgentRun.providerName' | 'GenerationRecord.providerName' | 'GenerationTrace.providerName' | 'AgentRun.modelName' | 'GenerationRecord.modelName' | 'GenerationTrace.modelName'

export interface EvidenceValue {
  value: string
  availability: EvidenceAvailability
  source?: string
}

export interface EvidenceEvent {
  key: string
  order: number
  stepId: number
  type: string
  name: string
  status: string
  statusLabel: string
  tone: EvidenceTone
  summary: string
  latencyMs?: number
  startedAt?: string
  completedAt?: string
  toolCalls: ToolCallRecord[]
  provenance: EvidenceValue
  missingFields: string[]
}

export interface ReviewViewModel {
  record?: HumanReview
  status: string
  statusLabel: string
  tone: EvidenceTone
  availability: EvidenceAvailability
  reason: EvidenceValue
  reviewer: string
  comment: string
  createdAt: string
  updatedAt: string
  canSave: boolean
  canConfirm: boolean
  canRequestChanges: boolean
  canReject: boolean
  writeBoundary: string
}

export interface RunEvidenceViewModel {
  run: AgentRunTrace['run']
  record?: GenerationRecord
  generationTrace?: GenerationTrace
  events: EvidenceEvent[]
  toolCalls: ToolCallRecord[]
  unlinkedToolCalls: ToolCallRecord[]
  references: KnowledgeReference[]
  provider: EvidenceValue
  model: EvidenceValue
  errorEvidence: EvidenceValue
  fallbackEvidence: EvidenceValue
  routeEvidence: EvidenceValue
  generatedOutput: EvidenceValue
  review: ReviewViewModel
  derivedAttention: string[]
  knowledgeState: 'unused' | 'available' | 'evidence-gap'
  normalizedSnapshot: Record<string, unknown>
}

export interface RunEvidenceInput {
  trace: AgentRunTrace
  records?: GenerationRecord[]
  generationTraces?: GenerationTrace[]
  references?: KnowledgeReference[]
}

const missing = '未记录'

export function createRunEvidenceViewModel(input: RunEvidenceInput): RunEvidenceViewModel {
  const { trace } = input
  const records = input.records ?? []
  const generationTraces = input.generationTraces ?? []
  const references = input.references ?? []
  const record = records.find((item) => item.id === trace.run.generationRecordId)
  const generationTrace = [...generationTraces]
    .filter((item) => !record || item.generationRecordId === record.id)
    .sort((a, b) => b.id - a.id)[0]

  const provider = resolvedField([
    ['AgentRun.providerName', trace.run.providerName],
    ['GenerationRecord.providerName', record?.providerName],
    ['GenerationTrace.providerName', generationTrace?.providerName],
  ])
  const model = resolvedField([
    ['AgentRun.modelName', trace.run.modelName],
    ['GenerationRecord.modelName', record?.modelName],
    ['GenerationTrace.modelName', generationTrace?.modelName],
  ])
  const errorEvidence = evidenceValue(firstRecorded(record?.errorMessage, generationTrace?.errorMessage))
  const explicitFallback = explicitFallbackEvidence(trace.run.status, record?.status, generationTrace?.status)
  const fallbackEvidence = explicitFallback
    ? { value: explicitFallback, availability: 'direct' as const, source: 'explicit fallback status' }
    : { value: '未记录明确降级结果', availability: 'unavailable' as const }
  const routeEvidence = provider.value.toLowerCase().includes('local-rule')
    ? { value: '本地演示路由', availability: 'direct' as const, source: provider.source }
    : { value: '未记录路由信息', availability: 'unavailable' as const }

  const sortedSteps = [...(trace.steps ?? [])].sort((a, b) => {
    if (a.stepOrder !== b.stepOrder) return a.stepOrder - b.stepOrder
    return a.id - b.id
  })
  const stepIds = new Set(sortedSteps.map((step) => step.id))
  const toolCalls = [...(trace.toolCalls ?? [])]
  const events = sortedSteps.map((step) => toEvidenceEvent(step, toolCalls))
  const unlinkedToolCalls = toolCalls.filter((tool) => !tool.stepId || !stepIds.has(tool.stepId))
  const review = createReviewViewModel(trace.humanReviews ?? [], sortedSteps, record)
  const hasKnowledgeStep = events.some((event) => normalizeStepType(event.type) === 'KNOWLEDGE_RETRIEVAL')
  const knowledgeState = references.length ? 'available' : hasKnowledgeStep ? 'evidence-gap' : 'unused'
  const derivedAttention = createDerivedAttention(events, record, generationTrace, knowledgeState)
  const generatedOutput = evidenceValue(firstRecorded(record?.outputContent))

  const normalizedSnapshot: Record<string, unknown> = {
    snapshotType: 'Normalized Snapshot',
    provenance: 'Derived / frontend DTO normalization',
    run: trace.run,
    steps: sortedSteps,
    toolCalls,
    unlinkedToolCalls,
    humanReviews: trace.humanReviews ?? [],
    generationRecord: record ?? null,
    generationTrace: generationTrace ?? null,
    knowledgeReferences: references,
    resolvedFields: {
      provider: { value: provider.value, source: provider.source ?? null },
      model: { value: model.value, source: model.source ?? null },
      errorEvidence: errorEvidence.availability === 'direct' ? errorEvidence.value : null,
      fallbackEvidence: fallbackEvidence.availability === 'direct' ? fallbackEvidence.value : null,
      routeEvidence: routeEvidence.availability === 'direct' ? routeEvidence.value : null,
    },
  }

  return {
    run: trace.run,
    record,
    generationTrace,
    events,
    toolCalls,
    unlinkedToolCalls,
    references,
    provider,
    model,
    errorEvidence,
    fallbackEvidence,
    routeEvidence,
    generatedOutput,
    review,
    derivedAttention,
    knowledgeState,
    normalizedSnapshot,
  }
}

function resolvedField(candidates: Array<[EvidenceSource, string | undefined]>): EvidenceValue {
  const candidate = candidates.find(([, value]) => Boolean(firstRecorded(value)))
  return candidate
    ? { value: firstRecorded(candidate[1])!, availability: 'direct', source: candidate[0] }
    : { value: missing, availability: 'unavailable' }
}

function explicitFallbackEvidence(...statuses: Array<string | undefined>) {
  const fallbackStatus = statuses.find((status) => normalizeStatus(status) === 'FALLBACK')
  return fallbackStatus ? `状态记录为 ${fallbackStatus}` : undefined
}

function toEvidenceEvent(step: AgentStep, toolCalls: ToolCallRecord[]): EvidenceEvent {
  const missingFields: string[] = []
  if (!firstRecorded(step.summary)) missingFields.push('summary')
  if (typeof step.latencyMs !== 'number') missingFields.push('latencyMs')
  if (!firstRecorded(step.startedAt)) missingFields.push('startedAt')
  if (!firstRecorded(step.completedAt)) missingFields.push('completedAt')

  return {
    key: `step-${step.id}`,
    order: step.stepOrder,
    stepId: step.id,
    type: firstRecorded(step.stepType) ?? missing,
    name: firstRecorded(step.stepName) ?? missing,
    status: normalizeStatus(step.status),
    statusLabel: statusLabel(step.status),
    tone: statusTone(step.status),
    summary: firstRecorded(step.summary) ?? missing,
    latencyMs: step.latencyMs,
    startedAt: step.startedAt,
    completedAt: step.completedAt,
    toolCalls: toolCalls.filter((tool) => tool.stepId === step.id),
    provenance: { value: 'trace.steps', availability: 'direct', source: 'AgentStep' },
    missingFields,
  }
}

function createReviewViewModel(reviews: HumanReview[], steps: AgentStep[], record?: GenerationRecord): ReviewViewModel {
  const review = [...reviews].sort((a, b) => timestamp(b.updatedAt ?? b.createdAt) - timestamp(a.updatedAt ?? a.createdAt))[0]
  const reviewStep = [...steps]
    .filter((step) => normalizeStepType(step.stepType) === 'HUMAN_REVIEW' && Boolean(firstRecorded(step.summary)))
    .sort((a, b) => a.stepOrder - b.stepOrder || a.id - b.id)[0]
  const reason: EvidenceValue = reviewStep
    ? { value: firstRecorded(reviewStep.summary)!, availability: 'direct', source: 'AgentStep.summary' }
    : { value: missing, availability: 'unavailable' }
  if (!review) {
    return {
      status: 'UNAVAILABLE', statusLabel: '未进入人工复核', tone: 'unavailable', availability: 'unavailable',
      reason,
      reviewer: missing, comment: missing, createdAt: missing, updatedAt: missing,
      canSave: false, canConfirm: false, canRequestChanges: false, canReject: false,
      writeBoundary: '当前 Run 未返回 humanReviews；不会推断复核已经完成。',
    }
  }
  const recordStatus = normalizeStatus(record?.status)
  return {
    record: review, status: normalizeStatus(review.reviewStatus), statusLabel: reviewStatusLabel(review.reviewStatus),
    tone: statusTone(review.reviewStatus), availability: 'direct', reason, reviewer: firstRecorded(review.reviewer) ?? missing,
    comment: firstRecorded(review.comment) ?? missing, createdAt: firstRecorded(review.createdAt) ?? missing,
    updatedAt: firstRecorded(review.updatedAt) ?? missing, canSave: recordStatus === 'READY_FOR_REVIEW',
    canConfirm: recordStatus === 'SAVED', canRequestChanges: false, canReject: false,
    writeBoundary: 'Save 与 Confirm 仅在现有 Generation API 状态允许时可用；Request Changes / Reject 当前没有写接口。',
  }
}

function createDerivedAttention(events: EvidenceEvent[], record: GenerationRecord | undefined, generationTrace: GenerationTrace | undefined, knowledgeState: RunEvidenceViewModel['knowledgeState']) {
  const notices: string[] = []
  const failedCount = events.filter((event) => event.tone === 'error').length
  if (failedCount) notices.push(`证据提示：${failedCount} 个步骤返回失败状态`)
  if (!record) notices.push('证据提示：缺少 Generation Record')
  if (!generationTrace) notices.push('证据提示：缺少 Generation Trace')
  if (knowledgeState === 'evidence-gap') notices.push('证据缺口：知识检索步骤未返回引用')
  return notices
}

export function normalizeStatus(status?: string) {
  if (!status) return 'UNAVAILABLE'
  const aliases: Record<string, string> = { Draft: 'DRAFT', Generating: 'GENERATING', 'Ready for Review': 'READY_FOR_REVIEW', Saved: 'SAVED', Confirmed: 'CONFIRMED', Failed: 'FAILED' }
  return aliases[status] ?? status.toUpperCase().replace(/\s+/g, '_').replace(/-/g, '_')
}

export function statusLabel(status?: string) {
  const labels: Record<string, string> = {
    DRAFT: '草稿', GENERATING: '运行中', RUNNING: '运行中', READY_FOR_REVIEW: '待人工复核', WAITING_REVIEW: '待人工复核',
    PENDING: '待处理', WAITING: '等待中', SAVED: '已保存', CONFIRMED: '已确认', SUCCESS: '成功', PASSED: '成功',
    FAILED: '失败', ERROR: '错误', REJECTED: '已驳回', SKIPPED: '未执行', FALLBACK: '已降级', UNAVAILABLE: '未记录',
  }
  return labels[normalizeStatus(status)] ?? firstRecorded(status) ?? missing
}

export function reviewStatusLabel(status?: string) {
  const normalized = normalizeStatus(status)
  if (['PENDING', 'READY_FOR_REVIEW', 'WAITING_REVIEW'].includes(normalized)) return '等待人工复核'
  if (normalized === 'CONFIRMED') return '复核已确认'
  if (normalized === 'REJECTED') return '复核已驳回'
  if (normalized === 'SAVED') return '复核内容已保存'
  return statusLabel(status)
}

export function statusTone(status?: string): EvidenceTone {
  const normalized = normalizeStatus(status)
  if (['CONFIRMED', 'SAVED', 'SUCCESS', 'PASSED'].includes(normalized)) return 'success'
  if (['FAILED', 'ERROR', 'REJECTED'].includes(normalized)) return 'error'
  if (['GENERATING', 'RUNNING'].includes(normalized)) return 'running'
  if (['READY_FOR_REVIEW', 'WAITING_REVIEW', 'PENDING', 'WAITING', 'DRAFT'].includes(normalized)) return 'pending'
  if (normalized === 'FALLBACK') return 'attention'
  return 'unavailable'
}

function evidenceValue(value?: string): EvidenceValue {
  const recorded = firstRecorded(value)
  return recorded ? { value: recorded, availability: 'direct' } : { value: missing, availability: 'unavailable' }
}

function normalizeStepType(value: string) { return value.toUpperCase().replace(/\s+/g, '_').replace(/-/g, '_') }
function firstRecorded(...values: Array<string | undefined | null>) { return values.find((value): value is string => typeof value === 'string' && value.trim().length > 0)?.trim() }
function timestamp(value?: string) { const result = value ? new Date(value).getTime() : 0; return Number.isNaN(result) ? 0 : result }
