package com.apexflow.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
@Data
public class Customer {

    @Id
    private String id;

    private String name;
    private String email;
    private String company;
    private String status;
    private String industry;
    private String plan;
    private LocalDate subscriptionEndDate;

    @Transient
    private List<CustomerStats> customerStats = new ArrayList<>();
}
