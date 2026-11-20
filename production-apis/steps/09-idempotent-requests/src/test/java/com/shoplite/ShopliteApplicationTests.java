package com.shoplite;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ShopliteApplicationTests {

    // Fails if any bean cannot be created, a migration breaks, or an entity does not match its table.
    @Test
    void contextLoads() {
    }

}
