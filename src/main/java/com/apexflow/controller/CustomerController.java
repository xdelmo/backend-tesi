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

    @GetMapping("/{id}")
    public Customer getCustomerById(@PathVariable String id) {
        return customerRepository.findById(id).orElse(null);
    }

    @PostMapping
    public Customer createCustomer(@RequestBody Customer customer) {
        return customerRepository.save(customer);
    }

    @PatchMapping("/{id}")
    public Customer updateCustomer(@PathVariable String id, @RequestBody Customer customerDetails) {
        return customerRepository.findById(id).map(customer -> {
            if (customerDetails.getName() != null) customer.setName(customerDetails.getName());
            if (customerDetails.getEmail() != null) customer.setEmail(customerDetails.getEmail());
            if (customerDetails.getCompany() != null) customer.setCompany(customerDetails.getCompany());
            if (customerDetails.getStatus() != null) customer.setStatus(customerDetails.getStatus());
            if (customerDetails.getIndustry() != null) customer.setIndustry(customerDetails.getIndustry());
            if (customerDetails.getPlan() != null) customer.setPlan(customerDetails.getPlan());
            return customerRepository.save(customer);
        }).orElse(null);
    }
}