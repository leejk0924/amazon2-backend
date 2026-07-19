package com.jk.amazon2.posting.repository;

import com.jk.amazon2.posting.entity.BatchExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface BatchExecutionRepository extends JpaRepository<BatchExecution, Long> {

    @Query("SELECT be FROM BatchExecution be WHERE be.status = 'IN_PROGRESS' ORDER BY be.startedAt DESC LIMIT 1")
    Optional<BatchExecution> findCurrentExecution();

    @Query("SELECT be FROM BatchExecution be ORDER BY be.startedAt DESC LIMIT 1")
    Optional<BatchExecution> findLatestExecution();

    @Query("SELECT be FROM BatchExecution be WHERE be.status = 'COMPLETED' " +
        "AND be.startDate <= :weekStartDate AND be.endDate >= :weekStartDate " +
        "ORDER BY be.startedAt DESC LIMIT 1")
    Optional<BatchExecution> findCompletedExecutionByWeek(@Param("weekStartDate") LocalDate weekStartDate);
}
