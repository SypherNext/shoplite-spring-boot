package com.shoplite;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Base class for full-application tests: the real app on a real PostgreSQL (Testcontainers),
 * called through MockMvc. Every subclass has the same configuration, so Spring builds the
 * context (and starts the container) once and reuses it for all of them.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvcTester mvc;

    /** Reads one value out of a JSON response body, for example json(result, "$.id"). */
    protected static <T> T json(MvcTestResult result, String path) {
        try {
            return JsonPath.read(result.getResponse().getContentAsString(), path);
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
