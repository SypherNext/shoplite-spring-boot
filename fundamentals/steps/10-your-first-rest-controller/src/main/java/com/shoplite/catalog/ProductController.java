package com.shoplite.catalog;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // GET /api/products
    @GetMapping
    public List<Product> list() {
        return productService.findAll();
    }

    // GET /api/products/1: {id} in the path becomes the id parameter
    @GetMapping("/{id}")
    public Product get(@PathVariable Long id) {
        return productService.findById(id);
    }

}
