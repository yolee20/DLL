
import { createRouter, createWebHistory } from 'vue-router'
import Chat from '@/views/Chat.vue'
import SkillManagement from '@/views/SkillManagement.vue'
import ModelManagement from '@/views/ModelManagement.vue'
import KnowledgeBase from '@/views/KnowledgeBase.vue'
import Statistics from '@/views/Statistics.vue'

const routes = [
  { path: '/', name: 'Chat', component: Chat },
  { path: '/skills', name: 'SkillManagement', component: SkillManagement },
  { path: '/models', name: 'ModelManagement', component: ModelManagement },
  { path: '/knowledge', name: 'KnowledgeBase', component: KnowledgeBase },
  { path: '/statistics', name: 'Statistics', component: Statistics }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
