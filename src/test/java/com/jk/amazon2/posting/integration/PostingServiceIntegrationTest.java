package com.jk.amazon2.posting.integration;

import com.jk.amazon2.member.entity.Member;
import com.jk.amazon2.member.repository.MemberRepository;
import com.jk.amazon2.posting.dto.PostingResponse;
import com.jk.amazon2.posting.entity.Posting;
import com.jk.amazon2.posting.repository.PostingRepository;
import com.jk.amazon2.posting.service.PostingService;
import com.jk.amazon2.testsupport.TestContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestContainerConfig.class)
@DisplayName("[통합] 포스팅 조회 서비스 통합 테스트")
class PostingServiceIntegrationTest {

    @Autowired
    private PostingService postingService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PostingRepository postingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
        jdbcTemplate.execute("TRUNCATE TABLE posting");
        jdbcTemplate.execute("TRUNCATE TABLE member");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
    }

    @Test
    @DisplayName("[통합] soft delete된 멤버의 포스팅은 getPostings 결과에서 제외됨 [success]")
    void getPostings_softDelete된_멤버_포스팅_제외() {
        // Given
        Member activeMember = memberRepository.save(Member.of("active-user", "활성멤버", "TECH"));
        Member deletedMember = memberRepository.save(Member.of("deleted-user", "삭제된멤버", "TECH"));
        deletedMember.softDelete();
        memberRepository.save(deletedMember);

        LocalDate weekStart = LocalDate.of(2026, 6, 9);
        postingRepository.save(new Posting(activeMember.getId(), weekStart, 1, 1, 1, 1, 1, 1, 1, "admin"));
        postingRepository.save(new Posting(deletedMember.getId(), weekStart, 2, 2, 2, 2, 2, 2, 2, "admin"));

        // When
        Page<PostingResponse.PostingDto> result = postingService.getPostings(
            weekStart, null, null, PageRequest.of(0, 10)
        );

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).memberNickname()).isEqualTo("active-user");
        assertThat(result.getContent().get(0).memberName()).isEqualTo("활성멤버");
    }

    @Test
    @DisplayName("[통합] 활성 멤버의 포스팅은 정상 조회됨 [success]")
    void getPostings_활성_멤버_포스팅_정상_조회() {
        // Given
        Member member1 = memberRepository.save(Member.of("user-a", "멤버A", "TECH"));
        Member member2 = memberRepository.save(Member.of("user-b", "멤버B", "TECH"));

        LocalDate weekStart = LocalDate.of(2026, 6, 9);
        postingRepository.save(new Posting(member1.getId(), weekStart, 3, 3, 3, 3, 3, 3, 3, "admin"));
        postingRepository.save(new Posting(member2.getId(), weekStart, 5, 5, 5, 5, 5, 5, 5, "admin"));

        // When
        Page<PostingResponse.PostingDto> result = postingService.getPostings(
            weekStart, null, null, PageRequest.of(0, 10)
        );

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
            .extracting(PostingResponse.PostingDto::memberNickname)
            .containsExactlyInAnyOrder("user-a", "user-b");
        assertThat(result.getContent())
            .extracting(PostingResponse.PostingDto::memberName)
            .containsExactlyInAnyOrder("멤버A", "멤버B");
    }
}
