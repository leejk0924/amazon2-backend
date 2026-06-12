package com.jk.amazon2.repository;

import com.jk.amazon2.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    boolean existsByNickname(String nickname);
}
