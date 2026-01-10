package com.apexflow.config;

import com.apexflow.model.Customer;
import com.apexflow.model.User;
import com.apexflow.repository.CustomerRepository;
import com.apexflow.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration // Dice a Spring che questa è una classe di configurazione
public class DataInitializer {

    @Bean // Questo metodo verrà eseguito all'avvio
    CommandLineRunner initDatabase(CustomerRepository customerRepository, UserRepository userRepository) {
        return args -> {
            // Seeding Customers
            if (customerRepository.count() == 0) {
                System.out.println("--- Seeding Customers... ---");
                Customer c1 = new Customer();
                c1.setId("A1b2C3d4E5f6G7h8");
                c1.setName("Mario Rossi");
                c1.setEmail("mario@rossi.tech");
                c1.setCompany("Tech Spa");
                c1.setStatus("Attivo");
                c1.setIndustry("Software");
                c1.setPlan("Enterprise");
                customerRepository.save(c1);

                Customer c2 = new Customer();
                c2.setId("I9j0K1l2M3n4O5p6");
                c2.setName("Luca Bianchi");
                c2.setEmail("l.bianchi@consulting.it");
                c2.setCompany("Consulting Srl");
                c2.setStatus("In Attesa");
                c2.setIndustry("Software");
                c2.setPlan("Professional");
                customerRepository.save(c2);
                System.out.println("--- Customers seeded! ---");
            }

            // Seeding Users
            if (userRepository.count() == 0) {
                System.out.println("--- Seeding Users... ---");
                User admin = new User();
                admin.setEmail("admin@demo.com");
                admin.setPassword("password"); // In production use BCrypt!
                admin.setName("Utente Admin");
                admin.setRole("admin");
                admin.setAvatar("https://primefaces.org/cdn/primeng/images/demo/avatar/amyelsner.png");
                
                userRepository.save(admin);
                System.out.println("--- Users seeded! ---");
            }
        };
    }
}