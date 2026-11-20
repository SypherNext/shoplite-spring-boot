package com.shoplite.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.shoplite.TestUsers;
import com.shoplite.security.SecurityConfig;

/**
 * A web slice test: only the MVC layer (this controller, JSON, validation, error handling,
 * security filters) is started. ProductService is a Mockito mock, so no database is needed.
 * A slice does not scan @Configuration classes, so the test imports SecurityConfig itself.
 */
@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTests {

    private static final String NEW_PRODUCT = """
            {"name": "Desk Lamp", "sku": "DESK-LAMP-01", "price": 39.00, "stock": 8, "categoryId": 2}
            """;

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    ProductService productService;

    @Test
    void listsProductsAsAPage() {
        var summary = new ProductSummary(1L, "Enamel Mug", "MUG-ENAMEL-01", new BigDecimal("12.50"), 40, "Kitchen");
        given(productService.search(eq(null), eq(null), any()))
                .willReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));

        assertThat(mvc.get().uri("/api/products"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.content[0].sku", sku -> assertThat(sku).isEqualTo("MUG-ENAMEL-01"))
                .hasPathSatisfying("$.page.totalElements", total -> assertThat(total).isEqualTo(1));
    }

    @Test
    void turnsPageParametersIntoAPageable() {
        given(productService.search(any(), any(), any())).willReturn(new PageImpl<>(List.of()));

        mvc.get().uri("/api/products?page=2&size=5&sort=price,desc").exchange();

        then(productService).should().search(eq(null), eq(null),
                eq(PageRequest.of(2, 5, Sort.by("price").descending())));
    }

    @Test
    void creatingWithoutCredentialsIs401() {
        assertThat(mvc.post().uri("/api/products").contentType(MediaType.APPLICATION_JSON).content(NEW_PRODUCT))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void creatingAsACustomerIs403() {
        assertThat(mvc.post().uri("/api/products").with(TestUsers.customer("alice"))
                .contentType(MediaType.APPLICATION_JSON).content(NEW_PRODUCT))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void creatingAProductAnswers201WithItsLocation() {
        given(productService.create(any())).willReturn(new ProductResponse(9L, "Desk Lamp", "DESK-LAMP-01", null,
                new BigDecimal("39.00"), 8, true, 2L, "Desk"));

        assertThat(mvc.post().uri("/api/products").with(TestUsers.admin())
                .contentType(MediaType.APPLICATION_JSON).content(NEW_PRODUCT))
                .hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "/api/products/9");
    }

    @Test
    void anInvalidProductNeverReachesTheService() {
        assertThat(mvc.post().uri("/api/products").with(TestUsers.admin()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "", "sku": "bad sku", "price": -1, "stock": 1, "categoryId": 1}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errors.sku", sku -> assertThat(sku).isNotNull());
        then(productService).shouldHaveNoInteractions();
    }

    @Test
    void corsPreflightFromTheAllowedOriginSucceeds() {
        assertThat(mvc.options().uri("/api/products")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"))
                .hasStatusOk()
                .hasHeader("Access-Control-Allow-Origin", "http://localhost:5173");
    }

    @Test
    void corsPreflightFromAnotherOriginIsRejected() {
        assertThat(mvc.options().uri("/api/products")
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "POST"))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

}
