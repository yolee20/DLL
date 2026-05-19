package com.example.smartagent.service.qa;

import com.example.smartagent.dto.request.QACategoryRequest;
import com.example.smartagent.dto.request.QAPairRequest;
import com.example.smartagent.dto.response.QACategoryResponse;
import com.example.smartagent.dto.response.QAPairResponse;
import com.example.smartagent.entity.QACategoryEntity;
import com.example.smartagent.entity.QAPairEntity;
import com.example.smartagent.exception.ResourceNotFoundException;
import com.example.smartagent.repository.QACategoryRepository;
import com.example.smartagent.repository.QAPairRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QAService {

    private final QAPairRepository qaPairRepository;
    private final QACategoryRepository qaCategoryRepository;

    public List<QAPairResponse> getAllQAPairs() {
        return qaPairRepository.findAll().stream()
                .map(this::convertToResponse)
                .toList();
    }

    public QAPairResponse getQAPair(Long id) {
        QAPairEntity pair = qaPairRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("问答对", id.toString()));
        return convertToResponse(pair);
    }

    @Transactional
    public QAPairResponse createQAPair(QAPairRequest request) {
        QAPairEntity pair = new QAPairEntity();
        pair.setQuestion(request.getQuestion());
        pair.setAnswer(request.getAnswer());

        if (request.getCategoryId() != null) {
            QACategoryEntity category = qaCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("分类", request.getCategoryId().toString()));
            pair.setCategory(category);
        }

        QAPairEntity saved = qaPairRepository.save(pair);
        log.info("问答对已创建: {}", saved.getId());
        return convertToResponse(saved);
    }

    @Transactional
    public QAPairResponse updateQAPair(Long id, QAPairRequest request) {
        QAPairEntity pair = qaPairRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("问答对", id.toString()));

        pair.setQuestion(request.getQuestion());
        pair.setAnswer(request.getAnswer());

        if (request.getCategoryId() != null) {
            QACategoryEntity category = qaCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("分类", request.getCategoryId().toString()));
            pair.setCategory(category);
        }

        QAPairEntity updated = qaPairRepository.save(pair);
        log.info("问答对已更新: {}", id);
        return convertToResponse(updated);
    }

    @Transactional
    public void deleteQAPair(Long id) {
        if (!qaPairRepository.existsById(id)) {
            throw new ResourceNotFoundException("问答对", id.toString());
        }
        qaPairRepository.deleteById(id);
        log.info("问答对已删除: {}", id);
    }

    public List<QACategoryResponse> getAllCategories() {
        return qaCategoryRepository.findAll().stream()
                .map(this::convertCategoryToResponse)
                .toList();
    }

    public QACategoryResponse getCategory(Long id) {
        QACategoryEntity category = qaCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("分类", id.toString()));
        return convertCategoryToResponse(category);
    }

    @Transactional
    public QACategoryResponse createCategory(QACategoryRequest request) {
        QACategoryEntity category = new QACategoryEntity();
        category.setName(request.getName());
        category.setDescription(request.getDescription());

        QACategoryEntity saved = qaCategoryRepository.save(category);
        log.info("分类已创建: {}", saved.getName());
        return convertCategoryToResponse(saved);
    }

    @Transactional
    public QACategoryResponse updateCategory(Long id, QACategoryRequest request) {
        QACategoryEntity category = qaCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("分类", id.toString()));

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        QACategoryEntity updated = qaCategoryRepository.save(category);
        log.info("分类已更新: {}", id);
        return convertCategoryToResponse(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!qaCategoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("分类", id.toString());
        }
        qaCategoryRepository.deleteById(id);
        log.info("分类已删除: {}", id);
    }

    private QAPairResponse convertToResponse(QAPairEntity pair) {
        return QAPairResponse.builder()
                .id(pair.getId())
                .question(pair.getQuestion())
                .answer(pair.getAnswer())
                .categoryId(pair.getCategory() != null ? pair.getCategory().getId() : null)
                .categoryName(pair.getCategory() != null ? pair.getCategory().getName() : null)
                .createdAt(pair.getCreatedAt())
                .build();
    }

    private QACategoryResponse convertCategoryToResponse(QACategoryEntity category) {
        return QACategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .build();
    }
}
