package com.jk.amazon2.member.repository;

import com.jk.amazon2.member.dto.MemberCommand;
import com.jk.amazon2.member.dto.MemberResult;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static com.jk.amazon2.member.entity.QMember.member;
import static com.jk.amazon2.category.entity.QCategory.category;

@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<MemberResult.Summary> findMembers(MemberCommand.Search condition, Pageable pageable) {
        List<MemberResult.Summary> content = queryFactory
                .select(Projections.constructor(
                        MemberResult.Summary.class,
                        member.nickname,
                        category.name,
                        member.createdAt,
                        member.deleted
                ))
                .from(member)
                .leftJoin(category).on(
                        member.categoryCode.eq(category.code),
                        category.deleted.eq(false)
                )
                .where(
                        nicknameCondition(condition.getNickname()),
                        categoryCodeCondition(condition.getCategoryCode()),
                        deletedCondition(condition.getDeleted())
                )
                .orderBy(member.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(member.count())
                .from(member)
                .leftJoin(category).on(
                        member.categoryCode.eq(category.code),
                        category.deleted.eq(false)
                )
                .where(
                        nicknameCondition(condition.getNickname()),
                        categoryCodeCondition(condition.getCategoryCode()),
                        deletedCondition(condition.getDeleted())
                );
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression nicknameCondition(String nickname) {
        return nickname != null ? member.nickname.contains(nickname) : null;
    }

    private BooleanExpression categoryCodeCondition(String categoryCode) {
        return categoryCode != null ? member.categoryCode.eq(categoryCode) : null;
    }

    private BooleanExpression deletedCondition(Boolean deleted) {
        return deleted != null ? member.deleted.eq(deleted) : null;
    }
}
