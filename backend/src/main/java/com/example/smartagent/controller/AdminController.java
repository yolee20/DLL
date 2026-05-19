package com.example.smartagent.controller;

import com.example.smartagent.dto.request.ModelRegisterRequest;
import com.example.smartagent.dto.response.ApiResponse;
import com.example.smartagent.dto.response.ModelResponse;
import com.example.smartagent.model.strategy.ModelStatus;
import com.example.smartagent.service.management.ModelManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "模型管理", description = "模型注册、加载、卸载等管理接口")
public class AdminController {

    private final ModelManagementService modelManagementService;

    @Operation(summary = "获取所有模型", description = "查询系统中所有已注册的模型列表")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/models")
    public ResponseEntity<ApiResponse<List<ModelResponse>>> getAllModels() {
        List<ModelResponse> models = modelManagementService.getAllModels();
        return ResponseEntity.ok(ApiResponse.success(models));
    }

    @Operation(summary = "获取指定模型", description = "根据模型名称查询模型详情")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "模型不存在")
    })
    @GetMapping("/models/{name}")
    public ResponseEntity<ApiResponse<ModelResponse>> getModel(
            @Parameter(description = "模型名称") @PathVariable String name) {
        ModelResponse model = modelManagementService.getModel(name);
        return ResponseEntity.ok(ApiResponse.success(model));
    }

    @Operation(summary = "注册新模型", description = "向系统注册一个新的AI模型")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "创建成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数无效")
    })
    @PostMapping("/models")
    public ResponseEntity<ApiResponse<ModelResponse>> createModel(
            @Valid @RequestBody ModelRegisterRequest request) {
        ModelResponse model = modelManagementService.createModel(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("模型创建成功", model));
    }

    @Operation(summary = "更新模型信息", description = "更新已注册模型的基本信息")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "模型不存在")
    })
    @PutMapping("/models/{name}")
    public ResponseEntity<ApiResponse<ModelResponse>> updateModel(
            @Parameter(description = "模型名称") @PathVariable String name,
            @Valid @RequestBody ModelRegisterRequest request) {
        ModelResponse model = modelManagementService.updateModel(name, request);
        return ResponseEntity.ok(ApiResponse.success("模型更新成功", model));
    }

    @Operation(summary = "删除模型", description = "从系统中删除指定的模型")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "模型不存在")
    })
    @DeleteMapping("/models/{name}")
    public ResponseEntity<ApiResponse<Void>> deleteModel(
            @Parameter(description = "模型名称") @PathVariable String name) {
        modelManagementService.deleteModel(name);
        return ResponseEntity.ok(ApiResponse.success("模型删除成功", null));
    }

    @Operation(summary = "加载模型", description = "将模型加载到内存中以便使用")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "加载成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "模型不存在")
    })
    @PutMapping("/models/{name}/load")
    public ResponseEntity<ApiResponse<Void>> loadModel(
            @Parameter(description = "模型名称") @PathVariable String name) {
        modelManagementService.loadModel(name);
        return ResponseEntity.ok(ApiResponse.success("模型加载成功", null));
    }

    @Operation(summary = "卸载模型", description = "将模型从内存中卸载以释放资源")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "卸载成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "模型不存在")
    })
    @PutMapping("/models/{name}/unload")
    public ResponseEntity<ApiResponse<Void>> unloadModel(
            @Parameter(description = "模型名称") @PathVariable String name) {
        modelManagementService.unloadModel(name);
        return ResponseEntity.ok(ApiResponse.success("模型卸载成功", null));
    }

    @Operation(summary = "获取所有模型状态", description = "查询系统中所有模型当前的加载状态")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/models/status")
    public ResponseEntity<ApiResponse<List<ModelStatus>>> getAllModelStatus() {
        List<ModelStatus> statusList = modelManagementService.getAllModelStatus();
        return ResponseEntity.ok(ApiResponse.success(statusList));
    }
}
