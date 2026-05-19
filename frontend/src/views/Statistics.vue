<template>
  <div class="statistics-container">
    <div class="page-header">
      <h2>统计分析</h2>
      <el-button type="primary" size="small" @click="loadStats">刷新数据</el-button>
    </div>
    
    <div class="stats-cards">
      <el-card class="stat-card" shadow="hover">
        <div class="stat-icon conversations">
          <el-icon><Message /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalConversations }}</div>
          <div class="stat-label">总对话数</div>
        </div>
      </el-card>
      
      <el-card class="stat-card" shadow="hover">
        <div class="stat-icon skills">
          <el-icon><Setting /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalSkills }}</div>
          <div class="stat-label">技能数量</div>
        </div>
      </el-card>
      
      <el-card class="stat-card" shadow="hover">
        <div class="stat-icon models">
          <el-icon><Cpu /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalModels }}</div>
          <div class="stat-label">模型数量</div>
        </div>
      </el-card>
      
      <el-card class="stat-card" shadow="hover">
        <div class="stat-icon qapairs">
          <el-icon><Notebook /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalQAPairs }}</div>
          <div class="stat-label">问答对数量</div>
        </div>
      </el-card>
    </div>
    
    <div class="charts-section">
      <el-card title="技能调用统计" shadow="hover">
        <template #header>
          <div class="card-header">
            <span>技能调用统计</span>
          </div>
        </template>
        <div class="chart-container">
          <el-table :data="skillStats" border stripe v-loading="loadingSkill">
            <el-table-column prop="skillName" label="技能名称" width="150" />
            <el-table-column prop="count" label="调用次数" width="120" align="center" />
            <el-table-column prop="percentage" label="占比" align="center">
              <template #default="scope">
                <el-progress 
                  :percentage="scope.row.percentage" 
                  :color="getSkillColor(scope.$index)"
                  :show-text="true"
                />
              </template>
            </el-table-column>
          </el-table>
          <div v-if="skillStats.length === 0 && !loadingSkill" class="empty-tip">
            暂无技能调用数据
          </div>
        </div>
      </el-card>
      
      <el-card title="意图识别分布" shadow="hover">
        <template #header>
          <div class="card-header">
            <span>意图识别分布</span>
          </div>
        </template>
        <div class="chart-container">
          <el-table :data="intentStats" border stripe v-loading="loadingIntent">
            <el-table-column prop="intent" label="意图" width="150">
              <template #default="scope">
                <el-tag size="small">{{ getIntentLabel(scope.row.intent) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="count" label="数量" width="120" align="center" />
            <el-table-column prop="percentage" label="占比" align="center">
              <template #default="scope">
                <el-progress 
                  :percentage="scope.row.percentage" 
                  :color="getIntentColor(scope.$index)"
                  :show-text="true"
                />
              </template>
            </el-table-column>
          </el-table>
          <div v-if="intentStats.length === 0 && !loadingIntent" class="empty-tip">
            暂无意图识别数据
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Message, Setting, Cpu, Notebook } from '@element-plus/icons-vue'
import { skillApi, modelApi, qaApi, agentApi } from '@/api/apiService'

const stats = ref({
  totalConversations: 0,
  totalSkills: 0,
  totalModels: 0,
  totalQAPairs: 0
})

const skillStats = ref([])
const intentStats = ref([])
const loadingSkill = ref(false)
const loadingIntent = ref(false)

const skillColors = ['#67c23a', '#409eff', '#e6a23c', '#f56c6c', '#909399']
const intentColors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399']

const intentLabels = {
  qa: '问答',
  order: '订单',
  weather: '天气',
  default: '其他'
}

const getSkillColor = (index) => skillColors[index % skillColors.length]
const getIntentColor = (index) => intentColors[index % intentColors.length]
const getIntentLabel = (intent) => intentLabels[intent] || intentLabels.default

const loadStats = async () => {
  try {
    const [skillsRes, modelsRes, qaRes, sessionsRes] = await Promise.all([
      skillApi.getAll(),
      modelApi.getAll(),
      qaApi.getPairs(),
      agentApi.getSessions()
    ])
    
    stats.value = {
      totalConversations: sessionsRes.data?.length || 0,
      totalSkills: skillsRes.data?.length || 0,
      totalModels: modelsRes.data?.length || 0,
      totalQAPairs: qaRes.data?.length || 0
    }
    
    loadSkillStats()
    loadIntentStats()
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const loadSkillStats = async () => {
  loadingSkill.value = true
  try {
    const sessionsRes = await agentApi.getSessions()
    const sessions = sessionsRes.data || []
    
    if (sessions.length === 0) {
      skillStats.value = []
      return
    }
    
    const skillCounts = {}
    for (const sessionId of sessions) {
      try {
        const sessionData = await agentApi.getSession(sessionId)
        if (sessionData.data?.messages) {
          for (const msg of sessionData.data.messages) {
            const skillName = msg.skillName || 'unknown'
            skillCounts[skillName] = (skillCounts[skillName] || 0) + 1
          }
        }
      } catch (e) {
        console.error('获取会话详情失败:', e)
      }
    }
    
    const total = Object.values(skillCounts).reduce((sum, count) => sum + count, 0)
    skillStats.value = Object.entries(skillCounts)
      .map(([skillName, count]) => ({
        skillName,
        count,
        percentage: total > 0 ? Math.round((count / total) * 100) : 0
      }))
      .sort((a, b) => b.count - a.count)
  } catch (error) {
    console.error('加载技能统计失败:', error)
  } finally {
    loadingSkill.value = false
  }
}

const loadIntentStats = async () => {
  loadingIntent.value = true
  try {
    const sessionsRes = await agentApi.getSessions()
    const sessions = sessionsRes.data || []
    
    if (sessions.length === 0) {
      intentStats.value = []
      return
    }
    
    const intentCounts = {}
    for (const sessionId of sessions) {
      try {
        const sessionData = await agentApi.getSession(sessionId)
        if (sessionData.data?.messages) {
          for (const msg of sessionData.data.messages) {
            const intent = msg.intent || 'default'
            intentCounts[intent] = (intentCounts[intent] || 0) + 1
          }
        }
      } catch (e) {
        console.error('获取会话详情失败:', e)
      }
    }
    
    const total = Object.values(intentCounts).reduce((sum, count) => sum + count, 0)
    intentStats.value = Object.entries(intentCounts)
      .map(([intent, count]) => ({
        intent,
        count,
        percentage: total > 0 ? Math.round((count / total) * 100) : 0
      }))
      .sort((a, b) => b.count - a.count)
  } catch (error) {
    console.error('加载意图统计失败:', error)
  } finally {
    loadingIntent.value = false
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.statistics-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
}

.stat-icon .el-icon {
  font-size: 28px;
  color: white;
}

.stat-icon.conversations {
  background: linear-gradient(135deg, #409eff, #66b1ff);
}

.stat-icon.skills {
  background: linear-gradient(135deg, #67c23a, #85ce61);
}

.stat-icon.models {
  background: linear-gradient(135deg, #e6a23c, #ebb563);
}

.stat-icon.qapairs {
  background: linear-gradient(135deg, #f56c6c, #f78989);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #333;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #999;
  margin-top: 5px;
}

.charts-section {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.card-header {
  font-weight: 600;
}

.chart-container {
  padding: 10px 0;
}

.chart-container .el-progress {
  width: 100%;
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 30px;
}
</style>