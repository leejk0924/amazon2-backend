package com.jk.amazon2.testsupport;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("testcontainers.mysql")
public record TestContainerMySQLDataSource(
        String dockerImage,
        String databaseName,
        String username,
        String password,
        String initScript,
        List<String> commands
) {}
