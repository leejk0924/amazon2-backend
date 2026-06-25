package com.jk.amazon2.testsupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Tag("integration")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import({TestContainerConfig.class, TestJacksonConfig.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public abstract class IntegrationTestSupport {

    @Autowired
    private DatabaseCleanupService databaseCleanupService;

    @AfterEach
    void cleanUp() {
        databaseCleanupService.truncateAll();
    }
}
