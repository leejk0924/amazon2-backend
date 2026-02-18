package com.jk.amazon2.controller;

import com.jk.amazon2.controller.dto.CategoryRequest;
import com.jk.amazon2.controller.dto.CategoryResponse;
import com.jk.amazon2.controller.spec.CategoryApiSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CategoryController implements CategoryApiSpec {

    @Override
    @GetMapping("/categories")
    public ResponseEntity<Page<CategoryResponse.CategoryDto>> getCategories(
            CategoryRequest.CategorySearchCondition searchCondition,
            @PageableDefault(size = 10, sort = "name") Pageable pageable
    ) {
        List<CategoryResponse.CategoryDto> content = List.of(
                new CategoryResponse.CategoryDto("ELECTRONIC", "전자기기"),
                new CategoryResponse.CategoryDto("BEAUTY", "뷰티"),
                new CategoryResponse.CategoryDto("FASHION", "패션")
        );

        Page<CategoryResponse.CategoryDto> data = new PageImpl<>(
                content,
                pageable,
                content.size()
        ).map(c -> new CategoryResponse.CategoryDto(c.code(), c.name()));

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(data);
    }

    @Override
    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse.CategoryDto> createCategory(
            @RequestBody CategoryRequest.CategoryDto categoryDto
    ) {
        CategoryResponse.CategoryDto savedCategory = new CategoryResponse.CategoryDto(
                categoryDto.code(),
                categoryDto.name()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedCategory);
    }

    @Override
    @PutMapping("/categories/{id}")
    public ResponseEntity<CategoryResponse.CategoryDto> updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryRequest.CategoryDto categoryDto
    ) {
        CategoryResponse.CategoryDto updatedCategory = new CategoryResponse.CategoryDto(
                categoryDto.code(),
                categoryDto.name()
        );
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(updatedCategory);
    }

    @Override
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id
    ) {
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
