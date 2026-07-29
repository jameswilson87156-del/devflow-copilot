<script setup lang="ts">
import { computed, nextTick } from 'vue'
import type { EvidenceEvent } from '../runEvidenceViewModel'
import EvidenceEventRow from './EvidenceEventRow.vue'

const props = defineProps<{ events: EvidenceEvent[]; selectedKey: string; unlinkedCount: number; derivedAttention: string[] }>()
const emit = defineEmits<{ select: [event: EvidenceEvent]; openInspector: [] }>()
const selectedEvent = computed(() => props.events.find((event) => event.key === props.selectedKey))
function selectEvent(event: EvidenceEvent) { emit('select', event) }
function moveSelection(delta: number) {
  if (!props.events.length) return
  const index = Math.max(props.events.findIndex((event) => event.key === props.selectedKey), 0)
  const nextEvent = props.events[Math.min(Math.max(index + delta, 0), props.events.length - 1)]
  emit('select', nextEvent)
  nextTick(() => document.getElementById(`evidence-event-${nextEvent.key}`)?.focus())
}
function chooseCurrent() { if (selectedEvent.value) emit('select', selectedEvent.value) }
</script>

<template>
<section class="evidence-stream-pane workspace-pane" aria-label="执行证据" @keydown.up.prevent="moveSelection(-1)" @keydown.down.prevent="moveSelection(1)" @keydown.enter.prevent="chooseCurrent" @keydown.space.prevent="chooseCurrent">
  <header class="pane-toolbar evidence-stream-toolbar"><div><p class="pane-kicker">trace.steps</p><h2>执行证据</h2></div><div class="stream-counters"><span><b>{{ events.length }}</b> steps</span><span><b>{{ unlinkedCount }}</b> Run-level</span></div></header>
  <div v-if="events.length" class="evidence-event-list" role="listbox" aria-label="执行证据事件"><EvidenceEventRow v-for="event in events" :key="event.key" :event="event" :selected="selectedKey === event.key" @select="selectEvent" /></div>
  <div v-else class="workspace-empty"><strong>该 Run 没有 AgentStep</strong><span>仅展示实际 trace.steps，不补造固定流程。</span></div>
  <div v-if="derivedAttention.length" class="derived-attention" aria-label="证据提示"><span v-for="notice in derivedAttention" :key="notice">{{ notice }}</span></div>
  <section v-if="selectedEvent" class="mobile-selected-event"><p class="pane-kicker">Selected Event</p><strong>{{ selectedEvent.name }}</strong><span>{{ selectedEvent.summary }}</span><button type="button" class="primary-quiet-button" @click="$emit('openInspector')">查看详情</button></section>
  <footer class="stream-keyboard-hint"><span><kbd>↑</kbd><kbd>↓</kbd> 切换</span><span><kbd>Enter</kbd><kbd>Space</kbd> 选择</span><span>状态同时使用文字与形状表达</span></footer>
</section>
</template>
