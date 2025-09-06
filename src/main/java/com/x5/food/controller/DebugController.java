package com.x5.food.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {

    @GetMapping("/echo")
    public String handshake(@RequestParam(required = false) String message) {
        return message;
    }

}
