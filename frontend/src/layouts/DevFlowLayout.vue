<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { Connection, DataLine } from '@element-plus/icons-vue'
import { navItems } from '@/router'

const route = useRoute()

const currentTitle = computed(() => {
  const item = navItems.find((nav) => nav.name === route.name)
  return item?.label ?? 'DevFlow Copilot'
})
</script>

<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark" aria-hidden="true"><span>DF</span></div>
        <div class="brand-copy">
          <strong>DevFlow</strong>
          <span>AI 编码总控台</span>
        </div>
      </div>

      <div class="workspace-label">
        <span class="mono">工作空间</span>
        <strong>DevFlow Copilot</strong>
        <small>本地规则引擎 / MVP</small>
      </div>

      <nav class="nav-stack">
        <RouterLink
          v-for="item in navItems.slice(0, 5)"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: route.name === item.name }"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar-status">
        <div class="pulse-dot"></div>
        <div>
          <strong>本地规则引擎</strong>
          <span>生成结果需人工确认</span>
        </div>
      </div>
    </aside>

    <section class="main-shell">
      <header class="topbar">
        <div class="topbar-title">
          <span class="mono">DEVFLOW</span>
          <h1>{{ currentTitle }}</h1>
        </div>
        <div class="topbar-actions">
          <span class="system-chip">
            <el-icon><Connection /></el-icon>
            API 本地代理
          </span>
          <span class="system-chip accent">
            <el-icon><DataLine /></el-icon>
            人工 Review
          </span>
        </div>
      </header>

      <main class="content">
        <RouterView />
      </main>
    </section>
  </div>
</template>
