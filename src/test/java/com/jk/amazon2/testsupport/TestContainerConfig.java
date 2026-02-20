package com.jk.amazon2.testsupport;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;

@EnableConfigurationProperties(TestContainerMySQLDataSource.class)
@TestConfiguration(proxyBeanMethods = false)
public class TestContainerConfig {
    @Bean
    @ServiceConnection
    MySQLContainer mySQLContainer(TestContainerMySQLDataSource dataSource) {
        MySQLContainer container = new MySQLContainer(dataSource.dockerImage())
                .withDatabaseName(dataSource.databaseName())
                .withUsername(dataSource.username())
                .withPassword(dataSource.password())
                .withInitScript(dataSource.initScript());
        if(dataSource.commands() != null && !dataSource.commands().isEmpty()) {
            container.withCommand(dataSource.commands().toArray(new String[0]));
        }
        return container;
    }
}
