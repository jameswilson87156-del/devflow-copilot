<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  status: string
}>()

const normalized = computed(() => props.status.toLowerCase().replace(/\s+/g, '-'))
const statusLabel = computed(() => {
  const labels: Record<string, string> = {
    DRAFT: '草稿',
    GENERATING: '生成中',
    READY_FOR_REVIEW: '待人工确认',
    SAVED: '已保存',
    CONFIRMED: '已确认',
    FAILED: '失败',
    Draft: '草稿',
    Generating: '生成中',
    'Ready for Review': '待人工确认',
    Saved: '已保存',
    Confirmed: '已确认',
    Failed: '失败',
  }
  return labels[props.status] || props.status
})
</script>

<template>
  <span class="status-tag" :class="normalized">
    {{ statusLabel }}
  </span>
</template>

<style scoped>
.status-tag {
  height: 18px;
  display: inline-flex;
  align-items: center;
  padding: 0 var(--space-2);
  border-radius: var(--radius-sm);
  border: var(--border-default);
  background: var(--color-surface-raised);
  color: var(--color-text-secondary);
  font-size: var(--text-xs);
  line-height: 1;
  white-space: nowrap;
}

.status-tag::before {
  content: "";
  width: 6px;
  height: 6px;
  margin-right: var(--space-1);
  border-radius: 50%;
  background: var(--color-border);
}

.generating::before {
  background: var(--color-running);
  animation: pulse 1.2s ease-in-out infinite;
}

.ready_for_review::before,
.ready-for-review::before,
.draft::before {
  background: var(--color-running);
}

.saved::before,
.confirmed::before {
  background: var(--color-success);
}

.failed::before {
  width: 7px;
  height: 7px;
  border-radius: var(--radius-sm);
  background: var(--color-error);
}
</style>
