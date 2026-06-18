package com.jk.amazon2.member.service;

import com.jk.amazon2.member.dto.MemberRequest;
import com.jk.amazon2.member.entity.Member;
import com.jk.amazon2.member.exception.MemberErrorCode;
import com.jk.amazon2.common.exception.RestApiException;
import com.jk.amazon2.common.port.CategoryValidationPort;
import com.jk.amazon2.member.repository.MemberRepository;
import com.jk.amazon2.member.dto.MemberCommand;
import com.jk.amazon2.member.dto.MemberResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final CategoryValidationPort categoryValidationPort;

    @Transactional
    public MemberResult.Detail create(MemberCommand.Create command) {
        if (memberRepository.existsByNickname(command.getNickname())) {
            throw new RestApiException(MemberErrorCode.MEMBER_NICKNAME_ALREADY_EXISTS);
        }

        categoryValidationPort.validateCategoryExists(command.getCategoryCode());

        Member member = Member.of(
                command.getNickname(),
                command.getCategoryCode()
        );

        Member savedMember = memberRepository.save(member);
        return MemberResult.Detail.from(savedMember);
    }

    @Transactional
    public MemberResult.Update update(MemberCommand.Update command) {
        Member member = memberRepository
                .findById(command.getId())
                .orElseThrow(() -> new RestApiException(MemberErrorCode.MEMBER_NOT_FOUND));

        categoryValidationPort.validateCategoryExists(command.getCategoryCode());

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

    @Transactional
    public void hardDelete(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RestApiException(MemberErrorCode.MEMBER_NOT_FOUND));
        if (!member.isDeleted()) {
            throw new RestApiException(MemberErrorCode.MEMBER_NOT_DELETED);
        }
        memberRepository.delete(member);
    }
}
