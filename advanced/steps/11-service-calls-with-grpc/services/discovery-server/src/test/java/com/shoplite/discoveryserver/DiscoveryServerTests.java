package com.shoplite.discoveryserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// The build turns the Eureka client off for every test; the server needs it, so this test turns it back on.
@SpringBootTest(properties = "eureka.client.enabled=true")
class DiscoveryServerTests {

    @Test
    void contextLoads() {
    }

}
