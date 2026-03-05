package com.jk.amazon2.service;

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
}
