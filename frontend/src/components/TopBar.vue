<script setup lang="ts">
import { DataLine } from '@element-plus/icons-vue'
defineProps<{ title: string }>()
</script>

<template>
  <header class="topbar">
    <div class="topbar-title" aria-label="DEVFLOW / 当前页面"><span class="mono">DEVFLOW</span><h1>{{ title }}</h1></div>
    <div class="topbar-actions"><span class="system-chip"><el-icon><DataLine /></el-icon>Local workspace</span></div>
  </header>
</template>
