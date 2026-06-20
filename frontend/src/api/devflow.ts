import { deleteData, getData, postData, putData } from './http'
import type {
  AiGenerateRequest,
  AiGenerateResponse,
  DashboardStats,
  GenerationRecord,
  LogAnalysis,
  LogAnalyzeResponse,
  ProjectContext,
  PromptTemplate,
} from '@/types/domain'

export function fetchDashboardStats() {
  return getData<DashboardStats>('/dashboard/stats')
}

export function fetchProjects() {
  return getData<ProjectContext[]>('/projects')
}

export function createProject(payload: Partial<ProjectContext>) {
  return postData<ProjectContext>('/projects', payload)
}

export function updateProject(id: number, payload: Partial<ProjectContext>) {
  return putData<ProjectContext>(`/projects/${id}`, payload)
}

export function deleteProject(id: number) {
  return deleteData<void>(`/projects/${id}`)
}

export function generateAi(type: string, payload: AiGenerateRequest) {
  const endpointMap: Record<string, string> = {
    'requirement-split': '/ai/requirement-split',
    'code-plan': '/ai/code-plan',
    'readme-generate': '/ai/readme-generate',
    'commit-message': '/ai/commit-message',
    'fix-prompt': '/ai/fix-prompt',
  }
  return postData<AiGenerateResponse>(endpointMap[type], payload)
}

export function analyzeLog(payload: { projectId?: number; rawLog: string }) {
  return postData<LogAnalyzeResponse>('/logs/analyze', payload)
}

export function fetchLogHistory(projectId?: number) {
  return getData<LogAnalysis[]>('/logs/history', projectId ? { projectId } : undefined)
}

export function fetchPrompts(templateType?: string) {
  return getData<PromptTemplate[]>('/prompts', templateType ? { templateType } : undefined)
}

export function createPrompt(payload: Partial<PromptTemplate>) {
  return postData<PromptTemplate>('/prompts', payload)
}

export function updatePrompt(id: number, payload: Partial<PromptTemplate>) {
  return putData<PromptTemplate>(`/prompts/${id}`, payload)
}

export function fetchGenerations(params?: { projectId?: number; generationType?: string }) {
  return getData<GenerationRecord[]>('/generations', params)
}

export function fetchGeneration(id: number) {
  return getData<GenerationRecord>(`/generations/${id}`)
}

export function confirmGeneration(id: number) {
  return postData<GenerationRecord>(`/generations/${id}/confirm`)
}

export function saveGeneration(id: number) {
  return postData<GenerationRecord>(`/generations/${id}/save`)
}
