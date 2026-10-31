package com.apexflow.controller;

import com.apexflow.model.CustomerStats;
import com.apexflow.service.CustomerStatsService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customerStats")
public class CustomerStatsController {

    private final CustomerStatsService statsService;

    public CustomerStatsController(CustomerStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public ResponseEntity<List<CustomerStats>> getAllCustomerStats(@RequestParam Map<String, String> allParams) {
        Page<CustomerStats> page = statsService.getStats(allParams);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Total-Count", String.valueOf(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerStats> getCustomerStatsById(@PathVariable String id) {
        return statsService.getStatsById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CustomerStats> createStats(@RequestBody CustomerStats stats) {
        CustomerStats saved = statsService.saveStats(stats);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CustomerStats> updateStats(@PathVariable String id, @RequestBody CustomerStats stats) {
        return statsService.updateStats(id, stats)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
