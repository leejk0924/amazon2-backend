package com.jk.amazon2.repository;

import com.jk.amazon2.controller.dto.CategoryRequest;
import com.jk.amazon2.entity.Category;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.jk.amazon2.entity.QCategory.category;

@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryQueryRepository{
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Category> search(CategoryRequest.CategorySearchCondition condition, Pageable pageable) {
        List<Category> content = queryFactory
                .selectFrom(category)
                .where(
                        nameContains(condition.name()),
                        codeContains(condition.code())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(category.count())
                .from(category)
                .where(
                        nameContains(condition.name()),
                        codeContains(condition.code())
                );
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression nameContains(String name) {
        return StringUtils.hasText(name) ? category.name.contains(name) : null;
    }

    private BooleanExpression codeContains(String code) {
        return StringUtils.hasText(code) ? category.code.contains(code) : null;
    }
}
