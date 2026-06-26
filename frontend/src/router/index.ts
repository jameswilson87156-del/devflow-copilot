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
    label: '仪表盘',
    group: '工作流',
    icon: DataBoard,
  },
  {
    path: '/workbench',
    name: 'Workbench',
    label: '工作台',
    group: '工作流',
    icon: Cpu,
  },
  {
    path: '/prompts',
    name: 'PromptTemplates',
    label: '提示词工作室',
    group: '工作流',
    icon: ChatLineSquare,
  },
  {
    path: '/logs',
    name: 'LogAnalyzer',
    label: '日志分析',
    group: '可观测性',
    icon: Document,
  },
  {
    path: '/agent-runs',
    name: 'AgentRunTrace',
    label: '智能体运行追踪',
    group: '可观测性',
    icon: DataLine,
  },
  {
    path: '/history',
    name: 'GenerationHistory',
    label: '生成追踪',
    group: '可观测性',
    icon: Clock,
  },
  {
    name: 'ToolCalls',
    label: '工具调用',
    group: '可观测性',
    icon: Cpu,
    disabled: true,
    hint: '当前在智能体运行追踪中查看 Tool Call',
  },
  {
    path: '/knowledge',
    name: 'KnowledgeBase',
    label: '知识库',
    group: '知识与引用',
    icon: Connection,
  },
  {
    name: 'HumanReview',
    label: '人工审核',
    group: '治理与审核',
    icon: DataLine,
    disabled: true,
    hint: '当前在生成追踪和智能体运行追踪中查看 Human Review',
  },
  {
    name: 'Provider',
    label: 'Provider',
    group: '配置',
    icon: Connection,
    disabled: true,
    hint: 'Provider 通过后端环境变量配置',
  },
  {
    name: 'Settings',
    label: '设置',
    group: '配置',
    icon: Files,
    disabled: true,
    hint: '配置页待接入',
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
    { path: '/prompts', name: 'PromptTemplates', component: PromptTemplatesView },
    { path: '/history', name: 'GenerationHistory', component: GenerationHistoryView },
    { path: '/docs', redirect: '/' },
  ],
})

export default router
