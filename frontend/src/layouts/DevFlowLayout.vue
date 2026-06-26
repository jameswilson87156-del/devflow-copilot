<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import SidebarNav from '@/components/SidebarNav.vue'
import TopBar from '@/components/TopBar.vue'
import { navItems } from '@/router'

const route = useRoute()

const currentTitle = computed(() => {
  const item = navItems.find((nav) => nav.name === route.name)
  return item?.label ?? 'DevFlow Copilot'
})
</script>

<template>
  <div class="shell">
    <SidebarNav />

    <section class="main-shell">
      <TopBar :title="currentTitle" />

      <main class="content">
        <RouterView />
      </main>
    </section>
  </div>
</template>
