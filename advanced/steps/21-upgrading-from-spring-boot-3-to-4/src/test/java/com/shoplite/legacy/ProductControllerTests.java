package com.shoplite.legacy;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTests {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProductService service;

    @Test
    void searchReturnsProductsWithPlainPrices() throws Exception {
        given(service.search("mug")).willReturn(List.of(new Product("Enamel Mug", new BigDecimal("1E+1"))));

        mvc.perform(get("/api/products").param("name", "mug"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Enamel Mug"))
                .andExpect(jsonPath("$[0].price").value(10));
    }

    @Test
    void invalidProductIsA400ProblemDetail() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

}
