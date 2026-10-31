package com.apexflow.config;

import com.apexflow.model.Order;
import com.apexflow.repository.CustomerRepository;
import com.apexflow.repository.CustomerStatsRepository;
import com.apexflow.repository.OrderRepository;
import com.apexflow.repository.ProductRepository;
import com.apexflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class DataInitializerTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerStatsRepository customerStatsRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        customerStatsRepository.deleteAll();
        productRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();
        dataInitializer.seedDatabase();
    }

    @Test
    void testInitialDataSeededCounts() {
        assertThat(customerRepository.count()).isEqualTo(8);
        assertThat(productRepository.count()).isEqualTo(8);
        assertThat(orderRepository.count()).isEqualTo(7);
        assertThat(userRepository.count()).isEqualTo(2);
        assertThat(customerStatsRepository.count()).isEqualTo(5);
    }

    @Test
    void testCompleteDatabaseSeedingFromCleanState() {
        orderRepository.deleteAll();
        customerStatsRepository.deleteAll();
        productRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();

        dataInitializer.seedDatabase();

        assertThat(customerRepository.count()).isEqualTo(8);
        assertThat(productRepository.count()).isEqualTo(8);
        assertThat(orderRepository.count()).isEqualTo(7);
        assertThat(userRepository.count()).isEqualTo(2);
        assertThat(customerStatsRepository.count()).isEqualTo(5);
    }

    @Test
    void testSeedingIdempotency() {
        long initialCustomers = customerRepository.count();
        long initialProducts = productRepository.count();
        long initialOrders = orderRepository.count();
        long initialUsers = userRepository.count();
        long initialStats = customerStatsRepository.count();

        // Run initializer again when tables already contain data
        dataInitializer.seedDatabase();

        assertThat(customerRepository.count()).isEqualTo(initialCustomers);
        assertThat(productRepository.count()).isEqualTo(initialProducts);
        assertThat(orderRepository.count()).isEqualTo(initialOrders);
        assertThat(userRepository.count()).isEqualTo(initialUsers);
        assertThat(customerStatsRepository.count()).isEqualTo(initialStats);
    }

    @Test
    @Transactional
    void testOrderItemsBidirectionalRelationship() {
        Order order = orderRepository.findById("ORD-719").orElse(null);
        assertThat(order).isNotNull();
        assertThat(order.getItems()).isNotEmpty();
        assertThat(order.getItems().get(0).getOrder()).isNotNull();
        assertThat(order.getItems().get(0).getOrder().getId()).isEqualTo("ORD-719");
    }
}
