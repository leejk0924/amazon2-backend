package com.jk.amazon2.member.repository;

import com.jk.amazon2.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    boolean existsByNickname(String nickname);
    Optional<Member> findByNickname(String nickname);

    @Query("SELECT m FROM Member m WHERE m.deleted = false AND (:memberId IS NULL OR m.id = :memberId)")
    Page<Member> findActiveMembers(@Param("memberId") Long memberId, Pageable pageable);

    long countByDeletedFalse();

    List<Member> findAllByDeletedFalse();
}
