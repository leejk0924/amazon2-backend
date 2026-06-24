package com.jk.amazon2.testsupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@Component
@ActiveProfiles("test")
public class DatabaseCleanupService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // FK 의존 순서: posting_error/dead_letter → posting → batch_execution → member → blog_category
    private static final List<String> TABLE_NAMES = List.of(
            "posting_error",
            "posting_dead_letter",
            "posting",
            "batch_execution",
            "member",
            "blog_category"
    );

    public void truncateAll() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        TABLE_NAMES.forEach(table -> jdbcTemplate.execute("TRUNCATE TABLE " + table));
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
}
