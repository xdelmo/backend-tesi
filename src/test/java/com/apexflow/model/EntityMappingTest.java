package com.apexflow.model;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityMappingTest {

    private final ObjectMapper objectMapper = new JsonMapper();

    @Test
    void testCustomerSubscriptionEndDateSerialization() throws Exception {
        Customer customer = new Customer();
        customer.setId("TEST-ID-1");
        customer.setName("Acme Corp");
        customer.setSubscriptionEndDate(LocalDate.of(2027, 5, 20));

        String json = objectMapper.writeValueAsString(customer);
        assertThat(json).contains("\"subscriptionEndDate\":\"2027-05-20\"");

        Customer deserialized = objectMapper.readValue(json, Customer.class);
        assertThat(deserialized.getSubscriptionEndDate()).isEqualTo(LocalDate.of(2027, 5, 20));
    }

    @Test
    void testCustomerStatsTransientField() {
        Customer customer = new Customer();
        assertThat(customer.getCustomerStats()).isNotNull().isEmpty();

        CustomerStats stats = new CustomerStats();
        stats.setId("CS-1");
        stats.setCustomerId(customer.getId());
        stats.setTotalRevenue(1500.0);
        stats.setTotalOrders(10);
        customer.setCustomerStats(Collections.singletonList(stats));

        assertThat(customer.getCustomerStats()).hasSize(1);
        assertThat(customer.getCustomerStats().get(0).getTotalRevenue()).isEqualTo(1500.0);
    }

    @Test
    void testUserTimestamps() {
        User user = new User();
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        assertThat(user.getCreatedAt()).isEqualTo(now);
        assertThat(user.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void testOrderItemsBidirectionalMapping() throws Exception {
        Order order = new Order();
        order.setId("ORD-999");
        order.setDate(LocalDateTime.now());

        OrderItem item = new OrderItem();
        item.setProductId("P1");
        item.setName("Product 1");
        item.setPrice(100.0);
        item.setQuantity(2);
        item.setOrder(order);

        order.getItems().add(item);

        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getOrder()).isEqualTo(order);

        // Verify JSON serialization doesn't infinite loop due to bidirectional relationship
        String json = objectMapper.writeValueAsString(order);
        assertThat(json).contains("\"id\":\"ORD-999\"");
        assertThat(json).contains("\"productId\":\"P1\"");
        assertThat(json).doesNotContain("\"order\":{");

        // Verify toString does not trigger StackOverflowError
        assertThat(order.toString()).isNotNull();
        assertThat(item.toString()).isNotNull();
    }
}
