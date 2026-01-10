package com.apexflow.controller;

import com.apexflow.model.Customer;
import com.apexflow.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // 1. Definisce che questa classe gestisce API REST
@RequestMapping("/api/customers") // 2. L'URL base sarà http://localhost:8080/api/customers
@CrossOrigin(origins = "*") // 3. IMPORTANTE: Permette al frontend di comunicare con il backend
public class CustomerController {

    @Autowired // Iniettiamo il "telecomando" del database
    private CustomerRepository customerRepository;

    // Metodo per ottenere tutti i clienti
    @GetMapping
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }
}