<script setup lang="ts">
import { Connection, DataLine, Search } from '@element-plus/icons-vue'
import ProviderBadge from './ProviderBadge.vue'

defineProps<{
  title: string
}>()
</script>

<template>
  <header class="topbar">
    <div class="topbar-title">
      <span class="mono">DEVFLOW</span>
      <h1>{{ title }}</h1>
    </div>

    <div class="topbar-search" aria-label="全局搜索入口">
      <el-icon><Search /></el-icon>
      <span>搜索 Prompt、Trace、RAG、Tool Call...</span>
      <kbd class="mono">Ctrl K</kbd>
    </div>

    <div class="topbar-actions">
      <span class="system-chip">
        <el-icon><Connection /></el-icon>
        API 本地代理
      </span>
      <ProviderBadge provider="local-rule fallback" />
      <span class="system-chip accent">
        <el-icon><DataLine /></el-icon>
        Human Review
      </span>
    </div>
  </header>
</template>
