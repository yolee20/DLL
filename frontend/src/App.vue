
<template>
  <div class="app-container">
    <el-container>
      <el-aside width="200px" class="sidebar">
        <div class="logo">
          <h2>SmartAgent</h2>
        </div>
        <el-menu :default-active="activeMenu" class="sidebar-menu">
          <el-menu-item index="/" @click="navigate('/')">
            <el-icon><Message /></el-icon>
            <span>智能对话</span>
          </el-menu-item>
          <el-menu-item index="/knowledge" @click="navigate('/knowledge')">
            <el-icon><Notebook /></el-icon>
            <span>知识库管理</span>
          </el-menu-item>
          <el-menu-item index="/skills" @click="navigate('/skills')">
            <el-icon><Setting /></el-icon>
            <span>技能管理</span>
          </el-menu-item>
          <el-menu-item index="/models" @click="navigate('/models')">
            <el-icon><Cpu /></el-icon>
            <span>模型管理</span>
          </el-menu-item>
          <el-menu-item index="/statistics" @click="navigate('/statistics')">
            <el-icon><PieChart /></el-icon>
            <span>统计分析</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header class="header">
          <h1>SmartAgent 智能代理系统</h1>
        </el-header>
        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Message, Notebook, Setting, Cpu, PieChart } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const activeMenu = ref('/')

const navigate = (path) => {
  activeMenu.value = path
  router.push(path)
}

onMounted(() => {
  activeMenu.value = route.path
})
</script>

<style scoped>
.app-container {
  height: 100vh;
}

.sidebar {
  background-color: #2a3f5f;
  color: white;
}

.logo {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid #4a5f7f;
}

.logo h2 {
  margin: 0;
  font-size: 18px;
}

.sidebar-menu {
  border-right: none;
}

.el-menu-item {
  color: #b8c7ce;
}

.el-menu-item:hover,
.el-menu-item.is-active {
  background-color: #1a2f4f;
  color: white;
}

.header {
  background-color: #ffffff;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 20px;
}

.header h1 {
  font-size: 18px;
  color: #333;
}

.main-content {
  padding: 20px;
  overflow-y: auto;
}
</style>
