package com.apexflow.config;

import com.apexflow.model.Customer;
import com.apexflow.model.User;
import com.apexflow.model.CustomerStats;
import com.apexflow.model.Product;
import com.apexflow.model.Order;
import com.apexflow.model.OrderItem;
import com.apexflow.repository.CustomerStatsRepository;
import com.apexflow.repository.CustomerRepository;
import com.apexflow.repository.ProductRepository;
import com.apexflow.repository.OrderRepository;
import com.apexflow.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration // Dice a Spring che questa è una classe di configurazione
public class DataInitializer {

    @Bean // Questo metodo verrà eseguito all'avvio
    CommandLineRunner initDatabase(CustomerRepository customerRepository, UserRepository userRepository, CustomerStatsRepository customerStatsRepository, ProductRepository productRepository, OrderRepository orderRepository) {
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

            // Seeding CustomerStats
            if (customerStatsRepository.count() == 0) {
                System.out.println("--- Seeding CustomerStats... ---");
                
                CustomerStats cs1 = new CustomerStats();
                cs1.setId("cs-1767988302598");
                cs1.setCustomerId("I9j0K1l2M3n4O5p6");
                cs1.setTotalRevenue(499.5);
                cs1.setTotalOrders(1);
                cs1.setLastOrderDate(java.time.LocalDateTime.parse("2026-01-09T19:51:42.598"));
                customerStatsRepository.save(cs1);

                CustomerStats cs2 = new CustomerStats();
                cs2.setId("cs-1767988641922");
                cs2.setCustomerId("Q7r8S9t0U1v2W3x4");
                cs2.setTotalRevenue(1198.8);
                cs2.setTotalOrders(1);
                cs2.setLastOrderDate(java.time.LocalDateTime.parse("2026-01-09T19:57:21.922"));
                customerStatsRepository.save(cs2);
                
                System.out.println("--- CustomerStats seeded! ---");
            }

            // Seeding Products
            if (productRepository.count() == 0) {
                System.out.println("--- Seeding Products... ---");

                Product p1 = new Product();
                p1.setId("PROD-CL-001");
                p1.setName("Archiviazione Enterprise");
                p1.setDescription("Storage cloud scalabile ad alta disponibilità");
                p1.setCategory("Cloud");
                p1.setPrice(500.0);
                p1.setDuration("Mensile");
                p1.setStatus("Attivo");
                productRepository.save(p1);

                Product p2 = new Product();
                p2.setId("PROD-CL-002");
                p2.setName("Calcolo Serverless");
                p2.setDescription("Risorse di calcolo on-demand");
                p2.setCategory("Cloud");
                p2.setPrice(1200.0);
                p2.setDuration("Mensile");
                p2.setStatus("Attivo");
                productRepository.save(p2);

                Product p3 = new Product();
                p3.setId("PROD-AN-001");
                p3.setName("Dashboard Predittiva");
                p3.setDescription("Analisi avanzata con previsioni AI");
                p3.setCategory("Analytics");
                p3.setPrice(800.0);
                p3.setDuration("Annuale");
                p3.setStatus("Attivo");
                productRepository.save(p3);

                Product p4 = new Product();
                p4.setId("PROD-AN-002");
                p4.setName("Connettore Data Warehouse");
                p4.setDescription("Strumento di integrazione per i principali data warehouse");
                p4.setCategory("Analytics");
                p4.setPrice(300.0);
                p4.setDuration("Una Tantum");
                p4.setStatus("Attivo");
                productRepository.save(p4);

                Product p5 = new Product();
                p5.setId("PROD-AI-001");
                p5.setName("Analisi del Sentiment NLP");
                p5.setDescription("API di analisi del testo basata su AI");
                p5.setCategory("AI");
                p5.setPrice(1500.0);
                p5.setDuration("Mensile");
                p5.setStatus("Inattivo");
                productRepository.save(p5);

                Product p6 = new Product();
                p6.setId("PROD-AI-002");
                p6.setName("Riconoscimento Immagini API");
                p6.setDescription("Computer vision per la classificazione delle immagini");
                p6.setCategory("AI");
                p6.setPrice(2000.0);
                p6.setDuration("Mensile");
                p6.setStatus("Attivo");
                productRepository.save(p6);

                System.out.println("--- Products seeded! ---");
            }

            // Seeding Orders
            if (orderRepository.count() == 0) {
                System.out.println("--- Seeding Orders... ---");

                Order o1 = new Order();
                o1.setId("ORD-719");
                o1.setCustomerId("I9j0K1l2M3n4O5p6");
                o1.setDate(java.time.LocalDateTime.parse("2026-01-09T19:51:34.467"));
                o1.setStatus("Pagato");
                o1.setType("Abbonamento");
                o1.setSubtotal(500.0);
                o1.setTax(49.5);
                o1.setDiscountAmount(50.0);
                o1.setTotal(499.5);

                OrderItem oi1 = new OrderItem();
                oi1.setProductId("PROD-CL-001");
                oi1.setName("Archiviazione Enterprise");
                oi1.setPrice(500.0);
                oi1.setQuantity(1);
                oi1.setCategory("Cloud");
                oi1.setOrder(o1);

                o1.setItems(List.of(oi1));
                orderRepository.save(o1);

                Order o2 = new Order();
                o2.setId("ORD-4736");
                o2.setCustomerId("Q7r8S9t0U1v2W3x4");
                o2.setDate(java.time.LocalDateTime.parse("2026-01-09T19:57:10.184"));
                o2.setStatus("Pagato");
                o2.setType("Abbonamento");
                o2.setSubtotal(1200.0);
                o2.setTax(118.8);
                o2.setDiscountAmount(120.0);
                o2.setTotal(1198.8);

                OrderItem oi2 = new OrderItem();
                oi2.setProductId("PROD-CL-002");
                oi2.setName("Calcolo Serverless");
                oi2.setPrice(1200.0);
                oi2.setQuantity(1);
                oi2.setCategory("Cloud");
                oi2.setOrder(o2);

                o2.setItems(List.of(oi2));
                orderRepository.save(o2);

                System.out.println("--- Orders seeded! ---");
            }
        };
    }
}