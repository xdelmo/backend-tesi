package com.apexflow.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, String> home() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Welcome to ApexFlow Backend API");
        response.put("documentation", "https://github.com/xdelmo/backend-tesi");
        response.put("version", "0.0.1-SNAPSHOT");
        return response;
    }
}
