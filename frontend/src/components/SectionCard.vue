<script setup lang="ts">
withDefaults(defineProps<{
  title?: string
  subtitle?: string
  eyebrow?: string
  compact?: boolean
}>(), {
  title: '',
  subtitle: '',
  eyebrow: '',
  compact: false,
})
</script>

<template>
  <section class="section-card" :class="{ compact }">
    <header v-if="title || subtitle || eyebrow || $slots.actions" class="section-card__header">
      <div class="section-card__title">
        <span v-if="eyebrow" class="section-card__eyebrow mono">{{ eyebrow }}</span>
        <h3 v-if="title">{{ title }}</h3>
        <p v-if="subtitle">{{ subtitle }}</p>
      </div>
      <div v-if="$slots.actions" class="section-card__actions">
        <slot name="actions" />
      </div>
    </header>
    <div class="section-card__body">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.section-card {
  min-width: 0;
  border: var(--border-default);
  border-radius: var(--radius-card);
  background: var(--color-card);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.section-card__header {
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-2) var(--card-padding);
  border-bottom: var(--border-subtle);
  background: var(--color-card-soft);
}

.section-card__title {
  min-width: 0;
}

.section-card__title h3,
.section-card__title p,
.section-card__eyebrow {
  display: block;
  min-width: 0;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-card__title h3 {
  color: var(--color-text-primary);
  font-size: 13px;
  font-weight: 650;
}

.section-card__title p,
.section-card__eyebrow {
  color: var(--color-text-weak);
  font-size: 10px;
}

.section-card__eyebrow {
  margin-bottom: 2px;
  letter-spacing: 0.06em;
}

.section-card__actions {
  flex: 0 0 auto;
}

.section-card__body {
  min-width: 0;
  padding: var(--card-padding);
}

.section-card.compact .section-card__body {
  padding: var(--space-2);
}
</style>
