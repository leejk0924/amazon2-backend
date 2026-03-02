package com.jk.amazon2.controller;

import com.jk.amazon2.controller.dto.CategoryRequest;
import com.jk.amazon2.controller.dto.CategoryResponse;
import com.jk.amazon2.controller.spec.CategoryApiSpec;
import com.jk.amazon2.service.CategoryService;
import com.jk.amazon2.service.dto.CategoryCommand;
import com.jk.amazon2.service.dto.CategoryResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CategoryController implements CategoryApiSpec {
    private final CategoryService categoryService;

    @Override
    @GetMapping("/categories/{code}")
    public ResponseEntity<CategoryResponse.Info> getCategory(
            @PathVariable String code
    ) {
        CategoryResult.Info result = categoryService.getCategory(code);
        var response = CategoryResponse.Info.from(result);

        return ResponseEntity.
                status(HttpStatus.OK)
                .body(response);
    }

    @Override
    @GetMapping("/categories")
    public ResponseEntity<Page<CategoryResponse.Info>> getCategories(
            @Valid @ModelAttribute CategoryRequest.CategorySearchCondition searchCondition,
            @PageableDefault(size = 10, sort = "code") Pageable pageable
    ) {
        Page<CategoryResponse.Info> result = categoryService.getCategories(searchCondition, pageable).map(CategoryResponse.Info::from);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(result);
    }

    @Override
    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse.CategoryCreateDto> createCategory(
            @Valid @RequestBody CategoryRequest.CategoryCreateDto categoryDto
    ) {
        var categoryCommand = CategoryCommand.Create.from(categoryDto);

        CategoryResult.Detail result = categoryService.create(categoryCommand);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CategoryResponse.CategoryCreateDto.from(result));
    }

    @Override
    @PutMapping("/categories/{code}")
    public ResponseEntity<CategoryResponse.CategoryUpdateDto> updateCategory(
            @PathVariable String code,
            @Valid @RequestBody CategoryRequest.CategoryUpdateDto categoryUpdateDto
    ) {
        CategoryResult.Detail updatedCategory = categoryService.update(
                CategoryCommand.Update.of(code,
                        categoryUpdateDto.name(),
                        categoryUpdateDto.description())
        );
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(CategoryResponse.CategoryUpdateDto.from(updatedCategory));
    }

    @Override
    @DeleteMapping("/categories/{code}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable String code
    ) {
        categoryService.delete(code);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
