<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { navItems, type NavItem } from '@/router'

const route = useRoute()

const groups = computed(() => {
  const map = new Map<NavItem['group'], NavItem[]>()
  for (const item of navItems) {
    if (item.hidden) continue
    const groupItems = map.get(item.group) ?? []
    groupItems.push(item)
    map.set(item.group, groupItems)
  }
  return [...map.entries()].map(([label, items]) => ({ label, items }))
})
</script>

<template>
  <aside class="sidebar" aria-label="DevFlow 主导航">
    <div class="brand">
      <div class="brand-mark" aria-hidden="true"><span>DF</span></div>
      <div class="brand-copy">
        <strong>DevFlow</strong>
        <span>AI Coding Workbench</span>
      </div>
    </div>

    <div class="workspace-label">
      <span class="mono">当前工作空间</span>
      <strong>DevFlow Copilot</strong>
      <small>local-rule fallback / MVP</small>
    </div>

    <nav class="nav-stack">
      <section v-for="group in groups" :key="group.label" class="nav-group">
        <p class="nav-group-label">{{ group.label }}</p>
        <template v-for="item in group.items" :key="item.name">
          <RouterLink
            v-if="item.path && !item.disabled"
            :to="item.path"
            class="nav-item"
            :class="{ active: route.name === item.name }"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </RouterLink>
          <button v-else class="nav-item disabled" type="button" :title="item.hint || '待接入'" disabled>
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </button>
        </template>
      </section>
    </nav>

    <div class="sidebar-status">
      <div class="pulse-dot"></div>
      <div>
        <strong>本地规则引擎</strong>
        <span>生成结果需人工确认</span>
      </div>
    </div>
  </aside>
</template>
