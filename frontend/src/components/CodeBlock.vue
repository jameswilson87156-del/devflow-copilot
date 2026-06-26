<script setup lang="ts">
withDefaults(defineProps<{
  code: string
  language?: string
  title?: string
}>(), {
  language: 'text',
  title: '',
})
</script>

<template>
  <figure class="code-block">
    <figcaption v-if="title || language">
      <span>{{ title || 'Code' }}</span>
      <small class="mono">{{ language }}</small>
    </figcaption>
    <pre><code>{{ code }}</code></pre>
  </figure>
</template>

<style scoped>
.code-block {
  min-width: 0;
  margin: 0;
  border: var(--border-default);
  border-radius: var(--radius-card);
  background: var(--color-code-bg);
  overflow: hidden;
}

.code-block figcaption {
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  padding: 0 var(--space-3);
  border-bottom: var(--border-subtle);
  color: var(--color-text-secondary);
  font-size: 11px;
}

.code-block pre {
  margin: 0;
  padding: var(--space-3);
  overflow: auto;
  color: var(--color-text-primary);
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: var(--leading-code);
}
</style>
