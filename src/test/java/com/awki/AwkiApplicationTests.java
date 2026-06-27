package com.awki;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(AwkiApplicationTests.EmptyConfig.class)
class AwkiApplicationTests {

    static class EmptyConfig {}

    @Test
    void contextLoads() {
    }
}
