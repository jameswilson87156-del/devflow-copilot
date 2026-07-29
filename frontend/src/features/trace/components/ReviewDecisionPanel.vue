<script setup lang="ts">
import type { ReviewViewModel } from '../runEvidenceViewModel'

defineProps<{
  review: ReviewViewModel
  evidenceCount: number
  missingEvidenceCount: number
  busy?: boolean
}>()

defineEmits<{
  save: []
  confirm: []
}>()

function formatTimestamp(value: string) {
  if (value === '未记录') return value
  return value.replace('T', ' ').slice(0, 19)
}
</script>

<template>
<section class="decision-ledger">
  <div class="decision-current">
    <p class="pane-kicker">Decision Ledger</p>
    <span class="evidence-status" :data-tone="review.tone">
      <i aria-hidden="true"></i>{{ review.statusLabel }}
    </span>
  </div>

  <dl class="inspector-definition-list">
    <div>
      <dt>进入复核原因</dt>
      <dd :data-availability="review.reason.availability">
        <span>{{ review.reason.value }}</span>
        <small v-if="review.reason.source" class="mono">{{ review.reason.source }} · Direct</small>
      </dd>
    </div>
    <div>
      <dt>可用证据</dt>
      <dd>{{ evidenceCount }} 项 direct evidence</dd>
    </div>
    <div>
      <dt>缺失证据</dt>
      <dd>{{ missingEvidenceCount }} 项</dd>
    </div>
    <div>
      <dt>Reviewer</dt>
      <dd>{{ review.reviewer }}</dd>
    </div>
    <div>
      <dt>Comment</dt>
      <dd class="long-value">{{ review.comment }}</dd>
    </div>
    <div>
      <dt>Created</dt>
      <dd class="mono">{{ formatTimestamp(review.createdAt) }}</dd>
    </div>
    <div>
      <dt>Updated</dt>
      <dd class="mono">{{ formatTimestamp(review.updatedAt) }}</dd>
    </div>
  </dl>

  <div class="decision-actions">
    <button
      type="button"
      class="decision-action primary"
      :disabled="!review.canSave || busy"
      @click="$emit('save')"
    >
      保存记录
    </button>
    <button
      type="button"
      class="decision-action primary"
      :disabled="!review.canConfirm || busy"
      @click="$emit('confirm')"
    >
      最终确认
    </button>
    <button type="button" class="decision-action" disabled>Request Changes</button>
    <button type="button" class="decision-action danger" disabled>Reject</button>
  </div>

  <p class="write-boundary">{{ review.writeBoundary }}</p>
</section>
</template>
