<template>
  <div class="knowledge-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="问答对管理" name="pairs">
        <div class="page-header">
          <h2>问答对管理</h2>
          <el-button type="primary" @click="openPairModal(false)">添加问答对</el-button>
        </div>
        
        <el-table :data="qaPairs" border v-loading="loadingPairs">
          <el-table-column prop="question" label="问题" min-width="200" />
          <el-table-column prop="answer" label="答案" min-width="300" show-overflow-tooltip />
          <el-table-column prop="categoryName" label="分类" width="120">
            <template #default="scope">
              {{ scope.row.categoryName || '未分类' }}
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="160">
            <template #default="scope">
              {{ formatDate(scope.row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center">
            <template #default="scope">
              <el-button size="small" type="primary" @click="openPairModal(true, scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="deletePair(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      
      <el-tab-pane label="分类管理" name="categories">
        <div class="page-header">
          <h2>分类管理</h2>
          <el-button type="primary" @click="openCategoryModal(false)">添加分类</el-button>
        </div>
        
        <el-table :data="categories" border v-loading="loadingCategories">
          <el-table-column prop="name" label="分类名称" width="200" />
          <el-table-column prop="description" label="描述" min-width="300" />
          <el-table-column prop="createdAt" label="创建时间" width="160">
            <template #default="scope">
              {{ formatDate(scope.row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="center">
            <template #default="scope">
              <el-button size="small" type="primary" @click="openCategoryModal(true, scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteCategory(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
    
    <!-- 问答对对话框 -->
    <el-dialog 
      :title="isEditPair ? '编辑问答对' : '添加问答对'" 
      :visible.sync="pairDialogVisible"
      width="600px"
    >
      <el-form :model="pairForm" label-width="80px" :rules="pairRules" ref="pairFormRef">
        <el-form-item label="问题" prop="question">
          <el-input v-model="pairForm.question" type="textarea" :rows="3" placeholder="请输入问题" />
        </el-form-item>
        <el-form-item label="答案" prop="answer">
          <el-input v-model="pairForm.answer" type="textarea" :rows="4" placeholder="请输入答案" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="pairForm.categoryId" placeholder="选择分类（可选）" clearable style="width: 100%;">
            <el-option 
              v-for="cat in categories" 
              :key="cat.id" 
              :label="cat.name" 
              :value="cat.id" 
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pairDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePair" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
    
    <!-- 分类对话框 -->
    <el-dialog 
      :title="isEditCategory ? '编辑分类' : '添加分类'" 
      :visible.sync="categoryDialogVisible"
      width="500px"
    >
      <el-form :model="categoryForm" label-width="80px" :rules="categoryRules" ref="categoryFormRef">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="categoryForm.description" type="textarea" :rows="2" placeholder="请输入分类描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCategory" :loading="savingCategory">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { qaApi } from '@/api/apiService'

const activeTab = ref('pairs')
const qaPairs = ref([])
const categories = ref([])
const loadingPairs = ref(false)
const loadingCategories = ref(false)

const pairDialogVisible = ref(false)
const categoryDialogVisible = ref(false)
const saving = ref(false)
const savingCategory = ref(false)
const isEditPair = ref(false)
const isEditCategory = ref(false)

const pairFormRef = ref(null)
const categoryFormRef = ref(null)

const pairForm = ref({
  id: null,
  question: '',
  answer: '',
  categoryId: null
})

const categoryForm = ref({
  id: null,
  name: '',
  description: ''
})

const pairRules = {
  question: [
    { required: true, message: '请输入问题', trigger: 'blur' }
  ],
  answer: [
    { required: true, message: '请输入答案', trigger: 'blur' }
  ]
}

const categoryRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' }
  ]
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

const loadQAPairs = async () => {
  loadingPairs.value = true
  try {
    const response = await qaApi.getPairs()
    qaPairs.value = response.data
  } catch (error) {
    console.error('加载问答对失败:', error)
  } finally {
    loadingPairs.value = false
  }
}

const loadCategories = async () => {
  loadingCategories.value = true
  try {
    const response = await qaApi.getCategories()
    categories.value = response.data
  } catch (error) {
    console.error('加载分类失败:', error)
  } finally {
    loadingCategories.value = false
  }
}

const openPairModal = (isEdit, pair = null) => {
  isEditPair.value = isEdit
  if (isEdit && pair) {
    pairForm.value = {
      id: pair.id,
      question: pair.question,
      answer: pair.answer,
      categoryId: pair.categoryId
    }
  } else {
    pairForm.value = {
      id: null,
      question: '',
      answer: '',
      categoryId: null
    }
  }
  pairDialogVisible.value = true
}

const savePair = async () => {
  const valid = await pairFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  saving.value = true
  try {
    const data = {
      question: pairForm.value.question,
      answer: pairForm.value.answer,
      categoryId: pairForm.value.categoryId
    }
    
    if (isEditPair.value) {
      await qaApi.updatePair(pairForm.value.id, data)
      ElMessage.success('问答对更新成功')
    } else {
      await qaApi.createPair(data)
      ElMessage.success('问答对创建成功')
    }
    pairDialogVisible.value = false
    loadQAPairs()
  } catch (error) {
    console.error('保存问答对失败:', error)
  } finally {
    saving.value = false
  }
}

const deletePair = async (pair) => {
  try {
    await ElMessageBox.confirm('确定要删除这条问答对吗？此操作不可恢复！', '警告', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await qaApi.deletePair(pair.id)
    ElMessage.success('问答对删除成功')
    loadQAPairs()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除问答对失败:', error)
    }
  }
}

const openCategoryModal = (isEdit, category = null) => {
  isEditCategory.value = isEdit
  if (isEdit && category) {
    categoryForm.value = {
      id: category.id,
      name: category.name,
      description: category.description || ''
    }
  } else {
    categoryForm.value = {
      id: null,
      name: '',
      description: ''
    }
  }
  categoryDialogVisible.value = true
}

const saveCategory = async () => {
  const valid = await categoryFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  savingCategory.value = true
  try {
    const data = {
      name: categoryForm.value.name,
      description: categoryForm.value.description
    }
    
    if (isEditCategory.value) {
      await qaApi.updateCategory(categoryForm.value.id, data)
      ElMessage.success('分类更新成功')
    } else {
      await qaApi.createCategory(data)
      ElMessage.success('分类创建成功')
    }
    categoryDialogVisible.value = false
    loadCategories()
  } catch (error) {
    console.error('保存分类失败:', error)
  } finally {
    savingCategory.value = false
  }
}

const deleteCategory = async (category) => {
  try {
    await ElMessageBox.confirm(`确定要删除分类 "${category.name}" 吗？此操作不可恢复！`, '警告', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await qaApi.deleteCategory(category.id)
    ElMessage.success('分类删除成功')
    loadCategories()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除分类失败:', error)
    }
  }
}

onMounted(() => {
  loadQAPairs()
  loadCategories()
})
</script>

<style scoped>
.knowledge-container {
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