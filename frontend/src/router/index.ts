import { createRouter, createWebHistory } from 'vue-router'
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

export const navItems = [
  {
    path: '/',
    name: 'Dashboard',
    label: '工作台概览',
    icon: DataBoard,
  },
  {
    path: '/workbench',
    name: 'Workbench',
    label: 'AI 工作台',
    icon: Cpu,
  },
  {
    path: '/logs',
    name: 'LogAnalyzer',
    label: '日志分析',
    icon: Document,
  },
  {
    path: '/agent-runs',
    name: 'AgentRunTrace',
    label: 'Agent Run Trace',
    icon: DataLine,
  },
  {
    path: '/knowledge',
    name: 'KnowledgeBase',
    label: 'Knowledge Base',
    icon: Connection,
  },
  {
    path: '/prompts',
    name: 'PromptTemplates',
    label: 'Prompt Studio',
    icon: ChatLineSquare,
  },
  {
    path: '/history',
    name: 'GenerationHistory',
    label: '生成历史',
    icon: Clock,
  },
  {
    path: '/docs',
    name: 'DocsShortcut',
    label: 'Docs',
    icon: Files,
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
