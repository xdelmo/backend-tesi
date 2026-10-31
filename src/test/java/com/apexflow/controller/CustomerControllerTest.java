package com.apexflow.controller;

import com.apexflow.model.Customer;
import com.apexflow.model.CustomerStats;
import com.apexflow.repository.CustomerRepository;
import com.apexflow.repository.CustomerStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerStatsRepository customerStatsRepository;

    @BeforeEach
    void setup() {
        customerStatsRepository.deleteAll();
        customerRepository.deleteAll();

        Customer c = new Customer();
        c.setId("CUST-1");
        c.setName("Alpha Corp");
        c.setEmail("contact@alpha.com");
        c.setStatus("Attivo");
        customerRepository.save(c);
    }

    @Test
    void testGetCustomersWithPaginationAndHeader() throws Exception {
        mockMvc.perform(get("/api/customers?_page=1&_limit=10"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$[0].name").value("Alpha Corp"));
    }

    @Test
    void testCreateCustomerReturnsCreatedStatus() throws Exception {
        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Beta Ltd\",\"email\":\"beta@ltd.com\",\"status\":\"Attivo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Beta Ltd"));
    }

    @Test
    void testGetCustomerById_Success() throws Exception {
        mockMvc.perform(get("/api/customers/CUST-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("CUST-1"))
                .andExpect(jsonPath("$.name").value("Alpha Corp"));
    }

    @Test
    void testGetCustomerById_NotFound() throws Exception {
        mockMvc.perform(get("/api/customers/NON-EXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateCustomer_Success() throws Exception {
        mockMvc.perform(patch("/api/customers/CUST-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Alpha Corp Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alpha Corp Updated"));
    }

    @Test
    void testUpdateCustomer_NotFound() throws Exception {
        mockMvc.perform(patch("/api/customers/NON-EXISTENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ghost Corp\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteCustomer_SuccessAndNotFound() throws Exception {
        mockMvc.perform(delete("/api/customers/CUST-1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/customers/CUST-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetCustomersWithEmbedCustomerStats() throws Exception {
        CustomerStats stats = new CustomerStats();
        stats.setId("STAT-1");
        stats.setCustomerId("CUST-1");
        stats.setTotalRevenue(1500.0);
        stats.setTotalOrders(5);
        customerStatsRepository.save(stats);

        mockMvc.perform(get("/api/customers?_embed=customerStats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerStats", hasSize(1)))
                .andExpect(jsonPath("$[0].customerStats[0].totalRevenue").value(1500.0));
    }

    @Test
    void testGetCustomersWithCustomSortTotalRevenue() throws Exception {
        Customer c2 = new Customer();
        c2.setId("CUST-2");
        c2.setName("Omega LLC");
        c2.setEmail("contact@omega.com");
        customerRepository.save(c2);

        CustomerStats s1 = new CustomerStats();
        s1.setId("STAT-1");
        s1.setCustomerId("CUST-1");
        s1.setTotalRevenue(100.0);
        customerStatsRepository.save(s1);

        CustomerStats s2 = new CustomerStats();
        s2.setId("STAT-2");
        s2.setCustomerId("CUST-2");
        s2.setTotalRevenue(500.0);
        customerStatsRepository.save(s2);

        mockMvc.perform(get("/api/customers?_sort=customerStats.totalRevenue&_order=desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("CUST-2"))
                .andExpect(jsonPath("$[1].id").value("CUST-1"));
    }
}
