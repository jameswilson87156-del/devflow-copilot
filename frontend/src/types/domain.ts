export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface ProjectContext {
  id: number
  projectName: string
  techStack: string
  readmeContent: string
  directoryStructure: string
  currentRequirement: string
  codingRules: string
  version: number
  createdAt: string
  updatedAt: string
}

export interface PromptTemplate {
  id: number
  templateKey: string
  templateName: string
  templateType: string
  templateContent: string
  variables: string
  enabled: boolean
  isDefault: boolean
  version: number
  createdAt: string
  updatedAt: string
}

export interface GenerationRecord {
  id: number
  projectId: number
  generationType: string
  inputSummary: string
  inputContent: string
  outputContent: string
  status: string
  confirmed: boolean
  providerName?: string
  modelName: string
  promptTemplateId?: number
  promptTemplateName?: string
  promptTemplateVersion?: number
  renderedPrompt?: string
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
  costTimeMs: number
  success: boolean
  errorMessage?: string
  version: number
  createdAt: string
  updatedAt?: string
}

export interface GenerationTrace {
  id: number
  generationRecordId: number
  promptVersion?: number
  inputVariables?: string
  renderedPromptSummary?: string
  providerName?: string
  modelName?: string
  status: string
  latencyMs?: number
  errorMessage?: string
  createdAt: string
}

export interface AgentRun {
  id: number
  projectId: number
  generationRecordId?: number
  title: string
  goal?: string
  status: string
  providerName?: string
  modelName?: string
  latencyMs?: number
  startedAt?: string
  completedAt?: string
  createdAt: string
  updatedAt?: string
}

export interface AgentStep {
  id: number
  runId: number
  stepOrder: number
  stepType: string
  stepName: string
  status: string
  summary?: string
  latencyMs?: number
  startedAt?: string
  completedAt?: string
}

export interface ToolCallRecord {
  id: number
  runId: number
  stepId?: number
  toolName: string
  inputSummary?: string
  outputSummary?: string
  status: string
  latencyMs?: number
  createdAt: string
}

export interface HumanReview {
  id: number
  runId: number
  generationRecordId: number
  reviewStatus: string
  reviewer?: string
  comment?: string
  createdAt: string
  updatedAt?: string
}

export interface AgentRunTrace {
  run: AgentRun
  steps: AgentStep[]
  toolCalls: ToolCallRecord[]
  humanReviews: HumanReview[]
}

export interface KnowledgeDocument {
  id: number
  title: string
  sourceType: string
  sourceUri?: string
  content: string
  chunkCount: number
  embeddingModel?: string
  metadata?: string
  createdAt: string
  updatedAt: string
}

export interface KnowledgeChunk {
  id: number
  documentId: number
  chunkIndex: number
  content: string
  contentSummary?: string
  keywords?: string
  embeddingModel?: string
  embeddingVector?: string
  createdAt: string
  updatedAt: string
}

export interface KnowledgeReference {
  documentId: number
  documentTitle: string
  chunkId: number
  chunkIndex: number
  score: number
  citationLabel: string
  snippet: string
}

export interface DashboardStats {
  projectCount: number
  todayGenerationCount: number
  logAnalysisCount: number
  promptTemplateCount: number
  agentRunCount: number
  humanReviewCount: number
  successCount: number
  successRate: number
  averageLatencyMs: number
  recentGenerations: GenerationRecord[]
}

export interface AiGenerateRequest {
  projectId?: number
  input: string
  extraContext?: string
  templateId?: number
  variables?: Record<string, string>
  knowledgeQuery?: string
  knowledgeDocumentIds?: number[]
}

export interface AiGenerateResponse {
  recordId: number
  generationType: string
  outputContent: string
  status: string
  providerName: string
  modelName: string
  costTimeMs: number
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
  promptTemplateId?: number
  promptTemplateName?: string
  promptTemplateVersion?: number
  errorMessage?: string
  agentRunId?: number
  knowledgeReferences?: KnowledgeReference[]
}

export interface LogAnalysis {
  id: number
  projectId: number
  rawLog: string
  exceptionType: string
  possibleReason: string
  diagnoseSteps: string
  fixPrompt: string
  riskTips: string
  riskLevel: string
  createdAt: string
}

export interface LogAnalyzeResponse {
  id: number
  recordId: number
  exceptionType: string
  possibleReason: string
  diagnoseSteps: string
  fixPrompt: string
  riskTips: string
  riskLevel: string
}
