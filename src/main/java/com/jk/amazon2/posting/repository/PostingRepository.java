package com.jk.amazon2.posting.repository;

import com.jk.amazon2.posting.entity.Posting;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface PostingRepository extends JpaRepository<Posting, Long> {

    @Query("SELECT p FROM Posting p WHERE p.memberId = :memberId AND p.weekStartDate = :weekStartDate")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Posting> findByMemberIdAndWeekStartDateWithLock(
        @Param("memberId") Long memberId,
        @Param("weekStartDate") LocalDate weekStartDate
    );
}
