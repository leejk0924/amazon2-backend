package com.jk.amazon2.repository.spec;

import com.jk.amazon2.controller.dto.CategoryRequest;
import com.jk.amazon2.entity.Category;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class CategorySpecification {

    public static Specification<Category> searchWith(CategoryRequest.CategorySearchCondition condition) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(condition.code())) {
                predicates.add(criteriaBuilder.like(root.get("code"), "%" + condition.code() + "%"));
            }

            if (StringUtils.hasText(condition.name())) {
                predicates.add(criteriaBuilder.like(root.get("name"), "%" + condition.name() + "%"));
            }
            
            if (predicates.isEmpty()) {
                return null;
            }
            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }
}
