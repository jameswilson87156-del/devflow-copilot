<script setup lang="ts">
type MetricTone = 'accent' | 'success' | 'warning' | 'danger' | 'running' | 'muted' | 'info'

withDefaults(defineProps<{
  label: string
  value: string | number
  code?: string
  tone?: MetricTone
}>(), {
  code: '',
  tone: 'accent',
})
</script>

<template>
  <article class="metric-card" :data-tone="tone">
    <span v-if="code" class="metric-card__code mono">{{ code }}</span>
    <strong class="metric-card__value">{{ value }}</strong>
    <small class="metric-card__label">{{ label }}</small>
  </article>
</template>

<style scoped>
.metric-card {
  min-width: 0;
  padding: var(--card-padding-sm);
  border: var(--border-default);
  border-radius: var(--radius-card);
  background:
    linear-gradient(135deg, rgba(124, 92, 255, 0.12), transparent 48%),
    var(--color-card);
  box-shadow: var(--shadow-card);
}

.metric-card__code,
.metric-card__value,
.metric-card__label {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-card__code {
  color: var(--color-text-weak);
  font-size: 9px;
  letter-spacing: 0.07em;
}

.metric-card__value {
  margin-top: 7px;
  color: var(--color-text-primary);
  font-family: var(--font-mono);
  font-size: 22px;
  line-height: 1;
}

.metric-card__label {
  margin-top: 5px;
  color: var(--color-text-secondary);
  font-size: 11px;
}

.metric-card[data-tone="success"] .metric-card__value {
  color: var(--color-success);
}

.metric-card[data-tone="warning"] .metric-card__value {
  color: var(--color-warning);
}

.metric-card[data-tone="danger"] .metric-card__value {
  color: var(--color-error);
}

.metric-card[data-tone="running"] .metric-card__value,
.metric-card[data-tone="info"] .metric-card__value {
  color: var(--color-running);
}

.metric-card[data-tone="muted"] .metric-card__value {
  color: var(--color-text-secondary);
}
</style>
