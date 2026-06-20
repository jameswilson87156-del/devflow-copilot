import { createRouter, createWebHistory } from 'vue-router'
import {
  Cpu,
  ChatLineSquare,
  Clock,
  DataBoard,
  Document,
  Files,
} from '@element-plus/icons-vue'
import DashboardView from '@/views/DashboardView.vue'
import WorkbenchView from '@/views/WorkbenchView.vue'
import LogAnalyzerView from '@/views/LogAnalyzerView.vue'
import PromptTemplatesView from '@/views/PromptTemplatesView.vue'
import GenerationHistoryView from '@/views/GenerationHistoryView.vue'

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
    path: '/prompts',
    name: 'PromptTemplates',
    label: 'Prompt 模板',
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
    { path: '/prompts', name: 'PromptTemplates', component: PromptTemplatesView },
    { path: '/history', name: 'GenerationHistory', component: GenerationHistoryView },
    { path: '/docs', redirect: '/' },
  ],
})

export default router
