<template>
  <div class="model-container">
    <div class="page-header">
      <h2>模型管理</h2>
    </div>
    
    <el-table :data="models" border v-loading="loading">
      <el-table-column prop="name" label="模型名称" width="150" />
      <el-table-column prop="version" label="版本" width="100" />
      <el-table-column prop="type" label="类型" width="120">
        <template #default="scope">
          <el-tag size="small">{{ getTypeLabel(scope.row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路径" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'loaded' ? 'success' : 'info'" size="small">
            {{ scope.row.status === 'loaded' ? '已加载' : '未加载' }}
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
          <el-button 
            size="small" 
            :type="scope.row.status === 'loaded' ? 'warning' : 'success'" 
            @click="toggleModel(scope.row)"
            :loading="scope.row.loading"
          >
            {{ scope.row.status === 'loaded' ? '卸载' : '加载' }}
          </el-button>
          <el-button size="small" type="primary" @click="editModel(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteModel(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <el-dialog 
      :title="isEdit ? '编辑模型' : '添加模型'" 
      :visible.sync="dialogVisible"
      width="500px"
    >
      <el-form :model="modelForm" label-width="100px" :rules="modelRules" ref="modelFormRef">
        <el-form-item label="模型名称" prop="name">
          <el-input v-model="modelForm.name" :disabled="isEdit" placeholder="请输入模型名称" />
        </el-form-item>
        <el-form-item label="版本号" prop="version">
          <el-input v-model="modelForm.version" placeholder="如：1.0.0" />
        </el-form-item>
        <el-form-item label="模型类型" prop="type">
          <el-select v-model="modelForm.type" placeholder="请选择模型类型" style="width: 100%;">
            <el-option label="意图识别模型" value="intent" />
            <el-option label="嵌入模型" value="embedding" />
            <el-option label="大语言模型" value="llm" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型路径" prop="path">
          <el-input v-model="modelForm.path" placeholder="请输入模型文件路径或模型标识" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveModel" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { modelApi } from '@/api/apiService'

const models = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const modelFormRef = ref(null)

const modelForm = ref({
  name: '',
  version: '',
  type: '',
  path: ''
})

const modelRules = {
  name: [
    { required: true, message: '请输入模型名称', trigger: 'blur' }
  ],
  version: [
    { required: true, message: '请输入版本号', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择模型类型', trigger: 'change' }
  ],
  path: [
    { required: true, message: '请输入模型路径', trigger: 'blur' }
  ]
}

const typeMap = {
  intent: '意图识别',
  embedding: '嵌入模型',
  llm: '大语言模型'
}

const getTypeLabel = (type) => {
  return typeMap[type] || type
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

const loadModels = async () => {
  loading.value = true
  try {
    const response = await modelApi.getAll()
    models.value = response.data
  } catch (error) {
    console.error('加载模型失败:', error)
  } finally {
    loading.value = false
  }
}

const openAddModal = () => {
  isEdit.value = false
  modelForm.value = { name: '', version: '', type: '', path: '' }
  dialogVisible.value = true
}

const editModel = (model) => {
  isEdit.value = true
  modelForm.value = { ...model }
  dialogVisible.value = true
}

const saveModel = async () => {
  const valid = await modelFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  saving.value = true
  try {
    if (isEdit.value) {
      await modelApi.update(modelForm.value.name, modelForm.value)
      ElMessage.success('模型更新成功')
    } else {
      await modelApi.create(modelForm.value)
      ElMessage.success('模型创建成功')
    }
    dialogVisible.value = false
    loadModels()
  } catch (error) {
    console.error('保存模型失败:', error)
  } finally {
    saving.value = false
  }
}

const toggleModel = async (model) => {
  const action = model.status === 'loaded' ? '卸载' : '加载'
  try {
    await ElMessageBox.confirm(`确定要${action}模型 "${model.name}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    model.loading = true
    const apiAction = model.status === 'loaded' ? 'unload' : 'load'
    await modelApi.updateStatus(model.name, apiAction)
    ElMessage.success(`模型${action}成功`)
    loadModels()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('切换模型状态失败:', error)
    }
  } finally {
    model.loading = false
  }
}

const deleteModel = async (model) => {
  try {
    await ElMessageBox.confirm(`确定要删除模型 "${model.name}" 吗？此操作不可恢复！`, '警告', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await modelApi.delete(model.name)
    ElMessage.success('模型删除成功')
    loadModels()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除模型失败:', error)
    }
  }
}

defineExpose({ openAddModal })

onMounted(() => {
  loadModels()
})
</script>

<style scoped>
.model-container {
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