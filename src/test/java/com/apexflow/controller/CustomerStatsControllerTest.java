package com.apexflow.controller;

import com.apexflow.model.CustomerStats;
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
public class CustomerStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerStatsRepository statsRepository;

    @BeforeEach
    void setup() {
        statsRepository.deleteAll();

        CustomerStats cs1 = new CustomerStats();
        cs1.setId("CS-1");
        cs1.setCustomerId("CUST-1");
        cs1.setTotalRevenue(500.0);
        cs1.setTotalOrders(2);
        statsRepository.save(cs1);

        CustomerStats cs2 = new CustomerStats();
        cs2.setId("CS-2");
        cs2.setCustomerId("CUST-2");
        cs2.setTotalRevenue(1200.0);
        cs2.setTotalOrders(5);
        statsRepository.save(cs2);
    }

    @Test
    void testFilterByCustomerId() throws Exception {
        mockMvc.perform(get("/api/customerStats?customerId=CUST-1"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("CS-1"))
                .andExpect(jsonPath("$[0].totalRevenue").value(500.0));
    }

    @Test
    void testGetStatsById() throws Exception {
        mockMvc.perform(get("/api/customerStats/CS-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("CS-1"))
                .andExpect(jsonPath("$.customerId").value("CUST-1"))
                .andExpect(jsonPath("$.totalRevenue").value(500.0));
    }

    @Test
    void testGetStatsByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/customerStats/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateStatsReturnsCreatedStatus() throws Exception {
        String newStatsJson = """
                {
                    "id": "CS-3",
                    "customerId": "CUST-3",
                    "totalRevenue": 350.0,
                    "totalOrders": 1
                }
                """;

        mockMvc.perform(post("/api/customerStats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newStatsJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("CS-3"))
                .andExpect(jsonPath("$.customerId").value("CUST-3"))
                .andExpect(jsonPath("$.totalRevenue").value(350.0));
    }

    @Test
    void testUpdateStats() throws Exception {
        String updateJson = """
                {
                    "totalRevenue": 750.0,
                    "totalOrders": 3
                }
                """;

        mockMvc.perform(patch("/api/customerStats/CS-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("CS-1"))
                .andExpect(jsonPath("$.totalRevenue").value(750.0))
                .andExpect(jsonPath("$.totalOrders").value(3));
    }

    @Test
    void testUpdateStatsNotFound() throws Exception {
        String updateJson = """
                {
                    "totalRevenue": 100.0
                }
                """;

        mockMvc.perform(patch("/api/customerStats/NONEXISTENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isNotFound());
    }
}
