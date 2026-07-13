package com.jk.amazon2.posting.repository;

import com.jk.amazon2.posting.entity.PostingError;
import com.jk.amazon2.testsupport.RepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PostingErrorRepository 통합 테스트")
class PostingErrorRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private PostingErrorRepository postingErrorRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Long MEMBER_ID = 93L;
    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 7, 6);

    private void insertPostingError(Long memberId, LocalDate targetDate, String dayOfWeek, int retryCount) {
        jdbcTemplate.update(
                "INSERT INTO posting_error (member_id, target_date, day_of_week, error_message, retry_count, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW())",
                memberId, targetDate, dayOfWeek, "test error", retryCount
        );
    }

    @Nested
    @DisplayName("findByMemberAndDateAndDayOfWeek 메서드 테스트")
    class FindByMemberAndDateAndDayOfWeek {

        @Test
        @DisplayName("id 오름차순으로 정렬되어 반환된다 [success]")
        void findByMemberAndDateAndDayOfWeek_orderedByIdAscending() {
            // given: id 순서와 다르게 여러 row를 생성해도 항상 id ASC로 반환되어야 함
            insertPostingError(MEMBER_ID, TARGET_DATE, "mon", 1);
            insertPostingError(MEMBER_ID, TARGET_DATE, "mon", 2);
            insertPostingError(MEMBER_ID, TARGET_DATE, "mon", 3);

            // when
            List<PostingError> result = postingErrorRepository
                    .findByMemberAndDateAndDayOfWeek(MEMBER_ID, TARGET_DATE, "mon");

            // then
            assertThat(result).hasSize(3);
            assertThat(result).isSortedAccordingTo((a, b) -> a.getId().compareTo(b.getId()));
            assertThat(result.get(0).getRetryCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("dayOfWeek이 다른 row는 조회되지 않는다 [success]")
        void findByMemberAndDateAndDayOfWeek_filtersOutOtherDayOfWeek() {
            // given
            insertPostingError(MEMBER_ID, TARGET_DATE, "mon", 1);
            insertPostingError(MEMBER_ID, TARGET_DATE, "tue", 1);
            insertPostingError(MEMBER_ID, TARGET_DATE, "wed", 1);

            // when
            List<PostingError> result = postingErrorRepository
                    .findByMemberAndDateAndDayOfWeek(MEMBER_ID, TARGET_DATE, "tue");

            // then
            assertThat(result)
                    .hasSize(1)
                    .allMatch(error -> error.getDayOfWeek().equals("tue"));
        }

        @Test
        @DisplayName("일치하는 row가 없으면 빈 리스트를 반환한다 [success]")
        void findByMemberAndDateAndDayOfWeek_returnsEmptyWhenNoMatch() {
            // when
            List<PostingError> result = postingErrorRepository
                    .findByMemberAndDateAndDayOfWeek(MEMBER_ID, TARGET_DATE, "sun");

            // then
            assertThat(result).isEmpty();
        }
    }
}
