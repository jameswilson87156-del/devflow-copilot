<script setup lang="ts">
import { computed, onMounted, reactive, shallowRef } from 'vue'
import { CopyDocument, Search, Warning } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { analyzeLog, fetchLogHistory, fetchProjects } from '@/api/devflow'
import CopyButton from '@/components/CopyButton.vue'
import type { LogAnalysis, LogAnalyzeResponse, ProjectContext } from '@/types/domain'

const projects = shallowRef<ProjectContext[]>([])
const history = shallowRef<LogAnalysis[]>([])
const selectedProjectId = shallowRef<number>()
const loading = shallowRef(false)
const result = shallowRef<LogAnalyzeResponse>()
const searchKeyword = shallowRef('')
const levelFilter = shallowRef('ALL')
const realtime = shallowRef(false)

const form = reactive({
  rawLog: `2026-06-20 10:18:12 INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat initialized with port(s): 8080 (http)
2026-06-20 10:18:12 INFO  c.power.plant.PowerPlantApplication - Starting PowerPlantApplication using Java 17
2026-06-20 10:18:13 INFO  o.s.b.w.s.c.ServletWebServerApplicationContext - Root WebApplicationContext initialized
2026-06-20 10:18:13 WARN  o.s.b.w.s.c.AnnotationConfigServletWebServerApplicationContext - Exception encountered during context initialization
2026-06-20 10:18:13 ERROR o.s.boot.SpringApplication - Application run failed
org.springframework.boot.web.server.PortInUseException: Port 8080 is already in use
    at org.springframework.boot.web.server.PortInUseException.lambda$throwIfPortBindingException$0(PortInUseException.java:70)
    at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.start(TomcatWebServer.java:250)
    at org.springframework.boot.web.servlet.context.WebServerStartStopLifecycle.start(WebServerStartStopLifecycle.java:44)
    at org.springframework.context.support.DefaultLifecycleProcessor.doStart(DefaultLifecycleProcessor.java:179)
    at org.springframework.context.support.DefaultLifecycleProcessor$LifecycleGroup.start(DefaultLifecycleProcessor.java:357)
    at org.springframework.context.support.DefaultLifecycleProcessor.startBeans(DefaultLifecycleProcessor.java:156)
    at org.springframework.context.support.DefaultLifecycleProcessor.onRefresh(DefaultLifecycleProcessor.java:124)
    at org.springframework.context.support.AbstractApplicationContext.finishRefresh(AbstractApplicationContext.java:946)
    at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:594)
    at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:146)
Caused by: java.net.BindException: Address already in use: bind
    at java.base/sun.nio.ch.Net.bind0(Native Method)
    at java.base/sun.nio.ch.Net.bind(Net.java:555)
    at java.base/sun.nio.ch.ServerSocketChannelImpl.netBind(ServerSocketChannelImpl.java:337)
    at java.base/sun.nio.ch.ServerSocketChannelImpl.bind(ServerSocketChannelImpl.java:294)
    at org.apache.tomcat.util.net.NioEndpoint.initServerSocket(NioEndpoint.java:247)
    at org.apache.tomcat.util.net.NioEndpoint.bind(NioEndpoint.java:202)
    at org.apache.tomcat.util.net.AbstractEndpoint.bindWithCleanup(AbstractEndpoint.java:1317)
    at org.apache.tomcat.util.net.AbstractEndpoint.start(AbstractEndpoint.java:1403)
2026-06-20 10:18:13 INFO  o.s.b.a.l.ConditionEvaluationReportLogger - Error starting ApplicationContext
2026-06-20 10:18:13 INFO  o.s.b.d.LoggingFailureAnalysisReporter - ***************************
2026-06-20 10:18:13 INFO  o.s.b.d.LoggingFailureAnalysisReporter - APPLICATION FAILED TO START
2026-06-20 10:18:13 INFO  o.s.b.d.LoggingFailureAnalysisReporter - ***************************
2026-06-20 10:18:13 ERROR o.s.b.d.LoggingFailureAnalysisReporter - Web server failed to start. Port 8080 was already in use.
2026-06-20 10:18:13 ERROR o.s.b.d.LoggingFailureAnalysisReporter - Action: Identify and stop the process listening on port 8080 or configure this application to listen on another port.`,
})

const logExamples = [
  {
    label: 'NullPointerException 示例',
    value: `java.lang.NullPointerException: Cannot invoke "com.demo.User.getName()" because "user" is null
    at com.demo.service.UserService.buildProfile(UserService.java:42)
    at com.demo.controller.UserController.profile(UserController.java:27)`,
  },
  {
    label: 'BeanCreationException 示例',
    value: `org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'alarmService'
Caused by: org.springframework.beans.factory.NoSuchBeanDefinitionException: No qualifying bean of type 'com.power.plant.mapper.AlarmMapper' available`,
  },
  {
    label: 'Port already in use 示例',
    value: `***************************
APPLICATION FAILED TO START
***************************

Web server failed to start. Port 8080 was already in use.`,
  },
  {
    label: 'SQLSyntaxErrorException 示例',
    value: `java.sql.SQLSyntaxErrorException: Unknown column 'review_status' in 'field list'
    at com.mysql.cj.jdbc.exceptions.SQLError.createSQLException(SQLError.java:120)
    at org.mybatis.spring.MyBatisSystemException`,
  },
]

const levelOptions = ['ALL', 'ERROR', 'WARN', 'INFO', 'DEBUG']

const logRows = computed(() => {
  return form.rawLog
    .split(/\r?\n/)
    .map((line, index) => {
      const normalized = line.trimEnd()
      const level = detectLevel(normalized)
      return {
        id: `${index}-${normalized.slice(0, 12)}`,
        timestamp: detectTimestamp(normalized) || `line ${String(index + 1).padStart(3, '0')}`,
        level,
        source: detectSource(normalized),
        message: normalized || ' ',
        cost: detectCost(normalized),
      }
    })
    .filter((row) => levelFilter.value === 'ALL' || row.level === levelFilter.value)
    .filter((row) => !searchKeyword.value || row.message.toLowerCase().includes(searchKeyword.value.toLowerCase()))
})

const matchCount = computed(() => {
  if (!searchKeyword.value) return 0
  return form.rawLog.toLowerCase().split(searchKeyword.value.toLowerCase()).length - 1
})

function detectLevel(line: string) {
  if (/ERROR|Exception|Caused by|FAILED TO START/i.test(line)) return 'ERROR'
  if (/WARN|warning/i.test(line)) return 'WARN'
  if (/DEBUG/i.test(line)) return 'DEBUG'
  return 'INFO'
}

function detectTimestamp(line: string) {
  return line.match(/\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}:\d{2}/)?.[0] || line.match(/\d{2}:\d{2}:\d{2}/)?.[0]
}

function detectSource(line: string) {
  const atMatch = line.match(/at\s+([\w.]+)\(/)
  if (atMatch?.[1]) return atMatch[1].split('.').slice(-2).join('.')
  const beanMatch = line.match(/org\.springframework\.([\w.]+)/)
  if (beanMatch?.[1]) return `spring.${beanMatch[1].split('.')[0]}`
  return 'application'
}

function detectCost(line: string) {
  return line.match(/\b\d+ms\b/)?.[0] || '--'
}

async function loadPageData() {
  const projectData = await fetchProjects()
  projects.value = projectData
  selectedProjectId.value = projectData.find((project) => project.projectName === 'power-plant-system')?.id || projectData[0]?.id
  history.value = await fetchLogHistory(selectedProjectId.value)
}

async function runAnalyze() {
  if (!form.rawLog.trim()) {
    ElMessage.warning('请先粘贴 Java / Spring Boot 报错日志')
    return
  }
  loading.value = true
  try {
    result.value = await analyzeLog({
      projectId: selectedProjectId.value,
      rawLog: form.rawLog,
    })
    history.value = await fetchLogHistory(selectedProjectId.value)
    ElMessage.success('日志分析完成')
  } finally {
    loading.value = false
  }
}

function fillExample(value: string) {
  form.rawLog = value
}

async function copyFixPrompt(text: string) {
  await navigator.clipboard.writeText(text)
  ElMessage.success('修复 Prompt 已复制')
}

onMounted(loadPageData)
</script>

<template>
  <div class="log-page">
    <section class="panel input-panel">
      <div class="log-toolbar">
        <div>
          <h3 class="panel-title">日志分析工作台</h3>
          <p class="panel-subtitle">Java / Spring Boot 异常诊断、根因定位与修复 Prompt</p>
        </div>
        <div class="toolbar-controls">
          <el-select v-model="selectedProjectId" placeholder="选择项目" class="project-select">
            <el-option v-for="project in projects" :key="project.id" :label="project.projectName" :value="project.id" />
          </el-select>
          <el-input v-model="searchKeyword" :prefix-icon="Search" placeholder="搜索日志" class="search-input">
            <template #suffix>
              <span class="match-count">{{ matchCount }}</span>
            </template>
          </el-input>
          <el-switch v-model="realtime" active-text="实时" inactive-text="静态" />
        </div>
      </div>
      <div class="panel-body input-body">
        <div class="level-chips">
          <button
            v-for="level in levelOptions"
            :key="level"
            :class="{ active: levelFilter === level }"
            @click="levelFilter = level"
          >
            {{ level }}
          </button>
        </div>
        <div class="example-grid">
          <button v-for="item in logExamples" :key="item.label" @click="fillExample(item.value)">
            {{ item.label }}
          </button>
        </div>
        <el-input
          v-model="form.rawLog"
          class="log-textarea mono"
          type="textarea"
          :autosize="{ minRows: 16, maxRows: 24 }"
          placeholder="粘贴 NullPointerException / SQLSyntaxErrorException / BeanCreationException 等日志"
        />
        <div class="log-table" aria-label="parsed log rows">
          <div class="log-table-head">
            <span>时间</span>
            <span>级别</span>
            <span>来源</span>
            <span>日志消息</span>
            <span>耗时</span>
          </div>
          <div class="log-table-body">
            <div v-for="row in logRows" :key="row.id" class="log-row" :class="row.level.toLowerCase()">
              <span class="mono">{{ row.timestamp }}</span>
              <span class="level-badge" :class="row.level.toLowerCase()">{{ row.level }}</span>
              <span class="mono source-cell">{{ row.source }}</span>
              <span class="message-cell">{{ row.message }}</span>
              <span class="mono cost-cell">{{ row.cost }}</span>
            </div>
          </div>
        </div>
        <div class="input-actions">
          <CopyButton :text="form.rawLog" label="复制日志" />
          <el-button type="primary" :icon="Search" :loading="loading" @click="runAnalyze">分析日志</el-button>
        </div>
      </div>
    </section>

    <section class="panel diagnosis-panel">
      <div class="panel-header">
        <div>
          <h3 class="panel-title">诊断结果</h3>
          <p class="panel-subtitle">异常类型 · 根因分析 · 建议修复步骤 · 修复 Prompt</p>
        </div>
      </div>
      <div v-if="result" class="diagnosis-body">
        <div class="diagnosis-summary">
          <div>
            <span>异常类型</span>
            <strong>{{ result.exceptionType }}</strong>
          </div>
          <div>
            <span>严重级别</span>
            <strong>{{ result.riskLevel }}</strong>
          </div>
        </div>
        <article class="diagnosis-section">
          <h4>根因判断</h4>
          <p>{{ result.possibleReason }}</p>
        </article>
        <article class="diagnosis-section">
          <h4>建议修复步骤</h4>
          <pre>{{ result.diagnoseSteps }}</pre>
        </article>
        <article class="diagnosis-section">
          <h4>影响范围与风险提示</h4>
          <p><el-icon><Warning /></el-icon>{{ result.riskTips }}</p>
        </article>
        <article class="diagnosis-section">
          <div class="section-title">
            <h4>修复 Prompt</h4>
            <CopyButton :text="result.fixPrompt" label="复制 Prompt" />
          </div>
          <pre class="prompt-box">{{ result.fixPrompt }}</pre>
        </article>
      </div>
      <div v-else class="empty-state">
        <div>
          <p>等待日志诊断</p>
          <span>选择示例或粘贴真实日志，然后点击“分析日志”。</span>
        </div>
      </div>
    </section>

    <aside class="panel side-panel">
      <div class="panel-header">
        <div>
          <h3 class="panel-title">诊断历史</h3>
          <p class="panel-subtitle">最近 Java / Spring Boot 日志分析</p>
        </div>
      </div>
      <div class="history-list">
        <div v-if="!history.length" class="empty-state">暂无日志分析历史</div>
        <article v-for="item in history" :key="item.id" class="history-item">
          <div>
            <strong>{{ item.exceptionType }}</strong>
            <span>{{ item.riskLevel }} · {{ item.createdAt }}</span>
          </div>
          <el-button :icon="CopyDocument" size="small" text @click="copyFixPrompt(item.fixPrompt)">
            复制
          </el-button>
        </article>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.log-page {
  display: grid;
  grid-template-columns: minmax(460px, 1.08fr) minmax(380px, 0.92fr) minmax(220px, 260px);
  gap: var(--space-3);
  min-height: calc(100vh - 126px);
  min-width: 0;
}

.input-panel,
.diagnosis-panel,
.side-panel {
  min-width: 0;
}

.log-toolbar {
  min-height: 40px;
  padding: var(--space-2) var(--space-3);
  border-bottom: var(--border-subtle);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.toolbar-controls {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.project-select {
  width: 180px;
}

.search-input {
  width: 240px;
}

.match-count {
  color: var(--color-text-secondary);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.input-body {
  display: grid;
  gap: var(--space-3);
}

.level-chips {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.level-chips button {
  height: 28px;
  padding: 0 var(--space-2);
  border-radius: var(--radius-sm);
  border: var(--border-default);
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: var(--text-xs);
}

.level-chips button.active,
.level-chips button:hover {
  color: var(--color-text-primary);
  border-color: var(--color-accent);
  background: var(--color-accent-muted);
}

.example-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-2);
}

.example-grid button {
  min-height: 32px;
  border-radius: var(--radius-md);
  border: var(--border-default);
  background: var(--color-bg);
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: var(--text-xs);
}

.example-grid button:hover {
  color: var(--color-text-primary);
  border-color: var(--color-accent);
  background: var(--color-accent-muted);
}

.log-textarea :deep(textarea) {
  font-family: var(--font-mono);
  line-height: var(--leading-code);
}

.log-table {
  border: var(--border-default);
  border-radius: var(--radius-lg);
  background: var(--color-bg);
  overflow: hidden;
}

.log-table-head,
.log-row {
  display: grid;
  grid-template-columns: 120px 56px 100px minmax(0, 1fr) 64px;
  align-items: center;
  gap: var(--space-2);
}

.log-table-head {
  height: 28px;
  padding: 0 var(--space-2);
  border-bottom: var(--border-subtle);
  color: var(--color-text-disabled);
  font-size: var(--text-xs);
  text-transform: uppercase;
}

.log-table-body {
  max-height: 280px;
  overflow: auto;
}

.log-row {
  height: 24px;
  padding: 0 var(--space-2);
  color: var(--color-text-secondary);
  font-family: var(--font-mono);
  font-size: var(--text-sm);
}

.log-row.error {
  background: rgba(242, 105, 105, 0.1);
  color: var(--color-text-primary);
}

.log-row.warn {
  background: rgba(245, 166, 35, 0.08);
}

.level-badge {
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  border: var(--border-default);
  color: var(--color-text-secondary);
  font-size: var(--text-xs);
}

.level-badge.error {
  color: var(--color-error);
}

.level-badge.warn {
  color: var(--color-warning);
}

.level-badge.debug {
  color: var(--color-text-disabled);
}

.source-cell,
.message-cell,
.cost-cell {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.diagnosis-body {
  padding: var(--space-3);
  display: grid;
  gap: var(--space-3);
  max-height: calc(100vh - 130px);
  overflow: auto;
}

.diagnosis-summary {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-2);
}

.diagnosis-summary div,
.diagnosis-section {
  border: var(--border-default);
  border-radius: var(--radius-lg);
  background: var(--color-bg);
  padding: var(--space-3);
}

.diagnosis-summary span {
  display: block;
  color: var(--muted);
  font-size: 11px;
  text-transform: uppercase;
}

.diagnosis-summary strong {
  display: block;
  margin-top: 7px;
}

.diagnosis-section h4 {
  margin: 0 0 8px;
  font-size: 13px;
}

.diagnosis-section p {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
}

.diagnosis-section p :deep(.el-icon),
.diagnosis-section p .el-icon {
  margin-right: 6px;
  color: var(--warning);
  vertical-align: -2px;
}

.diagnosis-section pre {
  margin: 0;
  white-space: pre-wrap;
  color: var(--color-text-primary);
  font-family: var(--font-mono);
  line-height: var(--leading-code);
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.section-title h4 {
  margin: 0;
}

.prompt-box {
  padding: var(--space-3);
  border-radius: var(--radius-lg);
  background: var(--color-bg);
  border: var(--border-default);
  max-height: 260px;
  overflow: auto;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
}

.history-list {
  padding: var(--space-2);
  display: grid;
  gap: var(--space-2);
}

.history-item {
  min-height: 56px;
  padding: var(--space-2);
  border: var(--border-default);
  border-radius: var(--radius-lg);
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.history-item strong,
.history-item span {
  display: block;
}

.history-item span {
  margin-top: var(--space-1);
  color: var(--muted);
  font-size: 11px;
}

@media (max-width: 1280px) {
  .log-page {
    grid-template-columns: minmax(420px, 1fr) minmax(360px, 0.9fr);
  }

  .side-panel {
    grid-column: 1 / -1;
  }
}

@media (max-width: 980px) {
  .log-page {
    grid-template-columns: 1fr;
  }
}
</style>
