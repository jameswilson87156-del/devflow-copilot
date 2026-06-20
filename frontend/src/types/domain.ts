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

export interface DashboardStats {
  projectCount: number
  todayGenerationCount: number
  logAnalysisCount: number
  promptTemplateCount: number
  recentGenerations: GenerationRecord[]
}

export interface AiGenerateRequest {
  projectId?: number
  input: string
  extraContext?: string
  templateId?: number
  variables?: Record<string, string>
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
