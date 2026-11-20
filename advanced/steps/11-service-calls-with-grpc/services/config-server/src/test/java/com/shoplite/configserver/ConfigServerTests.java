package com.shoplite.configserver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** Serves the real config-repo folder (the test runs in services/config-server, two levels down). */
@SpringBootTest
@AutoConfigureMockMvc
class ConfigServerTests {

    @Autowired
    MockMvcTester mvc;

    @Test
    void servesSharedAndServiceSpecificSettings() {
        assertThat(mvc.get().uri("/catalog-service/default"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.propertySources[0].name",
                        name -> assertThat(name).asString().endsWith("catalog-service.properties"))
                .hasPathSatisfying("$.propertySources[1].name",
                        name -> assertThat(name).asString().endsWith("application.properties"));
    }

}
