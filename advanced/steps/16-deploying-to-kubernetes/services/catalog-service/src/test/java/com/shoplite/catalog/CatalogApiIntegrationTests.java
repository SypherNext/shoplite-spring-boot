package com.shoplite.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.shoplite.IntegrationTestSupport;
import com.shoplite.TestJwts;

class CatalogApiIntegrationTests extends IntegrationTestSupport {

    @Autowired
    StringRedisTemplate redis;

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
    void cachesAProductInRedisAfterTheFirstRead() {
        redis.delete("products::4");
        assertThat(mvc.get().uri("/api/products/4")).hasStatusOk();
        assertThat(redis.hasKey("products::4")).isTrue();
        assertThat(redis.opsForValue().get("products::4")).contains("\"sku\":\"NOTE-A5-DOT\"");
    }

    @Test
    void onlyAdminsCanAddProducts() {
        String body = """
                {"name": "Desk Lamp", "sku": "DESK-LAMP-01", "price": 39.00, "stock": 8, "categoryId": 2}
                """;
        assertThat(mvc.post().uri("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.post().uri("/api/products").with(TestJwts.customer("alice"))
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.post().uri("/api/products").with(TestJwts.admin())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .hasStatus(HttpStatus.CREATED);
    }

    @Test
    void rejectsAnInvalidProductWithFieldErrors() {
        assertThat(mvc.post().uri("/api/products").with(TestJwts.admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "", "sku": "bad sku", "price": -1, "stock": 1, "categoryId": 1}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errors.sku", sku -> assertThat(sku).isNotNull());
    }

    @Test
    void lowStockListUsesOneQueryWithJoinFetch() {
        assertThat(mvc.get().uri("/api/products/low-stock"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[*].categoryName")
                .asArray()
                .contains("Desk", "Kitchen", "Apparel");
    }

}
