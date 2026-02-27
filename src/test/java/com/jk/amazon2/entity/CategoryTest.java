package com.jk.amazon2.entity;

import com.jk.amazon2.config.JpaAuditingConfig;
import com.jk.amazon2.config.QueryDslConfig;
import com.jk.amazon2.repository.CategoryRepository;
import com.jk.amazon2.testsupport.TestContainerConfig;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@DataJpaTest
@ActiveProfiles("test")
@Import({TestContainerConfig.class, JpaAuditingConfig.class, QueryDslConfig.class})
class CategoryTest {
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private TestEntityManager em;
    @MockitoBean
    private AuditorAware<String> auditorProvider;

    @Nested
    @DisplayName("Category 생성 시 Audit 테스트")
    class CreateCategory {
        @Test
        @DisplayName("Category Audit 필드 입력 검증")
        void createdByTest() throws Exception {
            // given
            String user = "TestUser";
            given(auditorProvider.getCurrentAuditor()).willReturn(Optional.of(user));
            Category category = Category.of("TECH", "기술", "기술 카테고리");

            // when
            Category saved = categoryRepository.save(category);
            em.flush();

            // then
            assertThat(saved.getCreatedBy())
                    .as("createdBy 검증")
                    .isEqualTo(user);
            assertThat(saved.getCreatedAt())
                    .as("createdAt 검증")
                    .isNotNull();
        }

        @Test
        @DisplayName("Category 조회 시 createdBy가 DB에 영속화되어 유지된다")
        void createdByPersisted() {
            // given
            String user = "TestUser";
            given(auditorProvider.getCurrentAuditor()).willReturn(Optional.of(user));
            Category category = Category.of("TECH", "기술", "기술 카테고리");
            Category saved = categoryRepository.save(category);
            em.flush();
            em.clear();

            // when
            Category found = categoryRepository.findById(saved.getCode()).orElseThrow();

            // then
            assertThat(found.getCreatedBy())
                    .as("createdBy 검증")
                    .isEqualTo(user);
            assertThat(found.getCreatedAt())
                    .as("createdAt 검증").isNotNull();
            assertThat(found.getCode())
                    .as("code 값 검증")
                    .isEqualTo("TECH");
        }
    }

    @Nested
    @DisplayName("Category 수정 테스트")
    class UpdateCategory {

        @DisplayName("Category 이름 수정 시, 필드 값이 정상적으로 변경된다.")
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("provideCategoryRequests")
        void updateCategoryName_success(
                String scenario,
                String updateName,
                String updateDescription,
                String expectedName,
                String expectedDescription
        ) {
            // given
            String code = "TECH";
            Category category = Category.of(code, "전자기기", "설명");

            // when
            category.updateNameCategory(updateName);
            category.updateDescription(updateDescription);

            // then
            SoftAssertions.assertSoftly(softly -> {
                assertThat(category.getCode()).as("code는 변하지 않아야 함").isEqualTo(code);
                assertThat(category.getName()).as("이름 변경 확인: " + scenario).isEqualTo(expectedName);
                assertThat(category.getDescription()).as("설명 변경 확인: " + scenario).isEqualTo(expectedDescription);
            });
        }

        private static Stream<Arguments> provideCategoryRequests() {
            return Stream.of(
                    Arguments.of("name만 수정", "테크", "설명", "테크", "설명"),
                    Arguments.of("description만 수정", "전자기기", "Description 수정", "전자기기", "Description 수정"),
                    Arguments.of("둘 다 수정", "테크", "설명", "테크", "설명"),
                    Arguments.of("변경 없음", "테크", "설명", "테크", "설명")
            );
        }
    }
}