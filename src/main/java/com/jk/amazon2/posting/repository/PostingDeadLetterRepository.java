package com.jk.amazon2.posting.repository;

import com.jk.amazon2.posting.entity.PostingDeadLetter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface PostingDeadLetterRepository extends JpaRepository<PostingDeadLetter, Long> {

    Page<PostingDeadLetter> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByMemberId(Long memberId);

    void deleteByMemberIdAndTargetDate(Long memberId, LocalDate targetDate);
}
