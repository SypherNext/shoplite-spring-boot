package com.shoplite.catalog;

import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shoplite.common.ProblemExamples;

@Tag(name = "Products", description = "Browse and manage the products in the catalog")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "List products, one page at a time",
            description = "Example: /api/products?categoryId=1&search=mug&page=0&size=5&sort=price,desc")
    @GetMapping
    public PagedModel<ProductSummary> list(@RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return new PagedModel<>(productService.search(categoryId, search, pageable));
    }

    @Operation(summary = "Get one product by id")
    @ApiResponse(responseCode = "200", description = "The product")
    @ApiResponse(responseCode = "404", description = "No product has this id",
            content = @Content(mediaType = "application/problem+json",
                    examples = @ExampleObject(value = ProblemExamples.NOT_FOUND)))
    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return productService.findById(id);
    }

    @GetMapping("/sku/{sku}")
    public ProductResponse getBySku(@PathVariable String sku) {
        return productService.findBySku(sku);
    }

    @GetMapping("/low-stock")
    public List<ProductResponse> lowStock() {
        return productService.findLowStock();
    }

    @Operation(summary = "Add a product to the catalog")
    @ApiResponse(responseCode = "201", description = "Created. The Location header points at the new product")
    @ApiResponse(responseCode = "400", description = "A field is missing or invalid",
            content = @Content(mediaType = "application/problem+json",
                    examples = @ExampleObject(value = ProblemExamples.VALIDATION_FAILED)))
    @ApiResponse(responseCode = "409", description = "Another product already uses this SKU",
            content = @Content(mediaType = "application/problem+json",
                    examples = @ExampleObject(value = ProblemExamples.CONFLICT)))
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.create(request);
        return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
