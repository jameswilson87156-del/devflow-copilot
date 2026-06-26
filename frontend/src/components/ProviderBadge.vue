<script setup lang="ts">
const props = withDefaults(defineProps<{
  provider?: string
  model?: string
}>(), {
  provider: 'local-rule',
  model: '',
})

const isFallback = props.provider.toLowerCase().includes('fallback') || props.provider.toLowerCase().includes('local-rule')
</script>

<template>
  <span class="provider-badge" :data-fallback="isFallback">
    <span>{{ provider }}</span>
    <small v-if="model" class="mono">{{ model }}</small>
  </span>
</template>

<style scoped>
.provider-badge {
  max-width: 210px;
  min-width: 0;
  height: 28px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 9px;
  border: var(--border-default);
  border-radius: var(--radius-sm);
  background: var(--color-card-soft);
  color: var(--color-text-secondary);
  font-size: 10px;
  white-space: nowrap;
}

.provider-badge::before {
  content: "";
  width: 7px;
  height: 7px;
  border-radius: 2px;
  background: var(--color-running);
  flex: 0 0 auto;
}

.provider-badge[data-fallback="true"] {
  border-color: var(--color-success-border);
  background: var(--color-success-muted);
  color: var(--color-success);
}

.provider-badge span,
.provider-badge small {
  overflow: hidden;
  text-overflow: ellipsis;
}

.provider-badge small {
  color: var(--color-text-weak);
}
</style>
