package com.jk.amazon2.entity;

import com.jk.amazon2.config.JpaAuditingConfig;
import com.jk.amazon2.repository.CategoryRepository;
import com.jk.amazon2.testsupport.TestContainerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@DataJpaTest
@ActiveProfiles("test")
@Import({TestContainerConfig.class, JpaAuditingConfig.class})
class CategoryTest {
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private TestEntityManager em;
    @MockitoBean
    private AuditorAware<String> auditorProvider;

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
        assertThat(saved.getCreateAt())
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
        assertThat(found.getCreateAt())
                .as("createdAt 검증").isNotNull();
        assertThat(found.getCode())
                .as("code 값 검증")
                .isEqualTo("TECH");
    }
}