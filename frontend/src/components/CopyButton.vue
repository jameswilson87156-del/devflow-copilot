<script setup lang="ts">
import { DocumentCopy } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  text: string
  label?: string
}>()

async function copy() {
  await navigator.clipboard.writeText(props.text || '')
  ElMessage.success('已复制')
}
</script>

<template>
  <el-button :icon="DocumentCopy" size="small" @click="copy">
    {{ label || '复制' }}
  </el-button>
</template>
