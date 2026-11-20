package com.shoplite.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.shoplite.IntegrationTestSupport;

/**
 * Calls the real API (controllers, services, Flyway-migrated PostgreSQL) through MockMvc.
 * Each test uses its own SKU so the tests do not depend on each other's data.
 */
class CatalogApiIntegrationTests extends IntegrationTestSupport {

    @Test
    void listsProductsOnePageAtATime() {
        assertThat(mvc.get().uri("/api/products?page=0&size=3&sort=price,desc"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.content.length()", size -> assertThat(size).isEqualTo(3))
                .hasPathSatisfying("$.content[0].sku", sku -> assertThat(sku).isEqualTo("HOODIE-ZIP-L"))
                .hasPathSatisfying("$.page.size", size -> assertThat(size).isEqualTo(3))
                .hasPathSatisfying("$.page.number", number -> assertThat(number).isEqualTo(0));
    }

    @Test
    void refusesToSortByAnUnknownField() {
        assertThat(mvc.get().uri("/api/products?sort=password"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Bad request");
    }

    @Test
    void createsAProductAndReturnsItsLocation() {
        assertThat(mvc.post().uri("/api/products").with(user("olivia"))
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
        assertThat(mvc.post().uri("/api/products").with(user("olivia"))
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
        assertThat(mvc.post().uri("/api/products").with(user("olivia"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("Another Mug", "MUG-ENAMEL-01", "10.00")))
                .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void refusesToDeleteACategoryThatStillHasProducts() {
        assertThat(mvc.delete().uri("/api/categories/1").with(user("olivia")))
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
