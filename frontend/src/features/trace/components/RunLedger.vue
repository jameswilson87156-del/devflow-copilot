<script setup lang="ts">
import type { AgentRun } from '@/types/domain'
import { statusLabel, statusTone } from '../runEvidenceViewModel'
defineProps<{ runs: AgentRun[]; selectedRunId?: number; loading?: boolean }>()
defineEmits<{ select: [id: number]; refresh: [] }>()
function formatTime(value?: string) { return value ? value.replace('T', ' ').slice(5, 16) : '未记录' }
</script>

<template>
  <aside class="run-ledger-pane workspace-pane" aria-label="运行记录">
    <header class="pane-toolbar"><div><p class="pane-kicker">RUNS</p><h2>运行记录</h2></div><button class="quiet-button" type="button" :disabled="loading" @click="$emit('refresh')">{{ loading ? '读取中' : '刷新' }}</button></header>
    <div v-if="runs.length" class="run-ledger-list"><button v-for="run in runs" :key="run.id" type="button" class="run-ledger-row" :class="{ selected: selectedRunId === run.id }" :aria-current="selectedRunId === run.id ? 'true' : undefined" @click="$emit('select', run.id)"><span class="run-ledger-marker" :data-tone="statusTone(run.status)" aria-hidden="true"></span><span class="run-ledger-copy"><strong>{{ run.title || '未命名 Run' }}</strong><small><span class="mono">run_{{ run.id }}</span><span>{{ run.providerName || 'Provider 未记录' }}</span></small></span><span class="run-ledger-state"><small>{{ statusLabel(run.status) }}</small><time class="mono">{{ formatTime(run.updatedAt || run.createdAt) }}</time></span></button></div>
    <div v-else class="workspace-empty compact"><strong>暂无运行记录</strong><span>请先在 Workbench 创建一次运行。</span></div>
    <footer class="ledger-boundary"><span>{{ runs.length }} runs</span><span>API · newest first</span></footer>
  </aside>
</template>
