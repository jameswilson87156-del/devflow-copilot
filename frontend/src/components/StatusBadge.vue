<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  status: string
  label?: string
}>()

const normalized = computed(() => props.status.toUpperCase().replace(/\s+/g, '_').replace(/-/g, '_'))
const tone = computed(() => {
  if (['CONFIRMED', 'SAVED', 'SUCCESS', 'PASSED'].includes(normalized.value)) return 'success'
  if (['FAILED', 'REJECTED', 'ERROR'].includes(normalized.value)) return 'danger'
  if (['GENERATING', 'RUNNING'].includes(normalized.value)) return 'running'
  if (['READY_FOR_REVIEW', 'PENDING', 'WAITING_REVIEW', 'DRAFT'].includes(normalized.value)) return 'warning'
  return 'muted'
})
const displayLabel = computed(() => {
  const labels: Record<string, string> = {
    DRAFT: '草稿',
    GENERATING: '运行中',
    RUNNING: '运行中',
    READY_FOR_REVIEW: '待审核',
    WAITING_REVIEW: '待审核',
    PENDING: '待审核',
    SAVED: '已保存',
    CONFIRMED: '已确认',
    SUCCESS: '成功',
    FAILED: '失败',
    REJECTED: '已驳回',
  }
  return props.label || labels[normalized.value] || props.status
})
</script>

<template>
  <span class="status-badge" :data-tone="tone">
    <i aria-hidden="true"></i>
    {{ displayLabel }}
  </span>
</template>

<style scoped>
.status-badge {
  max-width: 100%;
  height: 22px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 8px;
  border: 1px solid var(--status-border, var(--color-border));
  border-radius: var(--radius-sm);
  background: var(--status-bg, var(--color-card-soft));
  color: var(--status-fg, var(--color-text-secondary));
  font-size: 11px;
  line-height: 1;
  white-space: nowrap;
}

.status-badge i {
  width: 6px;
  height: 6px;
  border-radius: 2px;
  background: currentColor;
  flex: 0 0 auto;
}

.status-badge[data-tone="success"] {
  --status-bg: var(--color-success-muted);
  --status-border: var(--color-success-border);
  --status-fg: var(--color-success);
}

.status-badge[data-tone="warning"] {
  --status-bg: var(--color-warning-muted);
  --status-border: var(--color-warning-border);
  --status-fg: var(--color-warning);
}

.status-badge[data-tone="danger"] {
  --status-bg: var(--color-error-muted);
  --status-border: var(--color-error-border);
  --status-fg: var(--color-error);
}

.status-badge[data-tone="running"] {
  --status-bg: var(--color-running-muted);
  --status-border: var(--color-running-border);
  --status-fg: var(--color-running);
}
</style>
