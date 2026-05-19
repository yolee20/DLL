package com.example.smartagent.controller;

import com.example.smartagent.dto.request.QACategoryRequest;
import com.example.smartagent.dto.request.QAPairRequest;
import com.example.smartagent.dto.response.ApiResponse;
import com.example.smartagent.dto.response.QACategoryResponse;
import com.example.smartagent.dto.response.QAPairResponse;
import com.example.smartagent.service.qa.QAService;
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
@RequestMapping("/api/qa")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "知识库管理", description = "问答对和分类的CRUD管理接口")
public class QAController {

    private final QAService qaService;

    @Operation(summary = "获取所有问答对", description = "查询系统中所有已注册的问答对")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/pairs")
    public ResponseEntity<ApiResponse<List<QAPairResponse>>> getAllQAPairs() {
        List<QAPairResponse> pairs = qaService.getAllQAPairs();
        return ResponseEntity.ok(ApiResponse.success(pairs));
    }

    @Operation(summary = "获取指定问答对", description = "根据ID查询问答对详情")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "问答对不存在")
    })
    @GetMapping("/pairs/{id}")
    public ResponseEntity<ApiResponse<QAPairResponse>> getQAPair(
            @Parameter(description = "问答对ID") @PathVariable Long id) {
        QAPairResponse pair = qaService.getQAPair(id);
        return ResponseEntity.ok(ApiResponse.success(pair));
    }

    @Operation(summary = "创建问答对", description = "向知识库添加一个新的问答对")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "创建成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数无效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "指定的分类不存在")
    })
    @PostMapping("/pairs")
    public ResponseEntity<ApiResponse<QAPairResponse>> createQAPair(
            @Valid @RequestBody QAPairRequest request) {
        QAPairResponse pair = qaService.createQAPair(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("问答对创建成功", pair));
    }

    @Operation(summary = "更新问答对", description = "更新已存在问答对的内容")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "问答对或分类不存在")
    })
    @PutMapping("/pairs/{id}")
    public ResponseEntity<ApiResponse<QAPairResponse>> updateQAPair(
            @Parameter(description = "问答对ID") @PathVariable Long id,
            @Valid @RequestBody QAPairRequest request) {
        QAPairResponse pair = qaService.updateQAPair(id, request);
        return ResponseEntity.ok(ApiResponse.success("问答对更新成功", pair));
    }

    @Operation(summary = "删除问答对", description = "从知识库中删除指定的问答对")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "问答对不存在")
    })
    @DeleteMapping("/pairs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteQAPair(
            @Parameter(description = "问答对ID") @PathVariable Long id) {
        qaService.deleteQAPair(id);
        return ResponseEntity.ok(ApiResponse.success("问答对删除成功", null));
    }

    @Operation(summary = "获取所有分类", description = "查询系统中所有问答分类")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<QACategoryResponse>>> getAllCategories() {
        List<QACategoryResponse> categories = qaService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @Operation(summary = "获取指定分类", description = "根据ID查询分类详情")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "分类不存在")
    })
    @GetMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<QACategoryResponse>> getCategory(
            @Parameter(description = "分类ID") @PathVariable Long id) {
        QACategoryResponse category = qaService.getCategory(id);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @Operation(summary = "创建分类", description = "向系统添加一个新的问答分类")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "创建成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数无效")
    })
    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<QACategoryResponse>> createCategory(
            @Valid @RequestBody QACategoryRequest request) {
        QACategoryResponse category = qaService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("分类创建成功", category));
    }

    @Operation(summary = "更新分类", description = "更新已存在分类的名称和描述")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "分类不存在")
    })
    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<QACategoryResponse>> updateCategory(
            @Parameter(description = "分类ID") @PathVariable Long id,
            @Valid @RequestBody QACategoryRequest request) {
        QACategoryResponse category = qaService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("分类更新成功", category));
    }

    @Operation(summary = "删除分类", description = "从系统中删除指定的分类")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "分类不存在")
    })
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @Parameter(description = "分类ID") @PathVariable Long id) {
        qaService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("分类删除成功", null));
    }
}
