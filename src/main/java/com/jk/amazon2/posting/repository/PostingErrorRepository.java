package com.jk.amazon2.posting.repository;

import com.jk.amazon2.posting.entity.PostingError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PostingErrorRepository extends JpaRepository<PostingError, Long> {

    Page<PostingError> findByRetryCountLessThan(Integer retryCount, Pageable pageable);

    Page<PostingError> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT pe FROM PostingError pe WHERE pe.retryCount < 3 ORDER BY pe.createdAt ASC")
    List<PostingError> findRetryableErrors();

    @Query("SELECT pe FROM PostingError pe WHERE pe.memberId = :memberId AND pe.targetDate = :targetDate")
    List<PostingError> findByMemberAndDate(Long memberId, LocalDate targetDate);
}
