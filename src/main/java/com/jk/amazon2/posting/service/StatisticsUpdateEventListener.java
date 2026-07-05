package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.event.StatisticsUpdateEvent;
import com.jk.amazon2.posting.repository.MonthlyPostingSummaryRepository;
import com.jk.amazon2.posting.repository.PostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsUpdateEventListener {

    private final PostingRepository postingRepository;
    private final MonthlyPostingSummaryRepository summaryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(StatisticsUpdateEvent event) {
        LocalDate monthStart = event.weekStartDate().withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        try {
            int total = postingRepository.sumTotalByMemberAndMonth(event.memberId(), monthStart, monthEnd);
            summaryRepository.upsertTotal(event.memberId(), monthStart, total);
        } catch (Exception e) {
            log.error("월별 통계 갱신 실패 - memberId={}, month={}", event.memberId(), monthStart, e);
        }
    }
}
