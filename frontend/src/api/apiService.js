import axios from 'axios'
import { ElMessage } from 'element-plus'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

apiClient.interceptors.request.use(
  config => {
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

apiClient.interceptors.response.use(
  response => {
    const res = response.data
    if (res.success === false) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 400:
          ElMessage.error(data.message || '参数错误')
          break
        case 404:
          ElMessage.error(data.message || '资源不存在')
          break
        case 500:
          ElMessage.error(data.message || '服务器内部错误')
          break
        default:
          ElMessage.error(data.message || '请求失败')
      }
    } else if (error.request) {
      ElMessage.error('网络连接失败，请检查网络')
    } else {
      ElMessage.error(error.message || '请求失败')
    }
    return Promise.reject(error)
  }
)

export default apiClient

export const agentApi = {
  chat: (data) => apiClient.post('/agent/chat', data),
  getSessions: () => apiClient.get('/agent/sessions'),
  getSession: (sessionId) => apiClient.get(`/agent/sessions/${sessionId}`),
  deleteSession: (sessionId) => apiClient.delete(`/agent/sessions/${sessionId}`),
  submitFeedback: (conversationId, data) => apiClient.post(`/agent/conversations/${conversationId}/feedback`, data)
}

export const skillApi = {
  getAll: () => apiClient.get('/skills'),
  getByName: (name) => apiClient.get(`/skills/${name}`),
  create: (data) => apiClient.post('/skills', data),
  update: (name, data) => apiClient.put(`/skills/${name}`, data),
  delete: (name) => apiClient.delete(`/skills/${name}`),
  updateStatus: (name, enabled) => apiClient.put(`/skills/${name}/status`, { enabled }),
  getNames: () => apiClient.get('/skills/names')
}

export const modelApi = {
  getAll: () => apiClient.get('/mcp/models'),
  getByName: (name) => apiClient.get(`/mcp/models/${name}`),
  create: (data) => apiClient.post('/mcp/models', data),
  update: (name, data) => apiClient.put(`/mcp/models/${name}`, data),
  delete: (name) => apiClient.delete(`/mcp/models/${name}`),
  updateStatus: (name, action) => apiClient.put(`/mcp/models/${name}/status`, null, {
    params: { action }
  }),
  getAllStatus: () => apiClient.get('/mcp/models/status')
}

export const qaApi = {
  getPairs: () => apiClient.get('/qa/pairs'),
  getPair: (id) => apiClient.get(`/qa/pairs/${id}`),
  createPair: (data) => apiClient.post('/qa/pairs', data),
  updatePair: (id, data) => apiClient.put(`/qa/pairs/${id}`, data),
  deletePair: (id) => apiClient.delete(`/qa/pairs/${id}`),
  getCategories: () => apiClient.get('/qa/categories'),
  getCategory: (id) => apiClient.get(`/qa/categories/${id}`),
  createCategory: (data) => apiClient.post('/qa/categories', data),
  updateCategory: (id, data) => apiClient.put(`/qa/categories/${id}`, data),
  deleteCategory: (id) => apiClient.delete(`/qa/categories/${id}`)
}