package com.jk.amazon2.member.repository;

import com.jk.amazon2.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    boolean existsByNickname(String nickname);
    Optional<Member> findByNickname(String nickname);
}
