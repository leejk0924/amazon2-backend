package com.jk.amazon2.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * Flyway 데이터베이스 마이그레이션 설정
 * 테스트에서는 제외 (Hibernate ddl-auto: create 사용)
 */
@Configuration
@Profile("!test")
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:/db/migration")
                .table("flyway_schema_history")
                .baselineOnMigrate(true)
                .load();
    }
}
