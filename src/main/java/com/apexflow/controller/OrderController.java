package com.apexflow.controller;

import com.apexflow.model.Order;
import com.apexflow.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrders(@RequestParam Map<String, String> allParams) {
        Page<Order> page = orderService.getOrders(allParams);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Total-Count", String.valueOf(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable String id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        Order created = orderService.createOrder(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String id) {
        return orderService.deleteOrder(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/stats/revenue")
    public ResponseEntity<Map<String, Double>> getTotalRevenue() {
        return ResponseEntity.ok(Map.of("totalRevenue", orderService.getTotalRevenue()));
    }

    @GetMapping("/stats/monthly-revenue")
    public ResponseEntity<?> getMonthlyRevenue(@RequestParam(required = false) Integer year) {
        if (year == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Il parametro 'year' è obbligatorio"));
        }
        return ResponseEntity.ok(orderService.getMonthlyRevenue(year));
    }
}
