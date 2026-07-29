<script setup lang="ts">
import { computed } from 'vue'
import { statusLabel, statusTone, type RunEvidenceViewModel } from '../runEvidenceViewModel'
const props = defineProps<{ model?: RunEvidenceViewModel; dataBoundary: string }>()
defineEmits<{ openInspector: [] }>()
const latency = computed(() => formatDuration(props.model?.run.latencyMs))
function formatDuration(value?: number) { return typeof value === 'number' ? (value >= 1000 ? `${(value / 1000).toFixed(2)}s` : `${value}ms`) : '未记录' }
</script>

<template>
  <header class="run-context-bar">
    <div class="run-context-title"><p class="workspace-kicker">运行记录</p><h1>{{ model?.run.title || '运行记录' }}</h1><div class="run-title-meta"><span class="mono">run_{{ model?.run.id ?? '—' }}</span><span class="evidence-status" :data-tone="model ? statusTone(model.run.status) : 'unavailable'"><i aria-hidden="true"></i>{{ model?.run.status ? statusLabel(model.run.status) : '未选择 Run' }}</span><span class="context-source-tag">{{ dataBoundary }}</span></div></div>
    <dl class="run-context-facts" aria-label="运行记录上下文"><div><dt>Provider</dt><dd class="mono" :data-availability="model?.provider.availability">{{ model?.provider.value || '未记录' }}</dd></div><div><dt>Model</dt><dd class="mono" :data-availability="model?.model.availability">{{ model?.model.value || '未记录' }}</dd></div><div><dt>Latency</dt><dd class="mono">{{ latency }}</dd></div><div><dt>Review</dt><dd>{{ model?.review.statusLabel || '未记录' }}</dd></div></dl>
    <button class="mobile-inspector-trigger" type="button" @click="$emit('openInspector')">查看上下文与决策</button>
  </header>
</template>
