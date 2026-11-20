package com.shoplite;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Base class for full-application tests: real PostgreSQL and Redis (Testcontainers),
 * a WireMock server standing in for the payment provider, and MockMvc to call the API.
 * Every subclass has the same configuration, so Spring builds the context (and starts
 * the containers) once and reuses it for all of them.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestSupport {

    // One fake payment provider for the whole test run, on a free port.
    // http2PlainDisabled: the JDK HTTP client asks plain-HTTP servers to upgrade to HTTP/2 ("h2c"),
    // and with WireMock that upgrade failed here with "RST_STREAM" and "EOF reached" errors.
    // Serving HTTP/1.1 only avoids it.
    protected static final WireMockServer paymentProvider =
            new WireMockServer(options().dynamicPort().http2PlainDisabled(true));

    static {
        paymentProvider.start();
    }

    @DynamicPropertySource
    static void paymentProviderUrl(DynamicPropertyRegistry registry) {
        registry.add("spring.http.serviceclient.payments.base-url", paymentProvider::baseUrl);
    }

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
