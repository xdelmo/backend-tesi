package com.apexflow.service;

import com.apexflow.model.*;
import com.apexflow.repository.*;
import com.apexflow.util.QueryFilterParser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final CustomerStatsRepository customerStatsRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        CustomerStatsRepository customerStatsRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.customerStatsRepository = customerStatsRepository;
        this.productRepository = productRepository;
    }

    public Page<Order> getOrders(Map<String, String> params) {
        Pageable pageable = QueryFilterParser.getPageable(params);
        Specification<Order> spec = QueryFilterParser.parse(params);

        if (params != null && params.containsKey("customer.name_like")) {
            String nameQuery = params.get("customer.name_like").toLowerCase();
            List<Customer> matchingCustomers = customerRepository.findAll((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + nameQuery + "%")
            );
            List<String> customerIds = matchingCustomers.stream().map(Customer::getId).toList();

            Specification<Order> customerSpec = (root, query, cb) ->
                    customerIds.isEmpty() ? cb.disjunction() : root.get("customerId").in(customerIds);
            spec = spec.and(customerSpec);
        }

        return orderRepository.findAll(spec, pageable);
    }

    public Optional<Order> getOrderById(String id) {
        return orderRepository.findById(id);
    }

    @Transactional
    public Order createOrder(Order order) {
        if (order.getId() == null || order.getId().isBlank()) {
            order.setId("ORD-" + (int) (Math.random() * 9000 + 1000));
        }
        if (order.getDate() == null) {
            order.setDate(LocalDateTime.now());
        }

        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                item.setOrder(order);
            }
        }

        Order savedOrder = orderRepository.save(order);

        if (order.getCustomerId() != null) {
            customerRepository.findById(order.getCustomerId()).ifPresent(customer -> {
                boolean hasSubscription = false;
                int monthsToAdd = 0;

                if (order.getItems() != null) {
                    for (OrderItem item : order.getItems()) {
                        if (item.getProductId() != null) {
                            Optional<Product> prodOpt = productRepository.findById(item.getProductId());
                            if (prodOpt.isPresent()) {
                                Product product = prodOpt.get();
                                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                                if ("Mensile".equalsIgnoreCase(product.getDuration())) {
                                    hasSubscription = true;
                                    monthsToAdd += qty;
                                } else if ("Annuale".equalsIgnoreCase(product.getDuration())) {
                                    hasSubscription = true;
                                    monthsToAdd += qty * 12;
                                }
                            }
                        }
                    }
                }

                if (hasSubscription) {
                    customer.setStatus("Attivo");
                    LocalDate baseDate = LocalDate.now();
                    if (customer.getSubscriptionEndDate() != null && customer.getSubscriptionEndDate().isAfter(baseDate)) {
                        baseDate = customer.getSubscriptionEndDate();
                    }
                    customer.setSubscriptionEndDate(baseDate.plusMonths(monthsToAdd));
                    customerRepository.save(customer);
                }

                List<CustomerStats> statsList = customerStatsRepository.findByCustomerId(customer.getId());
                CustomerStats stats;
                if (statsList != null && !statsList.isEmpty()) {
                    stats = statsList.get(0);
                } else {
                    stats = new CustomerStats();
                    stats.setId("cs-" + System.currentTimeMillis());
                    stats.setCustomerId(customer.getId());
                    stats.setTotalOrders(0);
                    stats.setTotalRevenue(0.0);
                }

                double orderTotal = order.getTotal() != null ? order.getTotal() : 0.0;
                stats.setTotalOrders(stats.getTotalOrders() + 1);
                stats.setTotalRevenue(stats.getTotalRevenue() + orderTotal);
                stats.setLastOrderDate(order.getDate());
                customerStatsRepository.save(stats);
            });
        }

        return savedOrder;
    }

    @Transactional
    public boolean deleteOrder(String id) {
        if (orderRepository.existsById(id)) {
            orderRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public double getTotalRevenue() {
        return orderRepository.findAll().stream()
                .filter(o -> "Pagato".equalsIgnoreCase(o.getStatus()))
                .mapToDouble(o -> o.getTotal() != null ? o.getTotal() : 0.0)
                .sum();
    }

    public List<Double> getMonthlyRevenue(int year) {
        Double[] monthly = new Double[12];
        Arrays.fill(monthly, 0.0);

        for (Order o : orderRepository.findAll()) {
            if ("Pagato".equalsIgnoreCase(o.getStatus()) && o.getDate() != null && o.getDate().getYear() == year) {
                int month = o.getDate().getMonthValue() - 1;
                monthly[month] += (o.getTotal() != null ? o.getTotal() : 0.0);
            }
        }
        return Arrays.asList(monthly);
    }
}
