package com.jk.amazon2.posting.integration;

import com.jk.amazon2.member.entity.Member;
import com.jk.amazon2.member.repository.MemberRepository;
import com.jk.amazon2.posting.entity.BatchExecution;
import com.jk.amazon2.posting.repository.BatchExecutionRepository;
import com.jk.amazon2.posting.repository.PostingRepository;
import com.jk.amazon2.posting.service.BatchService;
import com.jk.amazon2.posting.service.NaverBlogScraper;
import com.jk.amazon2.posting.service.RateLimiter;
import com.jk.amazon2.posting.service.ScrapingResult;
import com.jk.amazon2.testsupport.TestContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestContainerConfig.class)
@DisplayName("배치 실행 통합 테스트")
class PostingBatchIntegrationTest {

    @Autowired
    private BatchService batchService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PostingRepository postingRepository;

    @Autowired
    private BatchExecutionRepository batchExecutionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private NaverBlogScraper scraper;

    @MockitoBean
    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        when(scraper.scrapePostingCount(any(), any())).thenReturn(new ScrapingResult.Success<>(5));
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
        jdbcTemplate.execute("TRUNCATE TABLE posting_error");
        jdbcTemplate.execute("TRUNCATE TABLE posting_dead_letter");
        jdbcTemplate.execute("TRUNCATE TABLE posting");
        jdbcTemplate.execute("TRUNCATE TABLE batch_execution");
        jdbcTemplate.execute("TRUNCATE TABLE member");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
    }

    @Nested
    @DisplayName("BatchExecution 상태 검증")
    class BatchExecutionStatus {

        @Test
        @DisplayName("[통합] 배치 실행 시 BatchExecution 생성 및 COMPLETED 상태 확인 [success]")
        void batchExecution_created_and_completed() {
            // given
            memberRepository.save(Member.of("test-user-1", "test-name-1", "TECH"));
            LocalDate startDate = LocalDate.of(2026, 6, 9);
            LocalDate endDate = LocalDate.of(2026, 6, 9);

            // when
            Long batchId = batchService.executeBatch(startDate, endDate, "TEST");

            // then
            Optional<BatchExecution> result = batchExecutionRepository.findById(batchId);
            assertThat(result).isPresent();
            BatchExecution execution = result.get();
            assertSoftly(softly -> {
                softly.assertThat(execution.getBatchType()).isEqualTo("TEST");
                softly.assertThat(execution.getStartDate()).isEqualTo(startDate);
                softly.assertThat(execution.getEndDate()).isEqualTo(endDate);
                softly.assertThat(execution.getStatus()).isEqualTo("COMPLETED");
                softly.assertThat(execution.getStartedAt()).isNotNull();
                softly.assertThat(execution.getCompletedAt()).isNotNull();
            });
        }

        @Test
        @DisplayName("[통합] 배치 완료 후 completedAt이 startedAt 이후로 설정 [success]")
        void batchExecution_completedAt_after_startedAt() {
            // given
            memberRepository.save(Member.of("test-user-2", "test-name-2", "TECH"));

            // when
            Long batchId = batchService.executeBatch(
                    LocalDate.of(2026, 6, 9), LocalDate.of(2026, 6, 9), "TEST");

            // then
            BatchExecution execution = batchExecutionRepository.findById(batchId).orElseThrow();
            assertThat(execution.getCompletedAt()).isAfterOrEqualTo(execution.getStartedAt());
        }
    }

    @Nested
    @DisplayName("BatchExecution 카운트 검증")
    class BatchExecutionCount {

        @Test
        @DisplayName("[통합] 단일 회원 3일 배치 실행 후 totalCount = 3 [success]")
        void batchExecution_totalCount_single_member() {
            // given
            memberRepository.save(Member.of("test-user-3", "test-name-3", "TECH"));

            // when
            Long batchId = batchService.executeBatch(
                    LocalDate.of(2026, 6, 9), LocalDate.of(2026, 6, 11), "TEST"); // 3일

            // then - 1명 × 3일 = 3
            BatchExecution execution = batchExecutionRepository.findById(batchId).orElseThrow();
            assertSoftly(softly -> {
                softly.assertThat(execution.getTotalCount()).isEqualTo(3);
                softly.assertThat(execution.getSuccessCount()).isEqualTo(3);
                softly.assertThat(execution.getFailedCount()).isEqualTo(0);
            });
        }

        @Test
        @DisplayName("[통합] 다중 회원 1일 배치 실행 후 totalCount = 2 [success]")
        void batchExecution_totalCount_multiple_members() {
            // given
            memberRepository.save(Member.of("test-user-multi-1", "test-name-4", "TECH"));
            memberRepository.save(Member.of("test-user-multi-2", "test-name-5", "TECH"));

            // when
            Long batchId = batchService.executeBatch(
                    LocalDate.of(2026, 6, 9), LocalDate.of(2026, 6, 9), "TEST"); // 1일

            // then - 2명 × 1일 = 2
            BatchExecution execution = batchExecutionRepository.findById(batchId).orElseThrow();
            assertSoftly(softly -> {
                softly.assertThat(execution.getStatus()).isEqualTo("COMPLETED");
                softly.assertThat(execution.getTotalCount()).isEqualTo(2);
                softly.assertThat(execution.getSuccessCount()).isEqualTo(2);
            });
        }
    }

    @Nested
    @DisplayName("DB 저장 검증")
    class DatabasePersistence {

        @Test
        @DisplayName("[통합] 배치 실행 후 BatchExecution DB 직접 조회 [success]")
        void batchExecution_saved_to_database() {
            // given
            memberRepository.save(Member.of("test-user-4", "test-name-4", "TECH"));

            // when
            Long batchId = batchService.executeBatch(
                    LocalDate.of(2026, 6, 9), LocalDate.of(2026, 6, 10), "MANUAL");

            // then
            String status = jdbcTemplate.queryForObject(
                    "SELECT status FROM batch_execution WHERE id = ?", String.class, batchId);
            String batchType = jdbcTemplate.queryForObject(
                    "SELECT batch_type FROM batch_execution WHERE id = ?", String.class, batchId);

            assertThat(status).isEqualTo("COMPLETED");
            assertThat(batchType).isEqualTo("MANUAL");
        }

        @Test
        @DisplayName("[통합] 배치 실행 후 Posting 데이터 생성 확인 [success]")
        void posting_data_created_after_batch() {
            // given
            memberRepository.save(Member.of("test-user-5", "test-name-5", "TECH"));

            // when
            batchService.executeBatch(
                    LocalDate.of(2026, 6, 9), LocalDate.of(2026, 6, 9), "TEST");

            // then
            assertThat(postingRepository.findAll()).isNotEmpty();
        }
    }
}
