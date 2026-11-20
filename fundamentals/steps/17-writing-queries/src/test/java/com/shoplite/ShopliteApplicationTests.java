package com.shoplite;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ShopliteApplicationTests {

    // Fails if any bean cannot be created or the database cannot be reached.
    @Test
    void contextLoads() {
    }

}
