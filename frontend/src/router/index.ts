import { createRouter, createWebHistory } from 'vue-router'
import type { Component } from 'vue'
import {
  Cpu,
  ChatLineSquare,
  Clock,
  Connection,
  DataBoard,
  DataLine,
  Document,
  Files,
} from '@element-plus/icons-vue'

const DashboardView = () => import('@/views/DashboardView.vue')
const WorkbenchView = () => import('@/views/WorkbenchView.vue')
const LogAnalyzerView = () => import('@/views/LogAnalyzerView.vue')
const PromptTemplatesView = () => import('@/views/PromptTemplatesView.vue')
const GenerationHistoryView = () => import('@/views/GenerationHistoryView.vue')
const AgentRunTraceView = () => import('@/views/AgentRunTraceView.vue')
const KnowledgeBaseView = () => import('@/views/KnowledgeBaseView.vue')
const HumanReviewView = () => import('@/views/HumanReviewView.vue')

export interface NavItem {
  path?: string
  name: string
  label: string
  group: '工作流' | '可观测性' | '知识与引用' | '治理与审核' | '配置'
  icon: Component
  disabled?: boolean
  hint?: string
  hidden?: boolean
}

export const navItems: NavItem[] = [
  {
    path: '/',
    name: 'Dashboard',
    label: 'Dashboard',
    group: '工作流',
    icon: DataBoard,
  },
  {
    path: '/workbench',
    name: 'Workbench',
    label: 'Workbench',
    group: '工作流',
    icon: Cpu,
  },
  {
    path: '/prompts',
    name: 'PromptTemplates',
    label: 'Prompt Templates',
    group: '工作流',
    icon: ChatLineSquare,
  },
  {
    path: '/agent-runs',
    name: 'AgentRunTrace',
    label: 'Trace Evidence',
    group: '可观测性',
    icon: DataLine,
  },
  {
    path: '/history',
    name: 'GenerationHistory',
    label: 'Generation History',
    group: '可观测性',
    icon: Clock,
  },
  {
    path: '/logs',
    name: 'LogAnalyzer',
    label: 'Log Analyzer',
    group: '可观测性',
    icon: Document,
  },
  {
    name: 'ToolCalls',
    label: 'Tool Calls',
    group: '可观测性',
    icon: Cpu,
    disabled: true,
    hint: '当前在 Trace Evidence 页面查看 Tool Call 明细',
  },
  {
    path: '/knowledge',
    name: 'KnowledgeBase',
    label: 'Knowledge Base',
    group: '知识与引用',
    icon: Connection,
  },
  {
    path: '/reviews',
    name: 'HumanReview',
    label: 'Human Review',
    group: '治理与审核',
    icon: DataLine,
  },
  {
    name: 'Provider',
    label: 'Provider',
    group: '配置',
    icon: Connection,
    disabled: true,
    hint: '配置预留 / 当前通过环境变量配置',
  },
  {
    name: 'Settings',
    label: 'Settings',
    group: '配置',
    icon: Files,
    disabled: true,
    hint: '配置预留 / 当前通过环境变量配置',
  },
  {
    path: '/docs',
    name: 'DocsShortcut',
    label: 'Docs',
    group: '配置',
    icon: Files,
    hidden: true,
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'Dashboard', component: DashboardView },
    { path: '/workbench', name: 'Workbench', component: WorkbenchView },
    { path: '/logs', name: 'LogAnalyzer', component: LogAnalyzerView },
    { path: '/agent-runs', name: 'AgentRunTrace', component: AgentRunTraceView },
    { path: '/knowledge', name: 'KnowledgeBase', component: KnowledgeBaseView },
    { path: '/reviews', name: 'HumanReview', component: HumanReviewView },
    { path: '/prompts', name: 'PromptTemplates', component: PromptTemplatesView },
    { path: '/history', name: 'GenerationHistory', component: GenerationHistoryView },
    { path: '/docs', redirect: '/' },
  ],
})

export default router
