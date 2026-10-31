package com.apexflow.config;

import com.apexflow.model.Customer;
import com.apexflow.model.CustomerStats;
import com.apexflow.model.Order;
import com.apexflow.model.OrderItem;
import com.apexflow.model.Product;
import com.apexflow.model.User;
import com.apexflow.repository.CustomerRepository;
import com.apexflow.repository.CustomerStatsRepository;
import com.apexflow.repository.OrderRepository;
import com.apexflow.repository.ProductRepository;
import com.apexflow.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final CustomerStatsRepository customerStatsRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    public DataInitializer(
            CustomerRepository customerRepository,
            UserRepository userRepository,
            CustomerStatsRepository customerStatsRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            ObjectMapper objectMapper) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.customerStatsRepository = customerStatsRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
    }

    @Bean
    public CommandLineRunner initDatabase() {
        return args -> seedDatabase();
    }

    public void seedDatabase() {
        ClassPathResource resource = new ClassPathResource("data/seed-db.json");
        if (!resource.exists()) {
            log.warn("Seed data file data/seed-db.json not found on classpath, skipping seeding.");
            return;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(inputStream);

            // 1. Seed Users
            if (userRepository.count() == 0 && root.has("users")) {
                log.info("Seeding users from seed-db.json...");
                List<User> users = objectMapper.convertValue(root.get("users"), new TypeReference<List<User>>() {});
                for (User user : users) {
                    user.setId(null);
                }
                userRepository.saveAll(users);
                log.info("Successfully seeded {} users.", users.size());
            } else {
                log.info("Users table is not empty ({} records), skipping.", userRepository.count());
            }

            // 2. Seed Customers
            if (customerRepository.count() == 0 && root.has("customers")) {
                log.info("Seeding customers from seed-db.json...");
                List<Customer> customers = objectMapper.convertValue(root.get("customers"), new TypeReference<List<Customer>>() {});
                customerRepository.saveAll(customers);
                log.info("Successfully seeded {} customers.", customers.size());
            } else {
                log.info("Customers table is not empty ({} records), skipping.", customerRepository.count());
            }

            // 3. Seed CustomerStats
            if (customerStatsRepository.count() == 0 && root.has("customerStats")) {
                log.info("Seeding customer stats from seed-db.json...");
                List<CustomerStats> customerStats = objectMapper.convertValue(root.get("customerStats"), new TypeReference<List<CustomerStats>>() {});
                customerStatsRepository.saveAll(customerStats);
                log.info("Successfully seeded {} customer stats.", customerStats.size());
            } else {
                log.info("CustomerStats table is not empty ({} records), skipping.", customerStatsRepository.count());
            }

            // 4. Seed Products
            if (productRepository.count() == 0 && root.has("products")) {
                log.info("Seeding products from seed-db.json...");
                List<Product> products = objectMapper.convertValue(root.get("products"), new TypeReference<List<Product>>() {});
                productRepository.saveAll(products);
                log.info("Successfully seeded {} products.", products.size());
            } else {
                log.info("Products table is not empty ({} records), skipping.", productRepository.count());
            }

            // 5. Seed Orders (with bidirectional order item relationship)
            if (orderRepository.count() == 0 && root.has("orders")) {
                log.info("Seeding orders from seed-db.json...");
                List<Order> orders = objectMapper.convertValue(root.get("orders"), new TypeReference<List<Order>>() {});
                for (Order order : orders) {
                    if (order.getItems() != null) {
                        for (OrderItem item : order.getItems()) {
                            item.setOrder(order);
                        }
                    }
                }
                orderRepository.saveAll(orders);
                log.info("Successfully seeded {} orders.", orders.size());
            } else {
                log.info("Orders table is not empty ({} records), skipping.", orderRepository.count());
            }

            log.info("Database seeding process finished.");
        } catch (Exception e) {
            log.error("Failed to seed database from seed-db.json: {}", e.getMessage(), e);
            throw new RuntimeException("Error during database seeding", e);
        }
    }
}
