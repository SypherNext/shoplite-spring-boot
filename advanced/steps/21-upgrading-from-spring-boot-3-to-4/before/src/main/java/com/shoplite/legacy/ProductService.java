package com.shoplite.legacy;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<Product> search(String name) {
        return products.findByNameContainingIgnoreCaseOrderByName(name == null ? "" : name);
    }

    @Transactional(readOnly = true)
    public Product get(long id) {
        return products.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No product " + id));
    }

    @Transactional
    public Product create(ProductRequest request) {
        return products.save(new Product(request.name(), request.price()));
    }

}
