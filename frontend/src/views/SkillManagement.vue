<template>
  <div class="skill-container">
    <div class="page-header">
      <h2>技能管理</h2>
    </div>
    
    <el-table :data="skills" border v-loading="loading">
      <el-table-column prop="name" label="技能名称" width="150" />
      <el-table-column prop="description" label="描述" min-width="200" />
      <el-table-column prop="intentPatterns" label="意图模式" min-width="200">
        <template #default="scope">
          <el-tag 
            v-for="pattern in parseIntentPatterns(scope.row.intentPatterns)" 
            :key="pattern"
            size="small"
            style="margin-right: 5px;"
          >
            {{ pattern }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="enabled" label="状态" width="80" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.enabled ? 'success' : 'danger'" size="small">
            {{ scope.row.enabled ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160">
        <template #default="scope">
          {{ formatDate(scope.row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" align="center">
        <template #default="scope">
          <el-button size="small" type="primary" @click="editSkill(scope.row)">编辑</el-button>
          <el-button 
            size="small" 
            :type="scope.row.enabled ? 'warning' : 'success'" 
            @click="toggleSkill(scope.row)"
          >
            {{ scope.row.enabled ? '禁用' : '启用' }}
          </el-button>
          <el-button size="small" type="danger" @click="deleteSkill(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-dialog 
      :title="isEdit ? '编辑技能' : '添加技能'" 
      :visible.sync="dialogVisible"
      width="500px"
    >
      <el-form :model="skillForm" label-width="100px" :rules="skillRules" ref="skillFormRef">
        <el-form-item label="技能名称" prop="name">
          <el-input v-model="skillForm.name" :disabled="isEdit" placeholder="请输入技能名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="skillForm.description" type="textarea" :rows="2" placeholder="请输入技能描述" />
        </el-form-item>
        <el-form-item label="意图模式" prop="intentPatterns">
          <el-input 
            v-model="intentPatternsInput" 
            placeholder="多个模式用逗号分隔，如：订餐,外卖,点餐" 
          />
        </el-form-item>
        <el-form-item label="类路径" prop="classPath">
          <el-input v-model="skillForm.classPath" placeholder="请输入完整的类路径" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSkill" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { skillApi } from '@/api/apiService'

const skills = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const skillFormRef = ref(null)

const skillForm = ref({
  name: '',
  description: '',
  intentPatterns: [],
  classPath: ''
})

const intentPatternsInput = ref('')

const skillRules = {
  name: [
    { required: true, message: '请输入技能名称', trigger: 'blur' }
  ],
  classPath: [
    { required: true, message: '请输入类路径', trigger: 'blur' }
  ]
}

const parseIntentPatterns = (patterns) => {
  if (!patterns) return []
  if (Array.isArray(patterns)) return patterns
  try {
    return JSON.parse(patterns)
  } catch {
    return []
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

const loadSkills = async () => {
  loading.value = true
  try {
    const response = await skillApi.getAll()
    skills.value = response.data
  } catch (error) {
    console.error('加载技能失败:', error)
  } finally {
    loading.value = false
  }
}

const openAddModal = () => {
  isEdit.value = false
  skillForm.value = {
    name: '',
    description: '',
    intentPatterns: [],
    classPath: ''
  }
  intentPatternsInput.value = ''
  dialogVisible.value = true
}

const editSkill = (skill) => {
  isEdit.value = true
  skillForm.value = { 
    name: skill.name,
    description: skill.description || '',
    intentPatterns: parseIntentPatterns(skill.intentPatterns),
    classPath: skill.classPath || ''
  }
  intentPatternsInput.value = parseIntentPatterns(skill.intentPatterns).join(', ')
  dialogVisible.value = true
}

const saveSkill = async () => {
  const valid = await skillFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  saving.value = true
  try {
    const data = {
      ...skillForm.value,
      intentPatterns: intentPatternsInput.value
        .split(',')
        .map(p => p.trim())
        .filter(p => p)
    }
    
    if (isEdit.value) {
      await skillApi.update(skillForm.value.name, data)
      ElMessage.success('技能更新成功')
    } else {
      await skillApi.create(data)
      ElMessage.success('技能创建成功')
    }
    dialogVisible.value = false
    loadSkills()
  } catch (error) {
    console.error('保存技能失败:', error)
  } finally {
    saving.value = false
  }
}

const toggleSkill = async (skill) => {
  const action = skill.enabled ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}技能 "${skill.name}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await skillApi.updateStatus(skill.name, !skill.enabled)
    ElMessage.success(`技能已${action}`)
    loadSkills()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('切换技能状态失败:', error)
    }
  }
}

const deleteSkill = async (skill) => {
  try {
    await ElMessageBox.confirm(`确定要删除技能 "${skill.name}" 吗？此操作不可恢复！`, '警告', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await skillApi.delete(skill.name)
    ElMessage.success('技能删除成功')
    loadSkills()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除技能失败:', error)
    }
  }
}

defineExpose({ openAddModal })

onMounted(() => {
  loadSkills()
})
</script>

<style scoped>
.skill-container {
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
</style>