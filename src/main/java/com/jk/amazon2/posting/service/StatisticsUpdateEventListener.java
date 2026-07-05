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

    // AFTER_COMMIT 시점엔 원본 트랜잭션이 이미 종료되어 REQUIRES_NEW로 별도 트랜잭션을 열어야 함
    // 델타 누적이 아니라 해당 월 전체를 재합산해 절대값으로 덮어씀 -> 이벤트 순서가 뒤바뀌어도
    // 마지막에 커밋한 쪽이 그 시점의 전체 합계를 읽으므로 결과가 항상 수렴함(자가 치유)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(StatisticsUpdateEvent event) {
        LocalDate monthStart = event.weekStartDate().withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        try {
            int total = postingRepository.sumTotalByMemberAndMonth(event.memberId(), monthStart, monthEnd);
            summaryRepository.upsertTotal(event.memberId(), monthStart, total);
        } catch (Exception e) {
            // AFTER_COMMIT 리스너의 예외는 Spring이 그대로 삼키므로 최소한 원인 추적이 가능하도록 로그 남김
            // (재시도는 하지 않음 - 이후 같은 회원/월에 대한 저장이 다시 발생하면 자연히 재계산됨)
            log.error("월별 통계 갱신 실패 - memberId={}, month={}", event.memberId(), monthStart, e);
        }
    }
}
