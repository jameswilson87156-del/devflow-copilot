<script setup lang="ts">
import { computed } from 'vue'
import type { EvidenceEvent } from '../runEvidenceViewModel'

const props = defineProps<{ event: EvidenceEvent; selected: boolean }>()
defineEmits<{ select: [event: EvidenceEvent] }>()

const stateGlyph = computed(() => ({ success: '✓', running: '→', pending: '•', attention: '!', error: '×', unavailable: '–' })[props.event.tone])
const toolLabel = computed(() => props.event.toolCalls.length > 0 ? `Tool ${props.event.toolCalls.length}` : '')
function formatDuration(value?: number) { return typeof value === 'number' ? (value >= 1000 ? `${(value / 1000).toFixed(2)}s` : `${value}ms`) : '未记录' }
</script>

<template>
  <button :id="`evidence-event-${event.key}`" type="button" class="evidence-event-row" :class="{ selected }"
    :data-tone="event.tone" role="option" :aria-selected="selected" @click="$emit('select', event)">
    <span class="event-rail" aria-hidden="true"><span class="event-order mono">{{ String(event.order).padStart(2, '0') }}</span><span class="event-node">{{ stateGlyph }}</span></span>
    <span class="event-copy">
      <span class="event-type-line mono">{{ event.type }}</span>
      <strong>{{ event.name }}</strong>
      <span class="event-summary">{{ event.summary }}</span>
      <span v-if="toolLabel" class="event-tool-chip mono">{{ toolLabel }}</span>
    </span>
    <span class="event-state"><span class="evidence-status" :data-tone="event.tone"><i aria-hidden="true"></i>{{ event.statusLabel }}</span><span class="mono">{{ formatDuration(event.latencyMs) }}</span></span>
  </button>
</template>
