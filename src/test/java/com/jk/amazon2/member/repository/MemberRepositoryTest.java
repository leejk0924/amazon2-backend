package com.jk.amazon2.member.repository;

import com.jk.amazon2.member.repository.MemberRepository;
import com.jk.amazon2.service.dto.MemberCommand;
import com.jk.amazon2.service.dto.MemberResult;
import com.jk.amazon2.testsupport.RepositoryTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.params.provider.Arguments.of;

@DisplayName("MemberRepository 통합 테스트")
class MemberRepositoryTest extends RepositoryTestSupport {
    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Nested
    @DisplayName("findMembers 메서드 테스트")
    class FindMembers {

        @BeforeEach
        void setUp() {
            String categoryInsertSql = "INSERT INTO blog_category (code, name, description, created_by, deleted) VALUES (?, ?, ?, ?, ?)";
            String memberInsertSql = "INSERT INTO member (nickname, category_code, created_by, updated_by, deleted) VALUES (?, ?, ?, ?, ?)";

            jdbcTemplate.batchUpdate(categoryInsertSql, List.of(
                    new Object[]{"DEV", "개발", "개발 팀", "test", false},
                    new Object[]{"DESIGN", "디자인", "디자인 팀", "test", false},
                    new Object[]{"OLD", "구팀", "삭제된 팀", "test", true}
            ));

            jdbcTemplate.batchUpdate(memberInsertSql, List.of(
                    new Object[]{"dev_user1", "DEV", "test", "test", false},
                    new Object[]{"dev_user2", "DEV", "test", "test", false},
                    new Object[]{"design_user", "DESIGN", "test", "test", false},
                    new Object[]{"deleted_user", "DEV", "test", "test", true}
            ));
        }

        @Test
        @DisplayName("Category와 JOIN하여 categoryName 반환 [success]")
        void findMembers_with_join_category() {
            // given
            MemberCommand.Search command = new MemberCommand.Search(null, null, null);
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<MemberResult.Summary> result = memberRepository.findMembers(command, pageable);

            // then
            assertThat(result.getContent())
                    .hasSize(4)
                    .allMatch(summary -> summary.categoryName() != null);
        }

        @Test
        @DisplayName("null 조건이면 전체 조회 [success]")
        void findMembers_with_null_conditions() {
            // given
            MemberCommand.Search command = new MemberCommand.Search(null, null, null);
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<MemberResult.Summary> result = memberRepository.findMembers(command, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(4);
        }

        @DisplayName("nickname으로 LIKE 검색 [success]")
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("provideNicknameSearchCases")
        void findMembers_with_nickname_filter(String scenario, String searchNickname, int expectedCount) {
            // given
            MemberCommand.Search command = new MemberCommand.Search(searchNickname, null, null);
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<MemberResult.Summary> result = memberRepository.findMembers(command, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(expectedCount);
        }

        private static Stream<Arguments> provideNicknameSearchCases() {
            return Stream.of(
                    of("dev_user 부분 검색", "dev", 2),
                    of("dev_user1 전체 검색", "dev_user1", 1),
                    of("design 검색", "design", 1),
                    of("존재하지 않는 검색", "nonexistent", 0)
            );
        }

        @Test
        @DisplayName("categoryCode로 정확 검색 [success]")
        void findMembers_with_categoryCode_filter() {
            // given
            MemberCommand.Search command = new MemberCommand.Search(null, "DEV", null);
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<MemberResult.Summary> result = memberRepository.findMembers(command, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getContent())
                    .allMatch(summary -> summary.categoryName().equals("개발"));
        }

        @DisplayName("deleted 필터 - active/deleted/null [success]")
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("provideDeletedFilterCases")
        void findMembers_with_deleted_filter(String scenario, Boolean deletedFilter, int expectedCount) {
            // given
            MemberCommand.Search command = new MemberCommand.Search(null, null, deletedFilter);
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<MemberResult.Summary> result = memberRepository.findMembers(command, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(expectedCount);
        }

        private static Stream<Arguments> provideDeletedFilterCases() {
            return Stream.of(
                    of("active만 조회 (deleted=false)", false, 3),
                    of("deleted만 조회 (deleted=true)", true, 1),
                    of("전체 조회 (deleted=null)", null, 4)
            );
        }

        @Test
        @DisplayName("페이지네이션 동작 확인 [success]")
        void findMembers_pagination_and_ordering() {
            // given
            MemberCommand.Search command = new MemberCommand.Search(null, null, null);
            Pageable pageable = PageRequest.of(0, 2);

            // when
            Page<MemberResult.Summary> result = memberRepository.findMembers(command, pageable);

            // then
            assertSoftly(softly -> {
                softly.assertThat(result.getNumber()).isEqualTo(0);
                softly.assertThat(result.getSize()).isEqualTo(2);
                softly.assertThat(result.getTotalElements()).isEqualTo(4);
                softly.assertThat(result.getTotalPages()).isEqualTo(2);
                softly.assertThat(result.getContent()).hasSize(2);
            });
        }

        @Test
        @DisplayName("createdAt DESC 정렬 확인 [success]")
        void findMembers_ordering_by_createdAt() {
            // given
            MemberCommand.Search command = new MemberCommand.Search(null, null, null);
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<MemberResult.Summary> result = memberRepository.findMembers(command, pageable);

            // then
            assertThat(result.getContent())
                    .isSortedAccordingTo((s1, s2) -> s2.createdAt().compareTo(s1.createdAt()));
        }

        @Test
        @DisplayName("복합 검색 조건 - nickname + categoryCode [success]")
        void findMembers_with_combined_filters() {
            // given
            MemberCommand.Search command = new MemberCommand.Search("dev", "DEV", null);
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<MemberResult.Summary> result = memberRepository.findMembers(command, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent())
                    .allMatch(summary ->
                        summary.nickname().contains("dev") && summary.categoryName().equals("개발")
                    );
        }
    }
}
