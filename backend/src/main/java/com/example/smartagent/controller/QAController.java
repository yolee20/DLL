package com.example.smartagent.controller;

import com.example.smartagent.dto.request.QACategoryRequest;
import com.example.smartagent.dto.request.QAPairRequest;
import com.example.smartagent.dto.response.ApiResponse;
import com.example.smartagent.dto.response.QACategoryResponse;
import com.example.smartagent.dto.response.QAPairResponse;
import com.example.smartagent.entity.QACategory;
import com.example.smartagent.entity.QAPair;
import com.example.smartagent.exception.ResourceNotFoundException;
import com.example.smartagent.repository.QACategoryRepository;
import com.example.smartagent.repository.QAPairRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * QA 知识库控制器
 * 提供问答对和分类管理相关的 REST API 接口
 */
@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
@Slf4j
public class QAController {

    private final QAPairRepository qaPairRepository;
    private final QACategoryRepository qaCategoryRepository;

    // ==================== 问答对管理 ====================

    @GetMapping("/pairs")
    public ResponseEntity<ApiResponse<List<QAPairResponse>>> getAllQAPairs() {
        List<QAPairResponse> pairs = qaPairRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(pairs));
    }

    @GetMapping("/pairs/{id}")
    public ResponseEntity<ApiResponse<QAPairResponse>> getQAPair(@PathVariable Long id) {
        QAPair pair = qaPairRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("问答对", id.toString()));
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(pair)));
    }

    @PostMapping("/pairs")
    public ResponseEntity<ApiResponse<QAPairResponse>> createQAPair(@Valid @RequestBody QAPairRequest request) {
        QAPair pair = new QAPair();
        pair.setQuestion(request.getQuestion());
        pair.setAnswer(request.getAnswer());
        
        if (request.getCategoryId() != null) {
            QACategory category = qaCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("分类", request.getCategoryId().toString()));
            pair.setCategory(category);
        }
        
        QAPair saved = qaPairRepository.save(pair);
        log.info("问答对已创建: {}", saved.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("问答对创建成功", convertToResponse(saved)));
    }

    @PutMapping("/pairs/{id}")
    public ResponseEntity<ApiResponse<QAPairResponse>> updateQAPair(
            @PathVariable Long id,
            @Valid @RequestBody QAPairRequest request) {
        
        QAPair pair = qaPairRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("问答对", id.toString()));
        
        pair.setQuestion(request.getQuestion());
        pair.setAnswer(request.getAnswer());
        
        if (request.getCategoryId() != null) {
            QACategory category = qaCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("分类", request.getCategoryId().toString()));
            pair.setCategory(category);
        }
        
        QAPair updated = qaPairRepository.save(pair);
        return ResponseEntity.ok(ApiResponse.success("问答对更新成功", convertToResponse(updated)));
    }

    @DeleteMapping("/pairs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteQAPair(@PathVariable Long id) {
        QAPair pair = qaPairRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("问答对", id.toString()));
        
        qaPairRepository.delete(pair);
        log.info("问答对已删除: {}", id);
        
        return ResponseEntity.ok(ApiResponse.success("问答对删除成功", null));
    }

    // ==================== 分类管理 ====================

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<QACategoryResponse>>> getAllCategories() {
        List<QACategoryResponse> categories = qaCategoryRepository.findAll().stream()
                .map(this::convertCategoryToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<QACategoryResponse>> getCategory(@PathVariable Long id) {
        QACategory category = qaCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("分类", id.toString()));
        return ResponseEntity.ok(ApiResponse.success(convertCategoryToResponse(category)));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<QACategoryResponse>> createCategory(
            @Valid @RequestBody QACategoryRequest request) {
        
        QACategory category = new QACategory();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        
        QACategory saved = qaCategoryRepository.save(category);
        log.info("分类已创建: {}", saved.getName());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("分类创建成功", convertCategoryToResponse(saved)));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<QACategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody QACategoryRequest request) {
        
        QACategory category = qaCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("分类", id.toString()));
        
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        
        QACategory updated = qaCategoryRepository.save(category);
        return ResponseEntity.ok(ApiResponse.success("分类更新成功", convertCategoryToResponse(updated)));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        QACategory category = qaCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("分类", id.toString()));
        
        qaCategoryRepository.delete(category);
        log.info("分类已删除: {}", id);
        
        return ResponseEntity.ok(ApiResponse.success("分类删除成功", null));
    }

    // ==================== 转换方法 ====================

    private QAPairResponse convertToResponse(QAPair pair) {
        return QAPairResponse.builder()
                .id(pair.getId())
                .question(pair.getQuestion())
                .answer(pair.getAnswer())
                .categoryId(pair.getCategory() != null ? pair.getCategory().getId() : null)
                .categoryName(pair.getCategory() != null ? pair.getCategory().getName() : null)
                .createdAt(pair.getCreatedAt())
                .build();
    }
    
    private QACategoryResponse convertCategoryToResponse(QACategory category) {
        return QACategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .build();
    }
}