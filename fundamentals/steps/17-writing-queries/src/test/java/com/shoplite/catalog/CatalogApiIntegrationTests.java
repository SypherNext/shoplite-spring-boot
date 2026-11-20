package com.shoplite.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.shoplite.TestcontainersConfiguration;

/**
 * Calls the real API (controllers, services, PostgreSQL) through MockMvc.
 * Each test uses its own SKU so the tests do not depend on each other's data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CatalogApiIntegrationTests {

    @Autowired
    MockMvcTester mvc;

    @Test
    void listsTheSeededProducts() {
        assertThat(mvc.get().uri("/api/products"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[*].sku")
                .asArray()
                .contains("MUG-ENAMEL-01", "NOTE-A5-DOT");
    }

    @Test
    void createsAProductAndReturnsItsLocation() {
        assertThat(mvc.post().uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("Travel Mug", "MUG-TRAVEL-01", "18.00")))
                .hasStatus(HttpStatus.CREATED)
                .satisfies(result -> assertThat(result.getResponse().getHeader("Location"))
                        .startsWith("/api/products/"))
                .bodyJson()
                .extractingPath("$.categoryName").isEqualTo("Kitchen");
    }

    @Test
    void rejectsAnInvalidProductWithFieldErrors() {
        assertThat(mvc.post().uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("", "bad sku", "-1")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .hasPathSatisfying("$.errors.name", name -> assertThat(name).isNotNull())
                .hasPathSatisfying("$.errors.sku", sku -> assertThat(sku).isNotNull())
                .hasPathSatisfying("$.errors.price", price -> assertThat(price).isNotNull());
    }

    @Test
    void answers404WithProblemDetailsForAnUnknownProduct() {
        assertThat(mvc.get().uri("/api/products/999999"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("Product 999999 was not found");
    }

    @Test
    void refusesADuplicateSku() {
        assertThat(mvc.post().uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("Another Mug", "MUG-ENAMEL-01", "10.00")))
                .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void refusesToDeleteACategoryThatStillHasProducts() {
        assertThat(mvc.delete().uri("/api/categories/1"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Conflict");
    }

    @Test
    void listsLowStockProductsUsingTheConfiguredThreshold() {
        // Threshold is 5: the bottle (3), the pens (2) and the hoodie (5) qualify;
        // the monitor stand has 0 but is inactive, so it is left out.
        assertThat(mvc.get().uri("/api/products/low-stock"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[*].sku")
                .asArray()
                .contains("PEN-GEL-5PK", "BOTTLE-STEEL-750", "HOODIE-ZIP-L")
                .doesNotContain("DESK-STAND-OAK");
    }

    private static String productJson(String name, String sku, String price) {
        return """
                {"name": "%s", "sku": "%s", "description": "Test product",
                 "price": %s, "stock": 10, "categoryId": 1}
                """.formatted(name, sku, price);
    }

}
