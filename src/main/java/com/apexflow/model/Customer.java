package com.apexflow.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity // 1. Dice a Spring che questa è una tabella del DB
@Table(name = "customers") // 2. Specifica il nome della tabella
@Data // 3. Se usi Lombok, questo crea automaticamente i Getter e Setter
public class Customer {

    @Id // 4. Chiave primaria
    private String id;

    private String name;
    private String email;
    private String company;
    
    // In Java usiamo le stringhe per gli stati per ora, per semplicità
    private String status; 
    private String industry;
    private String plan;
}
