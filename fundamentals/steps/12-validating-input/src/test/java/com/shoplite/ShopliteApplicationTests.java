package com.shoplite;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.shoplite.catalog.ProductService;

@SpringBootTest
class ShopliteApplicationTests {

    @Autowired
    ProductService productService;

    @Test
    void contextLoads() {
    }

    @Test
    void startsWithTheSeededCatalog() {
        assertThat(productService.findAll()).hasSize(8);
    }

}
