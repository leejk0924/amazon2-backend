package com.jk.amazon2.service;

import com.jk.amazon2.controller.dto.MemberRequest;
import com.jk.amazon2.entity.Category;
import com.jk.amazon2.entity.Member;
import com.jk.amazon2.exception.CategoryErrorCode;
import com.jk.amazon2.exception.MemberErrorCode;
import com.jk.amazon2.exception.RestApiException;
import com.jk.amazon2.repository.CategoryRepository;
import com.jk.amazon2.repository.MemberRepository;
import com.jk.amazon2.service.dto.MemberCommand;
import com.jk.amazon2.service.dto.MemberResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public MemberResult.Detail create(MemberCommand.Create command) {
        if (memberRepository.existsByNickname(command.getNickname())) {
            throw new RestApiException(MemberErrorCode.MEMBER_NICKNAME_ALREADY_EXISTS);
        }

        Category category = categoryRepository.findByCodeAndDeletedFalse(command.getCategoryCode())
                .orElseThrow(() -> new RestApiException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        Member member = Member.of(
                command.getNickname(),
                category.getCode()
        );

        Member savedMember = memberRepository.save(member);
        return MemberResult.Detail.from(savedMember);
    }

    @Transactional
    public MemberResult.Update update(MemberCommand.Update command) {
        Member member = memberRepository
                .findById(command.getId())
                .orElseThrow(() -> new RestApiException(MemberErrorCode.MEMBER_NOT_FOUND));

        categoryRepository
                .findByCodeAndDeletedFalse(command.getCategoryCode())
                .orElseThrow(() -> new RestApiException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        member.update(command.getNickname(), command.getCategoryCode());
        return MemberResult.Update.of(member.getNickname(), member.getCategoryCode());
    }

    @Transactional
    public void delete(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RestApiException(MemberErrorCode.MEMBER_NOT_FOUND));
        member.softDelete();
    }

    @Transactional(readOnly = true)
    public MemberResult.Detail findById(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RestApiException(MemberErrorCode.MEMBER_NOT_FOUND));
        return MemberResult.Detail.from(member);
    }

    @Transactional(readOnly = true)
    public Page<MemberResult.Summary> findMembers(MemberRequest.MemberSearchCondition searchCondition, Pageable pageable) {
        MemberCommand.Search command = MemberCommand.Search.from(searchCondition);
        return memberRepository.findMembers(command, pageable);
    }
}
