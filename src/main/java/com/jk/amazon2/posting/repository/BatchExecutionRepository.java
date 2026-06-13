package com.jk.amazon2.posting.repository;

import com.jk.amazon2.posting.entity.BatchExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BatchExecutionRepository extends JpaRepository<BatchExecution, Long> {

    @Query("SELECT be FROM BatchExecution be WHERE be.status = 'IN_PROGRESS' ORDER BY be.startedAt DESC LIMIT 1")
    Optional<BatchExecution> findCurrentExecution();

    @Query("SELECT be FROM BatchExecution be ORDER BY be.startedAt DESC LIMIT 1")
    Optional<BatchExecution> findLatestExecution();
}
