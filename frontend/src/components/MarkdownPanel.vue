<script setup lang="ts">
import { computed } from 'vue'
import { DocumentCopy } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps<{
  content: string
  title?: string
  copyLabel?: string
}>()

const html = computed(() => renderMarkdown(props.content))

async function copyContent() {
  await navigator.clipboard.writeText(props.content || '')
  ElMessage.success('已复制输出内容')
}
</script>

<template>
  <section class="panel markdown-panel">
    <div class="panel-header">
      <div>
        <h3 class="panel-title">{{ title || 'AI Output' }}</h3>
        <p class="panel-subtitle">Markdown Artifact / Human Review</p>
      </div>
      <el-button :icon="DocumentCopy" size="small" @click="copyContent">{{ copyLabel || 'Copy Result' }}</el-button>
    </div>
    <div class="panel-body">
      <div v-if="content" class="markdown-body output" v-html="html"></div>
      <div v-else class="empty-state">
        <div>
          <p>暂无结构化输出</p>
          <span>运行工作流后在这里审查结果。</span>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.markdown-panel {
  min-height: 100%;
}

.output {
  max-height: calc(100vh - 225px);
  overflow: auto;
  padding-right: 4px;
}

.empty-state p {
  margin-bottom: 6px;
  color: var(--text);
  font-size: var(--text-sm);
}

.empty-state span {
  font-size: var(--text-xs);
}
</style>
