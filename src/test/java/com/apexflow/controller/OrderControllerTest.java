package com.apexflow.controller;

import com.apexflow.model.*;
import com.apexflow.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerStatsRepository customerStatsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        orderRepository.deleteAll();
        customerStatsRepository.deleteAll();
        productRepository.deleteAll();
        customerRepository.deleteAll();

        Customer c = new Customer();
        c.setId("CUST-100");
        c.setName("Mario Rossi");
        c.setStatus("In Attesa");
        customerRepository.save(c);

        Product p = new Product();
        p.setId("PROD-M-1");
        p.setName("SaaS Subscription");
        p.setDuration("Mensile");
        p.setPrice(100.0);
        productRepository.save(p);
    }

    @Test
    void testCreateOrderReturns201ExtendsSubscriptionAndUpdatesStats() throws Exception {
        Order order = new Order();
        order.setId("ORD-TEST-1");
        order.setCustomerId("CUST-100");
        order.setDate(LocalDateTime.now());
        order.setStatus("Pagato");
        order.setTotal(100.0);

        OrderItem item = new OrderItem();
        item.setProductId("PROD-M-1");
        item.setName("SaaS Subscription");
        item.setPrice(100.0);
        item.setQuantity(2);
        order.setItems(List.of(item));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("ORD-TEST-1"));

        Customer updatedCustomer = customerRepository.findById("CUST-100").orElseThrow();
        assertThat(updatedCustomer.getStatus()).isEqualTo("Attivo");
        assertThat(updatedCustomer.getSubscriptionEndDate()).isAfterOrEqualTo(LocalDate.now().plusMonths(2).minusDays(1));

        List<CustomerStats> stats = customerStatsRepository.findByCustomerId("CUST-100");
        assertThat(stats).isNotEmpty();
        assertThat(stats.get(0).getTotalRevenue()).isEqualTo(100.0);
        assertThat(stats.get(0).getTotalOrders()).isEqualTo(1);
    }

    @Test
    void testGetStatsRevenue() throws Exception {
        Order o1 = new Order();
        o1.setId("O1");
        o1.setStatus("Pagato");
        o1.setTotal(250.0);
        o1.setDate(LocalDateTime.of(2026, 3, 15, 10, 0));
        orderRepository.save(o1);

        mockMvc.perform(get("/api/orders/stats/revenue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(250.0));

        mockMvc.perform(get("/api/orders/stats/monthly-revenue?year=2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[2]").value(250.0));
    }

    @Test
    void testGetMonthlyRevenueMissingYearReturns400() throws Exception {
        mockMvc.perform(get("/api/orders/stats/monthly-revenue"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Il parametro 'year' è obbligatorio"));
    }

    @Test
    void testGetOrdersWithPaginationAndHeader() throws Exception {
        Order o1 = new Order();
        o1.setId("O1");
        o1.setStatus("Pagato");
        o1.setTotal(100.0);
        o1.setDate(LocalDateTime.now());
        orderRepository.save(o1);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$[0].id").value("O1"));
    }

    @Test
    void testGetOrdersWithCustomerNameLikeFilter() throws Exception {
        Customer c2 = new Customer();
        c2.setId("CUST-200");
        c2.setName("Luigi Bianchi");
        c2.setStatus("Attivo");
        customerRepository.save(c2);

        Order o1 = new Order();
        o1.setId("O1");
        o1.setCustomerId("CUST-100"); // Mario Rossi
        o1.setStatus("Pagato");
        o1.setDate(LocalDateTime.now());
        orderRepository.save(o1);

        Order o2 = new Order();
        o2.setId("O2");
        o2.setCustomerId("CUST-200"); // Luigi Bianchi
        o2.setStatus("Pagato");
        o2.setDate(LocalDateTime.now());
        orderRepository.save(o2);

        mockMvc.perform(get("/api/orders?customer.name_like=Mario"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$[0].id").value("O1"));

        mockMvc.perform(get("/api/orders?customer.name_like=NonExistent"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "0"))
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testCreateOrderWithYearlySubscriptionExtendsFromFutureDate() throws Exception {
        LocalDate futureEnd = LocalDate.now().plusMonths(6);
        Customer c = customerRepository.findById("CUST-100").orElseThrow();
        c.setSubscriptionEndDate(futureEnd);
        customerRepository.save(c);

        Product pYearly = new Product();
        pYearly.setId("PROD-Y-1");
        pYearly.setName("SaaS Enterprise Annual");
        pYearly.setDuration("Annuale");
        pYearly.setPrice(1200.0);
        productRepository.save(pYearly);

        Order order = new Order();
        order.setCustomerId("CUST-100");
        order.setStatus("Pagato");
        order.setTotal(1200.0);

        OrderItem item = new OrderItem();
        item.setProductId("PROD-Y-1");
        item.setName("SaaS Enterprise Annual");
        item.setPrice(1200.0);
        item.setQuantity(1);
        order.setItems(List.of(item));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty());

        Customer updatedCustomer = customerRepository.findById("CUST-100").orElseThrow();
        assertThat(updatedCustomer.getStatus()).isEqualTo("Attivo");
        assertThat(updatedCustomer.getSubscriptionEndDate()).isEqualTo(futureEnd.plusMonths(12));
    }

    @Test
    void testGetOrdersWithExpandCustomer() throws Exception {
        Order o = new Order();
        o.setId("O-EXPAND-1");
        o.setCustomerId("CUST-100");
        o.setStatus("Pagato");
        o.setTotal(150.0);
        o.setDate(LocalDateTime.now());
        orderRepository.save(o);

        mockMvc.perform(get("/api/orders?_expand=customer"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$[0].id").value("O-EXPAND-1"))
                .andExpect(jsonPath("$[0].customer.id").value("CUST-100"))
                .andExpect(jsonPath("$[0].customer.name").value("Mario Rossi"));
    }

    @Test
    void testGetOrderById_SuccessAndNotFound() throws Exception {
        Order o = new Order();
        o.setId("O-FIND-1");
        o.setCustomerId("CUST-100");
        o.setStatus("Pagato");
        o.setTotal(300.0);
        o.setDate(LocalDateTime.now());
        orderRepository.save(o);

        mockMvc.perform(get("/api/orders/O-FIND-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("O-FIND-1"))
                .andExpect(jsonPath("$.total").value(300.0))
                .andExpect(jsonPath("$.customer.name").value("Mario Rossi"));

        mockMvc.perform(get("/api/orders/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteOrder() throws Exception {
        Order o1 = new Order();
        o1.setId("O1");
        o1.setStatus("In attesa");
        o1.setTotal(50.0);
        o1.setDate(LocalDateTime.now());
        orderRepository.save(o1);

        mockMvc.perform(delete("/api/orders/O1"))
                .andExpect(status().isNoContent());

        assertThat(orderRepository.existsById("O1")).isFalse();

        mockMvc.perform(delete("/api/orders/O1"))
                .andExpect(status().isNotFound());
    }
}
