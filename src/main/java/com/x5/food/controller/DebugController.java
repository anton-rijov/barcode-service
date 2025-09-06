package com.x5.food.controller;

import com.x5.food.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {

    private final ProductService productService;

    public DebugController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/echo")
    public String handshake(@RequestParam(required = false) String message) {
        return message;
    }

    @GetMapping("/db")
    public String db() {
        return productService.getData();
    }

}
