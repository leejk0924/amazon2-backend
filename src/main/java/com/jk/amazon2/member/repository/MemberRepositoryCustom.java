package com.jk.amazon2.member.repository;

import com.jk.amazon2.service.dto.MemberCommand;
import com.jk.amazon2.service.dto.MemberResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberRepositoryCustom {
    Page<MemberResult.Summary> findMembers(MemberCommand.Search condition, Pageable pageable);
}
