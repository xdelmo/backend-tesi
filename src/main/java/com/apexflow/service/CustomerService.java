package com.apexflow.service;

import com.apexflow.model.Customer;
import com.apexflow.model.CustomerStats;
import com.apexflow.repository.CustomerRepository;
import com.apexflow.repository.CustomerStatsRepository;
import com.apexflow.util.QueryFilterParser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerStatsRepository customerStatsRepository;

    public CustomerService(CustomerRepository customerRepository, CustomerStatsRepository customerStatsRepository) {
        this.customerRepository = customerRepository;
        this.customerStatsRepository = customerStatsRepository;
    }

    public Page<Customer> getCustomers(Map<String, String> params) {
        Pageable pageable = QueryFilterParser.getPageable(params);
        Specification<Customer> spec = QueryFilterParser.parse(params);

        if ("customerStats.totalRevenue".equals(params.get("_sort"))) {
            List<Customer> allFiltered = customerRepository.findAll(spec);
            List<CustomerStats> allStats = customerStatsRepository.findAll();
            Map<String, Double> revenueMap = new HashMap<>();
            for (CustomerStats s : allStats) {
                revenueMap.put(s.getCustomerId(), s.getTotalRevenue() != null ? s.getTotalRevenue() : 0.0);
            }

            int order = "desc".equalsIgnoreCase(params.get("_order")) ? -1 : 1;
            allFiltered.sort((a, b) -> {
                double revA = revenueMap.getOrDefault(a.getId(), 0.0);
                double revB = revenueMap.getOrDefault(b.getId(), 0.0);
                return Double.compare(revA, revB) * order;
            });

            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allFiltered.size());
            List<Customer> pagedList = start > allFiltered.size() ? List.of() : allFiltered.subList(start, end);

            embedStats(pagedList);
            return new PageImpl<>(pagedList, pageable, allFiltered.size());
        }

        Page<Customer> page = customerRepository.findAll(spec, pageable);
        if (params.containsKey("_embed") && "customerStats".equals(params.get("_embed"))) {
            embedStats(page.getContent());
        }
        return page;
    }

    private void embedStats(List<Customer> customers) {
        for (Customer c : customers) {
            List<CustomerStats> stats = customerStatsRepository.findByCustomerId(c.getId());
            c.setCustomerStats(stats != null ? stats : List.of());
        }
    }

    public Optional<Customer> getCustomerById(String id) {
        return customerRepository.findById(id).map(c -> {
            embedStats(List.of(c));
            return c;
        });
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        if (customer.getId() == null || customer.getId().isBlank()) {
            customer.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        }
        return customerRepository.save(customer);
    }

    @Transactional
    public Optional<Customer> updateCustomer(String id, Customer details) {
        return customerRepository.findById(id).map(c -> {
            if (details.getName() != null) c.setName(details.getName());
            if (details.getEmail() != null) c.setEmail(details.getEmail());
            if (details.getCompany() != null) c.setCompany(details.getCompany());
            if (details.getStatus() != null) c.setStatus(details.getStatus());
            if (details.getIndustry() != null) c.setIndustry(details.getIndustry());
            if (details.getPlan() != null) c.setPlan(details.getPlan());
            if (details.getSubscriptionEndDate() != null) c.setSubscriptionEndDate(details.getSubscriptionEndDate());
            return customerRepository.save(c);
        });
    }

    @Transactional
    public boolean deleteCustomer(String id) {
        if (customerRepository.existsById(id)) {
            customerRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
