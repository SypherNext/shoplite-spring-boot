package com.shoplite.shop;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopProperties shopProperties;

    public ShopController(ShopProperties shopProperties) {
        this.shopProperties = shopProperties;
    }

    @GetMapping
    public ShopProperties shopInfo() {
        // The settings are all safe to show, so the record doubles as the response body.
        return shopProperties;
    }

}
