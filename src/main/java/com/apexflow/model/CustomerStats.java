package com.apexflow.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_stats")
@Data
public class CustomerStats {

    @Id
    private String id;

    private String customerId;
    private Double totalRevenue;
    private Integer totalOrders;
    private LocalDateTime lastOrderDate;
}
