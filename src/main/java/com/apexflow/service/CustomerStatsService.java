package com.apexflow.service;

import com.apexflow.model.CustomerStats;
import com.apexflow.repository.CustomerStatsRepository;
import com.apexflow.util.QueryFilterParser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CustomerStatsService {

    private final CustomerStatsRepository statsRepository;

    public CustomerStatsService(CustomerStatsRepository statsRepository) {
        this.statsRepository = statsRepository;
    }

    public Page<CustomerStats> getStats(Map<String, String> params) {
        Pageable pageable = QueryFilterParser.getPageable(params);
        Specification<CustomerStats> spec = QueryFilterParser.parse(params);
        return statsRepository.findAll(spec, pageable);
    }

    public Optional<CustomerStats> getStatsById(String id) {
        return statsRepository.findById(id);
    }

    @Transactional
    public CustomerStats saveStats(CustomerStats stats) {
        if (stats.getId() == null || stats.getId().isBlank()) {
            stats.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        }
        return statsRepository.save(stats);
    }

    @Transactional
    public Optional<CustomerStats> updateStats(String id, CustomerStats details) {
        return statsRepository.findById(id).map(s -> {
            if (details.getTotalRevenue() != null) s.setTotalRevenue(details.getTotalRevenue());
            if (details.getTotalOrders() != null) s.setTotalOrders(details.getTotalOrders());
            if (details.getLastOrderDate() != null) s.setLastOrderDate(details.getLastOrderDate());
            return statsRepository.save(s);
        });
    }
}
