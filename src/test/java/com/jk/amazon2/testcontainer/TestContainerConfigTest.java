package com.jk.amazon2.testcontainer;

import com.jk.amazon2.testsupport.TestContainerConfig;
import com.jk.amazon2.testsupport.TestContainerMySQLDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.mysql.MySQLContainer;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
public class TestContainerConfigTest {

    @Autowired
    private MySQLContainer mySQLContainer;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TestContainerMySQLDataSource mySQLDataSource;

    @DisplayName("[성공 케이스] - Test DataSource 검증")
    @Test
    void dataSourceShouldBeAutoConfigured() throws SQLException {
        // given & when
        // @ServiceConnection 에서 DataSource 설정

        // then
        assertThat(dataSource).isNotNull();
        assertThat(dataSource.getConnection()).isNotNull();

        assertThat(mySQLContainer.getDatabaseName()).isEqualTo(mySQLDataSource.databaseName());
        assertThat(mySQLContainer.getUsername()).isEqualTo(mySQLDataSource.username());
        assertThat(mySQLContainer.getPassword()).isEqualTo(mySQLDataSource.password());
    }

    @DisplayName("[성공 케이스] - 테이블 생성 검증")
    @Test
    void initScriptShouldBeExecuted() {
        // given
        List<String> expectedTables = List.of("blog_category", "member", "posting");

        // then
        expectedTables.stream()
                        .forEach(tableName ->{
                            Integer count = getTableCount(tableName);
                            assertThat(count).as("테이블 %s가 존재", tableName)
                                    .isEqualTo(1);
                        });
    }

    private Integer getTableCount(String tableName) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                tableName
        );
    }
}
