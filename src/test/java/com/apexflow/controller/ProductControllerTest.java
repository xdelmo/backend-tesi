package com.apexflow.controller;

import com.apexflow.model.Product;
import com.apexflow.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setup() {
        productRepository.deleteAll();
        Product p = new Product();
        p.setId("PROD-1");
        p.setName("Cloud DB");
        p.setStatus("Attivo");
        p.setCategory("Database");
        p.setPrice(199.99);
        p.setDuration("Mensile");
        p.setDescription("Cloud database service");
        productRepository.save(p);
    }

    @Test
    void testGetProductsPaginationAndHeader() throws Exception {
        mockMvc.perform(get("/api/products?_page=1&_limit=10"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$[0].name").value("Cloud DB"));
    }

    @Test
    void testCreateProductReturns201() throws Exception {
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"AI Analytics\",\"price\":500.0,\"duration\":\"Mensile\",\"status\":\"Attivo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("AI Analytics"));
    }

    @Test
    void testGetProductById_Success() throws Exception {
        mockMvc.perform(get("/api/products/PROD-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("PROD-1"))
                .andExpect(jsonPath("$.name").value("Cloud DB"));
    }

    @Test
    void testGetProductById_NotFound() throws Exception {
        mockMvc.perform(get("/api/products/NON-EXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateProduct_Success() throws Exception {
        mockMvc.perform(patch("/api/products/PROD-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Cloud DB Pro\",\"price\":299.99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cloud DB Pro"))
                .andExpect(jsonPath("$.price").value(299.99));
    }

    @Test
    void testUpdateProduct_NotFound() throws Exception {
        mockMvc.perform(patch("/api/products/NON-EXISTENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ghost Product\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteProduct_SuccessAndNotFound() throws Exception {
        mockMvc.perform(delete("/api/products/PROD-1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/products/PROD-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetProductsWithFilter() throws Exception {
        Product p2 = new Product();
        p2.setId("PROD-2");
        p2.setName("Security Suite");
        p2.setStatus("Inattivo");
        productRepository.save(p2);

        mockMvc.perform(get("/api/products?status=Attivo"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$[0].id").value("PROD-1"));

        mockMvc.perform(get("/api/products?name_like=Security"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$[0].id").value("PROD-2"));
    }
}
