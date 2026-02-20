package com.jk.amazon2;

import com.jk.amazon2.testsupport.TestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
class Amazon2ApplicationTests {

    @Test
    void contextLoads() {
    }

}
