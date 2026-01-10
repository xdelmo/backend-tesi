package com.apexflow.controller;

import com.apexflow.model.CustomerStats;
import com.apexflow.repository.CustomerStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customerStats")
@CrossOrigin(origins = "*")
public class CustomerStatsController {

    @Autowired
    private CustomerStatsRepository customerStatsRepository;

    @GetMapping
    public List<CustomerStats> getAllCustomerStats() {
        return customerStatsRepository.findAll();
    }
}
