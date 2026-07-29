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
  displayTitle?: string
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
    label: '总览 Dashboard',
    displayTitle: '总览 Dashboard',
    group: '工作流',
    icon: DataBoard,
  },
  {
    path: '/workbench',
    name: 'Workbench',
    label: '工作台 Workbench',
    displayTitle: '工作台 Workbench',
    group: '工作流',
    icon: Cpu,
  },
  {
    path: '/prompts',
    name: 'PromptTemplates',
    label: 'Prompt 模板',
    displayTitle: 'Prompt 模板',
    group: '工作流',
    icon: ChatLineSquare,
  },
  {
    path: '/agent-runs',
    name: 'AgentRunTrace',
    label: '执行证据 Trace',
    displayTitle: '执行证据',
    group: '可观测性',
    icon: DataLine,
  },
  {
    path: '/history',
    name: 'GenerationHistory',
    label: '生成历史',
    displayTitle: '生成历史 Generation History',
    group: '可观测性',
    icon: Clock,
  },
  {
    path: '/logs',
    name: 'LogAnalyzer',
    label: '日志分析',
    displayTitle: '日志分析 Log Analyzer',
    group: '可观测性',
    icon: Document,
  },
  {
    path: '/knowledge',
    name: 'KnowledgeBase',
    label: '知识库',
    displayTitle: '知识库 Knowledge Base',
    group: '知识与引用',
    icon: Connection,
  },
  {
    name: 'KnowledgeReferences',
    label: '知识引用',
    group: '知识与引用',
    icon: Files,
    disabled: true,
    hint: '当前在 Trace Evidence 和 Knowledge Base 页面查看引用明细',
  },
  {
    path: '/reviews',
    name: 'HumanReview',
    label: '人工复核',
    displayTitle: '人工复核 Human Review',
    group: '治理与审核',
    icon: DataLine,
  },
  {
    name: 'Provider',
    label: 'Provider 配置',
    group: '配置',
    icon: Connection,
    disabled: true,
    hint: '配置预留 / 当前通过环境变量配置',
  },
  {
    name: 'Settings',
    label: '系统设置',
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
