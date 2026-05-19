<template>
  <div class="chat-container">
    <div class="chat-header">
      <div class="header-left">
        <h3>智能助手</h3>
        <span class="session-info" v-if="sessionId">会话ID: {{ sessionId.substring(0, 8) }}...</span>
      </div>
      <div class="header-right">
        <el-button size="small" @click="showHistory = true">历史会话</el-button>
        <el-button size="small" type="danger" @click="clearChat">清空对话</el-button>
      </div>
    </div>
    
    <div class="chat-messages" ref="messageContainer">
      <div 
        v-for="(msg, index) in messages" 
        :key="index" 
        :class="['message-item', msg.type]"
      >
        <div class="avatar">
          <el-icon v-if="msg.type === 'user'"><User /></el-icon>
          <el-icon v-else><MessageBox /></el-icon>
        </div>
        <div class="message-content">
          <div class="message-header" v-if="msg.type === 'agent'">
            <span class="intent-tag" v-if="msg.intent">
              意图: {{ msg.intent }} ({{ (msg.intentConfidence * 100).toFixed(0) }}%)
            </span>
            <span class="skill-tag" v-if="msg.skill">
              技能: {{ msg.skill }}
            </span>
          </div>
          <p>{{ msg.content }}</p>
          <div class="message-footer">
            <span class="message-time">{{ msg.time }}</span>
            <el-button 
              v-if="msg.type === 'agent' && msg.conversationId" 
              size="mini" 
              type="text"
              @click="showFeedbackDialog(msg)"
            >
              反馈
            </el-button>
          </div>
        </div>
      </div>
      
      <div v-if="loading" class="loading-indicator">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>正在思考...</span>
      </div>
    </div>
    
    <div class="chat-input">
      <el-input 
        v-model="inputMessage" 
        placeholder="请输入您的问题，按 Enter 发送..."
        :disabled="loading"
        @keyup.enter="sendMessage"
      />
      <el-button type="primary" @click="sendMessage" :loading="loading">发送</el-button>
    </div>
    
    <!-- 历史会话对话框 -->
    <el-dialog title="历史会话" :visible.sync="showHistory" width="600px">
      <el-list>
        <el-list-item v-for="sess in sessions" :key="sess">
          <div class="session-item">
            <span>会话ID: {{ sess }}</span>
            <el-button size="small" type="primary" @click="loadSession(sess)">加载</el-button>
          </div>
        </el-list-item>
      </el-list>
      <div v-if="sessions.length === 0" class="empty-tip">
        暂无历史会话
      </div>
    </el-dialog>
    
    <!-- 反馈对话框 -->
    <el-dialog title="提交反馈" :visible.sync="showFeedback" width="400px">
      <el-form :model="feedbackForm" label-width="80px">
        <el-form-item label="满意度">
          <el-radio-group v-model="feedbackForm.satisfaction">
            <el-radio :label="1">非常满意</el-radio>
            <el-radio :label="2">满意</el-radio>
            <el-radio :label="3">一般</el-radio>
            <el-radio :label="4">不满意</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input 
            v-model="feedbackForm.comment" 
            type="textarea" 
            :rows="3"
            placeholder="请输入您的补充说明（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showFeedback = false">取消</el-button>
        <el-button type="primary" @click="submitFeedback">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { User, MessageBox, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { agentApi } from '@/api/apiService'

const messages = ref([
  {
    type: 'agent',
    content: '您好！我是 SmartAgent 智能助手，有什么可以帮助您的吗？',
    time: new Date().toLocaleTimeString(),
    skill: null,
    intent: null,
    intentConfidence: null,
    conversationId: null
  }
])

const inputMessage = ref('')
const messageContainer = ref(null)
const sessionId = ref(null)
const loading = ref(false)
const showHistory = ref(false)
const showFeedback = ref(false)
const sessions = ref([])

const feedbackForm = ref({
  satisfaction: 1,
  comment: '',
  conversationId: null
})

const sendMessage = async () => {
  if (!inputMessage.value.trim() || loading.value) return
  
  const userMessage = inputMessage.value.trim()
  inputMessage.value = ''
  
  messages.value.push({
    type: 'user',
    content: userMessage,
    time: new Date().toLocaleTimeString(),
    skill: null,
    intent: null,
    intentConfidence: null,
    conversationId: null
  })
  
  scrollToBottom()
  loading.value = true
  
  // 添加机器人消息占位
  const botMsgIndex = messages.value.push({
    type: 'agent',
    content: '',
    time: new Date().toLocaleTimeString(),
    skill: null,
    intent: null,
    intentConfidence: null,
    conversationId: null
  }) - 1
  
  scrollToBottom()
  
  try {
    const response = await fetch('/api/agent/chat/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        message: userMessage,
        sessionId: sessionId.value
      })
    })
    
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      messages.value[botMsgIndex].content += decoder.decode(value)
      scrollToBottom()
    }
    
    loadSessions()
  } catch (error) {
    console.error('发送消息失败:', error)
    messages.value[botMsgIndex].content = '抱歉，系统暂时无法响应，请稍后重试。'
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  })
}

const clearChat = () => {
  messages.value = [
    {
      type: 'agent',
      content: '您好！我是 SmartAgent 智能助手，有什么可以帮助您的吗？',
      time: new Date().toLocaleTimeString(),
      skill: null,
      intent: null,
      intentConfidence: null,
      conversationId: null
    }
  ]
  sessionId.value = null
}

const loadSessions = async () => {
  try {
    const response = await agentApi.getSessions()
    sessions.value = response.data || []
  } catch (error) {
    console.error('加载会话列表失败:', error)
  }
}

const loadSession = async (sessId) => {
  try {
    const response = await agentApi.getSession(sessId)
    const sessionData = response.data
    
    if (sessionData && sessionData.messages) {
      messages.value = sessionData.messages.map(m => ({
        type: 'agent',
        content: m.agentResponse,
        time: new Date(m.createdAt).toLocaleTimeString(),
        skill: m.skillName,
        intent: m.intent,
        intentConfidence: m.intentConfidence,
        conversationId: m.id
      }))
      
      messages.value.unshift({
        type: 'user',
        content: sessionData.messages[0]?.userMessage || '历史会话',
        time: new Date(sessionData.createdAt).toLocaleTimeString(),
        skill: null,
        intent: null,
        intentConfidence: null,
        conversationId: null
      })
      
      sessionId.value = sessId
    }
    
    showHistory.value = false
    ElMessage.success('会话加载成功')
  } catch (error) {
    console.error('加载会话失败:', error)
  }
}

const showFeedbackDialog = (msg) => {
  feedbackForm.value = {
    satisfaction: 1,
    comment: '',
    conversationId: msg.conversationId
  }
  showFeedback.value = true
}

const submitFeedback = async () => {
  if (!feedbackForm.value.conversationId) {
    ElMessage.warning('请先选择一条对话记录')
    return
  }
  
  try {
    await agentApi.submitFeedback(feedbackForm.value.conversationId, {
      satisfaction: feedbackForm.value.satisfaction,
      comment: feedbackForm.value.comment
    })
    ElMessage.success('反馈提交成功')
    showFeedback.value = false
  } catch (error) {
    console.error('提交反馈失败:', error)
  }
}

onMounted(() => {
  scrollToBottom()
  loadSessions()
})
</script>

<style scoped>
.chat-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
  border-radius: 8px;
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background-color: #2a3f5f;
  color: white;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.header-left h3 {
  margin: 0;
  font-size: 16px;
}

.session-info {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.header-right {
  display: flex;
  gap: 10px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-item.user .message-content {
  background-color: #4a90d9;
  color: white;
  border-radius: 12px 12px 0 12px;
}

.message-item.agent .message-content {
  background-color: white;
  border-radius: 12px 12px 12px 0;
  border: 1px solid #e8e8e8;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 10px;
  flex-shrink: 0;
}

.message-item.user .avatar {
  background-color: #4a90d9;
  color: white;
}

.message-item.agent .avatar {
  background-color: #2a3f5f;
  color: white;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
}

.message-header {
  margin-bottom: 8px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.intent-tag, .skill-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.intent-tag {
  background-color: #e8f4fd;
  color: #4a90d9;
}

.skill-tag {
  background-color: #e8f8e8;
  color: #67c23a;
}

.message-content p {
  margin: 0 0 8px 0;
  line-height: 1.5;
}

.message-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.message-time {
  font-size: 12px;
  color: #999;
}

.message-item.user .message-time {
  color: rgba(255, 255, 255, 0.6);
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  color: #999;
}

.chat-input {
  display: flex;
  padding: 15px;
  background-color: white;
  border-top: 1px solid #e8e8e8;
  gap: 10px;
}

.chat-input .el-input {
  flex: 1;
}

.session-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 10px 0;
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 20px;
}
</style>