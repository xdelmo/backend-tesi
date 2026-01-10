package com.apexflow.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "products")
@Data
public class Product {

    @Id
    private String id;

    private String name;
    private String description;
    private String category;
    private Double price;
    private String duration;
    private String status;
}
