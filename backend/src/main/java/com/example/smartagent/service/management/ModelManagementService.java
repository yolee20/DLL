package com.example.smartagent.service.management;

import com.example.smartagent.service.llm.ModelService;

import com.example.smartagent.dto.request.ModelRegisterRequest;
import com.example.smartagent.dto.response.ModelResponse;
import com.example.smartagent.entity.ModelRegistryEntity;
import com.example.smartagent.exception.ResourceNotFoundException;
import com.example.smartagent.model.strategy.ModelStatus;
import com.example.smartagent.repository.ModelRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelManagementService {

    private final ModelRegistryRepository modelRegistryRepository;
    private final ModelService modelService;

    public List<ModelResponse> getAllModels() {
        return modelRegistryRepository.findAll().stream()
                .map(this::convertToResponse)
                .toList();
    }

    public ModelResponse getModel(String name) {
        ModelRegistryEntity model = modelRegistryRepository.findByModelName(name)
                .orElseThrow(() -> new ResourceNotFoundException("模型", name));
        return convertToResponse(model);
    }

    @Transactional
    public ModelResponse createModel(ModelRegisterRequest request) {
        ModelRegistryEntity model = ModelRegistryEntity.builder()
                .modelName(request.getName())
                .modelType(request.getType())
                .endpointUrl(request.getPath())
                .provider("local")
                .loaded(false)
                .build();

        ModelRegistryEntity saved = modelRegistryRepository.save(model);
        log.info("模型已创建: {}", saved.getModelName());
        return convertToResponse(saved);
    }

    @Transactional
    public ModelResponse updateModel(String name, ModelRegisterRequest request) {
        ModelRegistryEntity model = modelRegistryRepository.findByModelName(name)
                .orElseThrow(() -> new ResourceNotFoundException("模型", name));

        model.setModelType(request.getType());
        model.setEndpointUrl(request.getPath());

        ModelRegistryEntity updated = modelRegistryRepository.save(model);
        log.info("模型已更新: {}", name);
        return convertToResponse(updated);
    }

    @Transactional
    public void deleteModel(String name) {
        ModelRegistryEntity model = modelRegistryRepository.findByModelName(name)
                .orElseThrow(() -> new ResourceNotFoundException("模型", name));
        modelRegistryRepository.delete(model);
        log.info("模型已删除: {}", name);
    }

    public void loadModel(String name) {
        if (!modelRegistryRepository.findByModelName(name).isPresent()) {
            throw new ResourceNotFoundException("模型", name);
        }
        modelService.loadModel(name);
        log.info("模型已加载: {}", name);
    }

    public void unloadModel(String name) {
        if (!modelRegistryRepository.findByModelName(name).isPresent()) {
            throw new ResourceNotFoundException("模型", name);
        }
        modelService.unloadModel(name);
        log.info("模型已卸载: {}", name);
    }

    public List<ModelStatus> getAllModelStatus() {
        return modelService.getAllModelStatus();
    }

    private ModelResponse convertToResponse(ModelRegistryEntity model) {
        return ModelResponse.builder()
                .id(model.getId())
                .name(model.getModelName())
                .version("1.0.0")
                .type(model.getModelType())
                .path(model.getEndpointUrl())
                .status(model.getLoaded() ? "loaded" : "unloaded")
                .createdAt(model.getCreatedAt())
                .build();
    }
}
