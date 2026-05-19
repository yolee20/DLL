package com.example.smartagent.controller;

import com.example.smartagent.dto.request.ModelRegisterRequest;
import com.example.smartagent.dto.response.ApiResponse;
import com.example.smartagent.dto.response.ModelResponse;
import com.example.smartagent.entity.ModelRegistry;
import com.example.smartagent.exception.ResourceNotFoundException;
import com.example.smartagent.mcp.ModelControlProtocol;
import com.example.smartagent.mcp.ModelStatus;
import com.example.smartagent.repository.ModelRegistryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP (Model Control Protocol) 控制器
 * 提供模型管理相关的 REST API 接口
 */
@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
@Slf4j
public class MCPController {

    private final ModelControlProtocol mcp;
    private final ModelRegistryRepository modelRegistryRepository;

    @GetMapping("/models")
    public ResponseEntity<ApiResponse<List<ModelResponse>>> getAllModels() {
        List<ModelResponse> models = modelRegistryRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(models));
    }

    @GetMapping("/models/{name}")
    public ResponseEntity<ApiResponse<ModelResponse>> getModel(@PathVariable String name) {
        ModelRegistry model = modelRegistryRepository.findByModelName(name)
                .orElseThrow(() -> new ResourceNotFoundException("模型", name));
        return ResponseEntity.ok(ApiResponse.success(convertToResponse(model)));
    }

    @PostMapping("/models")
    public ResponseEntity<ApiResponse<ModelResponse>> createModel(@Valid @RequestBody ModelRegisterRequest request) {
        ModelRegistry model = ModelRegistry.builder()
                .modelName(request.getName())
                .modelType(request.getType())
                .endpointUrl(request.getPath())
                .provider("local")
                .loaded(false)
                .build();

        ModelRegistry saved = modelRegistryRepository.save(model);
        log.info("模型已创建: {}", saved.getModelName());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("模型创建成功", convertToResponse(saved)));
    }

    @PutMapping("/models/{name}")
    public ResponseEntity<ApiResponse<ModelResponse>> updateModel(
            @PathVariable String name,
            @Valid @RequestBody ModelRegisterRequest request) {

        ModelRegistry model = modelRegistryRepository.findByModelName(name)
                .orElseThrow(() -> new ResourceNotFoundException("模型", name));

        model.setModelType(request.getType());
        model.setEndpointUrl(request.getPath());

        ModelRegistry updated = modelRegistryRepository.save(model);
        return ResponseEntity.ok(ApiResponse.success("模型更新成功", convertToResponse(updated)));
    }

    @DeleteMapping("/models/{name}")
    public ResponseEntity<ApiResponse<Void>> deleteModel(@PathVariable String name) {
        ModelRegistry model = modelRegistryRepository.findByModelName(name)
                .orElseThrow(() -> new ResourceNotFoundException("模型", name));

        modelRegistryRepository.delete(model);
        log.info("模型已删除: {}", name);

        return ResponseEntity.ok(ApiResponse.success("模型删除成功", null));
    }

    /**
     * 加载模型
     */
    @PutMapping("/models/{name}/status")
    public ResponseEntity<ApiResponse<ModelResponse>> updateModelStatus(
            @PathVariable String name,
            @RequestParam String action) {
        
        ModelRegistry model = modelRegistryRepository.findByModelName(name)
                .orElseThrow(() -> new ResourceNotFoundException("模型", name));

        if ("load".equalsIgnoreCase(action)) {
            mcp.loadModel(name);
            log.info("模型已加载: {}", name);
            return ResponseEntity.ok(ApiResponse.success("模型加载成功", convertToResponse(model)));
        } else if ("unload".equalsIgnoreCase(action)) {
            mcp.unloadModel(name);
            log.info("模型已卸载: {}", name);
            return ResponseEntity.ok(ApiResponse.success("模型卸载成功", convertToResponse(model)));
        } else {
            throw new IllegalArgumentException("不支持的操作: " + action + "，支持的操作: load, unload");
        }
    }

    @GetMapping("/models/status")
    public ResponseEntity<ApiResponse<List<ModelStatus>>> getAllModelStatus() {
        List<ModelStatus> statusList = mcp.getAllModelStatus();
        return ResponseEntity.ok(ApiResponse.success(statusList));
    }

    private ModelResponse convertToResponse(ModelRegistry model) {
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