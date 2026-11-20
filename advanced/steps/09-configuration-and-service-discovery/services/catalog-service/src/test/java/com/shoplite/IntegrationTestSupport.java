package com.shoplite;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Base class for full catalog-service tests: real PostgreSQL, Redis and Kafka, MockMvc to call the API. */
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

    protected int stockOf(long productId) {
        return json(mvc.get().uri("/api/products/{id}", productId).exchange(), "$.stock");
    }

}
