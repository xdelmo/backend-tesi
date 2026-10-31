# Backend Full Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the complete Spring Boot backend for ApexFlow, providing 100% contract compatibility with the Angular frontend (pagination `_page`/`_limit`, sorting `_sort`/`_order`, dynamic filters `*_like`, `X-Total-Count` header, order business logic, subscription expiration scheduler, and automatic `db.json` seeding).

**Architecture:** Layered architecture (REST Controller -> Transactional Service -> JPA Repository + Criteria API Specification Executor). A reusable `QueryFilterParser` translates Angular/json-server query params to Spring Data `Pageable` and `Specification<T>`.

**Tech Stack:** Java 21, Spring Boot 4.0.1, Spring Data JPA, Spring Security, PostgreSQL, Lombok, Jackson, JUnit 5, Mockito, MockMvc.

## Global Constraints

- Backend repository path: `/Users/emanueledelmonte/Documents/Università/Tesi/backend-tesi`
- Frontend repository path: `/Users/emanueledelmonte/Documents/Università/Tesi/dashboard-tesi`
- Base API path: `/api`
- Exposed Header: `X-Total-Count` in CORS and responses
- Page numbering: Frontend sends 1-indexed `_page`; Spring Data uses 0-indexed page (`page - 1`)
- Seeding: Auto-populate database on startup if empty using `seed-db.json` mirrored from `db.json`

---

### Task 1: CORS & Security Configuration

**Files:**
- Modify: `src/main/java/com/apexflow/config/SecurityConfig.java`
- Create: `src/test/java/com/apexflow/config/SecurityConfigTest.java`

**Interfaces:**
- Consumes: Spring Security `HttpSecurity`, `CorsConfigurationSource`
- Produces: Permissive SecurityFilterChain with CORS exposing `X-Total-Count`

- [ ] **Step 1: Write the failing test for CORS configuration and X-Total-Count exposure**

Create `src/test/java/com/apexflow/config/SecurityConfigTest.java`:
```java
package com.apexflow.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SecurityConfigTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testCorsHeadersAndPermitAll() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", "http://localhost:4200");
        headers.set("Access-Control-Request-Method", "GET");
        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/products",
                HttpMethod.OPTIONS,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NO_CONTENT);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).contains("http://localhost:4200");
        assertThat(response.getHeaders().getAccessControlExposeHeaders()).contains("X-Total-Count");
    }
}
```

- [ ] **Step 2: Run test to verify it fails or does not expose X-Total-Count**

Run: `./mvnw test -Dtest=SecurityConfigTest`
Expected: FAIL (missing `Access-Control-Expose-Headers` or CORS origin rejection)

- [ ] **Step 3: Update `SecurityConfig.java` with complete CORS bean and permitAll**

Modify `src/main/java/com/apexflow/config/SecurityConfig.java`:
```java
package com.apexflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Total-Count", "Authorization"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=SecurityConfigTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/apexflow/config/SecurityConfig.java src/test/java/com/apexflow/config/SecurityConfigTest.java
git commit -m "feat(security): configure CORS with exposed X-Total-Count header"
```

---

### Task 2: Domain Entities Alignment

**Files:**
- Modify: `src/main/java/com/apexflow/model/Customer.java`
- Modify: `src/main/java/com/apexflow/model/User.java`
- Modify: `src/main/java/com/apexflow/model/Order.java`
- Modify: `src/main/java/com/apexflow/model/OrderItem.java`
- Create: `src/test/java/com/apexflow/model/EntityMappingTest.java`

**Interfaces:**
- Consumes: JPA / Hibernate annotations
- Produces: Fully aligned entity classes supporting `subscriptionEndDate`, `createdAt`, `updatedAt`, and bidirectional Order-Item relation.

- [ ] **Step 1: Write test for entity attributes and JSON serialization**

Create `src/test/java/com/apexflow/model/EntityMappingTest.java`:
```java
package com.apexflow.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityMappingTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

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
    void testOrderItemsBidirectionalMapping() {
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
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=EntityMappingTest`
Expected: FAIL (cannot resolve method `setSubscriptionEndDate` on `Customer`)

- [ ] **Step 3: Update `Customer.java`, `User.java`, `Order.java`, `OrderItem.java`**

Modify `src/main/java/com/apexflow/model/Customer.java`:
```java
package com.apexflow.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
@Data
public class Customer {

    @Id
    private String id;

    private String name;
    private String email;
    private String company;
    private String status;
    private String industry;
    private String plan;
    private LocalDate subscriptionEndDate;

    @Transient
    private List<CustomerStats> customerStats = new ArrayList<>();
}
```

Modify `src/main/java/com/apexflow/model/User.java`:
```java
package com.apexflow.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String password;
    private String name;
    private String role;
    private String avatar;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=EntityMappingTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/apexflow/model/Customer.java src/main/java/com/apexflow/model/User.java src/test/java/com/apexflow/model/EntityMappingTest.java
git commit -m "feat(model): add subscriptionEndDate to Customer and timestamps to User"
```

---

### Task 3: Query Filter Parser & Specification Builder

**Files:**
- Create: `src/main/java/com/apexflow/util/QueryFilterParser.java`
- Modify: `src/main/java/com/apexflow/repository/CustomerRepository.java`
- Modify: `src/main/java/com/apexflow/repository/OrderRepository.java`
- Modify: `src/main/java/com/apexflow/repository/ProductRepository.java`
- Modify: `src/main/java/com/apexflow/repository/UserRepository.java`
- Modify: `src/main/java/com/apexflow/repository/CustomerStatsRepository.java`
- Create: `src/test/java/com/apexflow/util/QueryFilterParserTest.java`

**Interfaces:**
- Consumes: `Map<String, String>` query parameters
- Produces: `Pageable getPageable(Map<String, String> params)`, `Specification<T> parse(Map<String, String> params)`

- [ ] **Step 1: Write test for QueryFilterParser**

Create `src/test/java/com/apexflow/util/QueryFilterParserTest.java`:
```java
package com.apexflow.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryFilterParserTest {

    @Test
    void testPageableExtractionDefaults() {
        Map<String, String> params = new HashMap<>();
        Pageable pageable = QueryFilterParser.getPageable(params);

        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
    }

    @Test
    void testPageableExtractionCustom() {
        Map<String, String> params = new HashMap<>();
        params.put("_page", "2");
        params.put("_limit", "25");
        params.put("_sort", "name");
        params.put("_order", "desc");

        Pageable pageable = QueryFilterParser.getPageable(params);

        assertThat(pageable.getPageNumber()).isEqualTo(1); // 1-indexed to 0-indexed
        assertThat(pageable.getPageSize()).isEqualTo(25);
        assertThat(pageable.getSort().getOrderFor("name")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.DESC);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=QueryFilterParserTest`
Expected: FAIL (`QueryFilterParser` does not exist)

- [ ] **Step 3: Implement `QueryFilterParser.java` and update repositories**

Create `src/main/java/com/apexflow/util/QueryFilterParser.java`:
```java
package com.apexflow.util;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

public class QueryFilterParser {

    private static final Set<String> RESERVED_KEYS = Set.of(
            "_page", "_limit", "_sort", "_order", "_embed", "_expand", "_refresh", "q"
    );

    public static Pageable getPageable(Map<String, String> params) {
        int page = 1;
        if (params.containsKey("_page")) {
            try {
                page = Math.max(1, Integer.parseInt(params.get("_page")));
            } catch (NumberFormatException ignored) {}
        }

        int limit = 10;
        if (params.containsKey("_limit")) {
            try {
                limit = Math.max(1, Integer.parseInt(params.get("_limit")));
            } catch (NumberFormatException ignored) {}
        }

        Sort sort = Sort.unsorted();
        if (params.containsKey("_sort")) {
            String sortField = params.get("_sort");
            // ignore complex nested sort that cannot be mapped directly
            if (!sortField.contains(".")) {
                Sort.Direction direction = "desc".equalsIgnoreCase(params.get("_order"))
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;
                sort = Sort.by(direction, sortField);
            }
        }

        return PageRequest.of(page - 1, limit, sort);
    }

    public static <T> Specification<T> parse(Map<String, String> params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (RESERVED_KEYS.contains(key) || value == null || value.trim().isEmpty()) {
                    continue;
                }

                if (key.endsWith("_like")) {
                    String field = key.substring(0, key.length() - 5);
                    Path<String> path = getPath(root, field);
                    if (path != null) {
                        predicates.add(cb.like(cb.lower(path), "%" + value.toLowerCase() + "%"));
                    }
                } else if (!key.contains(".")) {
                    try {
                        Path<Object> path = root.get(key);
                        predicates.add(cb.equal(path, value));
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> Path<String> getPath(jakarta.persistence.criteria.Root<T> root, String field) {
        try {
            return (Path<String>) (Path<?>) root.get(field);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
```

Update each repository to extend `JpaSpecificationExecutor<T>`:
Modify `src/main/java/com/apexflow/repository/CustomerRepository.java`:
```java
package com.apexflow.repository;

import com.apexflow.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String>, JpaSpecificationExecutor<Customer> {
}
```
(Apply the same pattern to `OrderRepository.java`, `ProductRepository.java`, `UserRepository.java`, `CustomerStatsRepository.java`).

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=QueryFilterParserTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/apexflow/util/QueryFilterParser.java src/main/java/com/apexflow/repository/*.java src/test/java/com/apexflow/util/QueryFilterParserTest.java
git commit -m "feat(query): implement QueryFilterParser and add JpaSpecificationExecutor to repositories"
```

---

### Task 4: Customer Service & Controller (Pagination, Filters, Stats Embed & Sorting)

**Files:**
- Create: `src/main/java/com/apexflow/service/CustomerService.java`
- Modify: `src/main/java/com/apexflow/controller/CustomerController.java`
- Create: `src/test/java/com/apexflow/controller/CustomerControllerTest.java`

**Interfaces:**
- Consumes: `CustomerRepository`, `CustomerStatsRepository`, `QueryFilterParser`
- Produces: `ResponseEntity<List<Customer>>` with `X-Total-Count` header, CRUD operations.

- [ ] **Step 1: Write integration test for CustomerController pagination and headers**

Create `src/test/java/com/apexflow/controller/CustomerControllerTest.java`:
```java
package com.apexflow.controller;

import com.apexflow.model.Customer;
import com.apexflow.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setup() {
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
}
```

- [ ] **Step 2: Run test to verify it fails (missing X-Total-Count header)**

Run: `./mvnw test -Dtest=CustomerControllerTest`
Expected: FAIL

- [ ] **Step 3: Create `CustomerService.java` and update `CustomerController.java`**

Create `src/main/java/com/apexflow/service/CustomerService.java`:
```java
package com.apexflow.service;

import com.apexflow.model.Customer;
import com.apexflow.model.CustomerStats;
import com.apexflow.repository.CustomerRepository;
import com.apexflow.repository.CustomerStatsRepository;
import com.apexflow.util.QueryFilterParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerStatsRepository customerStatsRepository;

    public Page<Customer> getCustomers(Map<String, String> params) {
        Pageable pageable = QueryFilterParser.getPageable(params);
        Specification<Customer> spec = QueryFilterParser.parse(params);

        // Check if custom sorting by customerStats.totalRevenue is requested
        if ("customerStats.totalRevenue".equals(params.get("_sort"))) {
            List<Customer> allFiltered = customerRepository.findAll(spec);
            List<CustomerStats> allStats = customerStatsRepository.findAll();
            Map<String, Double> revenueMap = new HashMap<>();
            for (CustomerStats s : allStats) {
                revenueMap.put(s.getCustomerId(), s.getTotalRevenue() != null ? s.getTotalRevenue() : 0.0);
            }

            int order = "desc".equalsIgnoreCase(params.get("_order")) ? -1 : 1;
            allFiltered.sort((a, b) -> {
                double revA = revenueMap.getOrDefault(a.getId(), 0.0);
                double revB = revenueMap.getOrDefault(b.getId(), 0.0);
                return Double.compare(revA, revB) * order;
            });

            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allFiltered.size());
            List<Customer> pagedList = start > allFiltered.size() ? List.of() : allFiltered.subList(start, end);

            embedStats(pagedList);
            return new PageImpl<>(pagedList, pageable, allFiltered.size());
        }

        Page<Customer> page = customerRepository.findAll(spec, pageable);
        if (params.containsKey("_embed") && "customerStats".equals(params.get("_embed"))) {
            embedStats(page.getContent());
        }
        return page;
    }

    private void embedStats(List<Customer> customers) {
        for (Customer c : customers) {
            List<CustomerStats> stats = customerStatsRepository.findByCustomerId(c.getId());
            c.setCustomerStats(stats != null ? stats : List.of());
        }
    }

    public Optional<Customer> getCustomerById(String id) {
        return customerRepository.findById(id).map(c -> {
            embedStats(List.of(c));
            return c;
        });
    }

    public Customer createCustomer(Customer customer) {
        if (customer.getId() == null || customer.getId().isBlank()) {
            customer.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        }
        return customerRepository.save(customer);
    }

    public Optional<Customer> updateCustomer(String id, Customer details) {
        return customerRepository.findById(id).map(c -> {
            if (details.getName() != null) c.setName(details.getName());
            if (details.getEmail() != null) c.setEmail(details.getEmail());
            if (details.getCompany() != null) c.setCompany(details.getCompany());
            if (details.getStatus() != null) c.setStatus(details.getStatus());
            if (details.getIndustry() != null) c.setIndustry(details.getIndustry());
            if (details.getPlan() != null) c.setPlan(details.getPlan());
            if (details.getSubscriptionEndDate() != null) c.setSubscriptionEndDate(details.getSubscriptionEndDate());
            return customerRepository.save(c);
        });
    }

    public boolean deleteCustomer(String id) {
        if (customerRepository.existsById(id)) {
            customerRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
```

In `CustomerStatsRepository.java`, add method:
```java
List<CustomerStats> findByCustomerId(String customerId);
```

Modify `src/main/java/com/apexflow/controller/CustomerController.java`:
```java
package com.apexflow.controller;

import com.apexflow.model.Customer;
import com.apexflow.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @GetMapping
    public ResponseEntity<List<Customer>> getCustomers(@RequestParam Map<String, String> allParams) {
        Page<Customer> page = customerService.getCustomers(allParams);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Total-Count", String.valueOf(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable String id) {
        return customerService.getCustomerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customer) {
        return ResponseEntity.ok(customerService.createCustomer(customer));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(@PathVariable String id, @RequestBody Customer customerDetails) {
        return customerService.updateCustomer(id, customerDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String id) {
        return customerService.deleteCustomer(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=CustomerControllerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/apexflow/service/CustomerService.java src/main/java/com/apexflow/controller/CustomerController.java src/main/java/com/apexflow/repository/CustomerStatsRepository.java src/test/java/com/apexflow/controller/CustomerControllerTest.java
git commit -m "feat(customer): add service layer, pagination, stats embed and X-Total-Count header"
```

---

### Task 5: Order Service & Stats Endpoints (`/stats/revenue`, `/stats/monthly-revenue`)

**Files:**
- Create: `src/main/java/com/apexflow/service/OrderService.java`
- Modify: `src/main/java/com/apexflow/controller/OrderController.java`
- Create: `src/test/java/com/apexflow/controller/OrderControllerTest.java`

**Interfaces:**
- Consumes: `OrderRepository`, `CustomerRepository`, `CustomerStatsRepository`, `ProductRepository`
- Produces: `POST /api/orders` with atomic customer subscription & stats update; `GET /api/orders/stats/revenue`; `GET /api/orders/stats/monthly-revenue`.

- [ ] **Step 1: Write integration test for Order creation business logic and stats**

Create `src/test/java/com/apexflow/controller/OrderControllerTest.java`:
```java
package com.apexflow.controller;

import com.apexflow.model.*;
import com.apexflow.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void testCreateOrderExtendsSubscriptionAndUpdatesStats() throws Exception {
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
                .andExpect(status().isOk());

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
                .andExpect(jsonPath("$[2]").value(250.0)); // Month 2 is March (0-indexed)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=OrderControllerTest`
Expected: FAIL

- [ ] **Step 3: Create `OrderService.java` and update `OrderController.java`**

Create `src/main/java/com/apexflow/service/OrderService.java`:
```java
package com.apexflow.service;

import com.apexflow.model.*;
import com.apexflow.repository.*;
import com.apexflow.util.QueryFilterParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerStatsRepository customerStatsRepository;

    @Autowired
    private ProductRepository productRepository;

    public Page<Order> getOrders(Map<String, String> params) {
        Pageable pageable = QueryFilterParser.getPageable(params);
        Specification<Order> spec = QueryFilterParser.parse(params);

        if (params.containsKey("customer.name_like")) {
            String nameQuery = params.get("customer.name_like").toLowerCase();
            List<Customer> matchingCustomers = customerRepository.findAll((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + nameQuery + "%")
            );
            List<String> customerIds = matchingCustomers.stream().map(Customer::getId).toList();

            Specification<Order> customerSpec = (root, query, cb) ->
                    customerIds.isEmpty() ? cb.disjunction() : root.get("customerId").in(customerIds);
            spec = spec.and(customerSpec);
        }

        return orderRepository.findAll(spec, pageable);
    }

    public Optional<Order> getOrderById(String id) {
        return orderRepository.findById(id);
    }

    public Order createOrder(Order order) {
        if (order.getId() == null || order.getId().isBlank()) {
            order.setId("ORD-" + (int) (Math.random() * 9000 + 1000));
        }
        if (order.getDate() == null) {
            order.setDate(LocalDateTime.now());
        }

        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                item.setOrder(order);
            }
        }

        Order savedOrder = orderRepository.save(order);

        // Business Logic: Handle subscription extension and stats update
        if (order.getCustomerId() != null) {
            customerRepository.findById(order.getCustomerId()).ifPresent(customer -> {
                boolean hasSubscription = false;
                int monthsToAdd = 0;

                if (order.getItems() != null) {
                    for (OrderItem item : order.getItems()) {
                        if (item.getProductId() != null) {
                            Optional<Product> prodOpt = productRepository.findById(item.getProductId());
                            if (prodOpt.isPresent()) {
                                Product product = prodOpt.get();
                                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                                if ("Mensile".equalsIgnoreCase(product.getDuration())) {
                                    hasSubscription = true;
                                    monthsToAdd += qty;
                                } else if ("Annuale".equalsIgnoreCase(product.getDuration())) {
                                    hasSubscription = true;
                                    monthsToAdd += qty * 12;
                                }
                            }
                        }
                    }
                }

                if (hasSubscription) {
                    customer.setStatus("Attivo");
                    LocalDate baseDate = LocalDate.now();
                    if (customer.getSubscriptionEndDate() != null && customer.getSubscriptionEndDate().isAfter(baseDate)) {
                        baseDate = customer.getSubscriptionEndDate();
                    }
                    customer.setSubscriptionEndDate(baseDate.plusMonths(monthsToAdd));
                    customerRepository.save(customer);
                }

                // Update CustomerStats
                List<CustomerStats> statsList = customerStatsRepository.findByCustomerId(customer.getId());
                CustomerStats stats;
                if (statsList != null && !statsList.isEmpty()) {
                    stats = statsList.get(0);
                } else {
                    stats = new CustomerStats();
                    stats.setId("cs-" + System.currentTimeMillis());
                    stats.setCustomerId(customer.getId());
                    stats.setTotalOrders(0);
                    stats.setTotalRevenue(0.0);
                }

                double orderTotal = order.getTotal() != null ? order.getTotal() : 0.0;
                stats.setTotalOrders(stats.getTotalOrders() + 1);
                stats.setTotalRevenue(stats.getTotalRevenue() + orderTotal);
                stats.setLastOrderDate(order.getDate());
                customerStatsRepository.save(stats);
            });
        }

        return savedOrder;
    }

    public boolean deleteOrder(String id) {
        if (orderRepository.existsById(id)) {
            orderRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public double getTotalRevenue() {
        return orderRepository.findAll().stream()
                .filter(o -> "Pagato".equalsIgnoreCase(o.getStatus()))
                .mapToDouble(o -> o.getTotal() != null ? o.getTotal() : 0.0)
                .sum();
    }

    public List<Double> getMonthlyRevenue(int year) {
        Double[] monthly = new Double[12];
        Arrays.fill(monthly, 0.0);

        for (Order o : orderRepository.findAll()) {
            if ("Pagato".equalsIgnoreCase(o.getStatus()) && o.getDate() != null && o.getDate().getYear() == year) {
                int month = o.getDate().getMonthValue() - 1; // 0 to 11
                monthly[month] += (o.getTotal() != null ? o.getTotal() : 0.0);
            }
        }
        return Arrays.asList(monthly);
    }
}
```

Modify `src/main/java/com/apexflow/controller/OrderController.java`:
```java
package com.apexflow.controller;

import com.apexflow.model.Order;
import com.apexflow.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    public ResponseEntity<List<Order>> getOrders(@RequestParam Map<String, String> allParams) {
        Page<Order> page = orderService.getOrders(allParams);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Total-Count", String.valueOf(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable String id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        return ResponseEntity.ok(orderService.createOrder(order));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String id) {
        return orderService.deleteOrder(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/stats/revenue")
    public ResponseEntity<Map<String, Double>> getTotalRevenue() {
        return ResponseEntity.ok(Map.of("totalRevenue", orderService.getTotalRevenue()));
    }

    @GetMapping("/stats/monthly-revenue")
    public ResponseEntity<?> getMonthlyRevenue(@RequestParam(required = false) Integer year) {
        if (year == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Il parametro 'year' è obbligatorio"));
        }
        return ResponseEntity.ok(orderService.getMonthlyRevenue(year));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=OrderControllerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/apexflow/service/OrderService.java src/main/java/com/apexflow/controller/OrderController.java src/test/java/com/apexflow/controller/OrderControllerTest.java
git commit -m "feat(order): implement order creation with subscription logic and revenue stats"
```

---

### Task 6: Product Service & Controller (Pagination, Soft-Delete)

**Files:**
- Create: `src/main/java/com/apexflow/service/ProductService.java`
- Modify: `src/main/java/com/apexflow/controller/ProductController.java`
- Create: `src/test/java/com/apexflow/controller/ProductControllerTest.java`

- [ ] **Step 1: Write integration test for Product pagination and X-Total-Count**

Create `src/test/java/com/apexflow/controller/ProductControllerTest.java`:
```java
package com.apexflow.controller;

import com.apexflow.model.Product;
import com.apexflow.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        productRepository.save(p);
    }

    @Test
    void testGetProductsPaginationAndHeader() throws Exception {
        mockMvc.perform(get("/api/products?_page=1&_limit=10"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$[0].name").value("Cloud DB"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=ProductControllerTest`
Expected: FAIL (missing header)

- [ ] **Step 3: Create `ProductService.java` and update `ProductController.java`**

Create `src/main/java/com/apexflow/service/ProductService.java`:
```java
package com.apexflow.service;

import com.apexflow.model.Product;
import com.apexflow.repository.ProductRepository;
import com.apexflow.util.QueryFilterParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public Page<Product> getProducts(Map<String, String> params) {
        Pageable pageable = QueryFilterParser.getPageable(params);
        Specification<Product> spec = QueryFilterParser.parse(params);
        return productRepository.findAll(spec, pageable);
    }

    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    public Product createProduct(Product product) {
        if (product.getId() == null || product.getId().isBlank()) {
            product.setId("PROD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        return productRepository.save(product);
    }

    public Optional<Product> updateProduct(String id, Product details) {
        return productRepository.findById(id).map(p -> {
            if (details.getName() != null) p.setName(details.getName());
            if (details.getDescription() != null) p.setDescription(details.getDescription());
            if (details.getCategory() != null) p.setCategory(details.getCategory());
            if (details.getPrice() != null) p.setPrice(details.getPrice());
            if (details.getDuration() != null) p.setDuration(details.getDuration());
            if (details.getStatus() != null) p.setStatus(details.getStatus());
            return productRepository.save(p);
        });
    }

    public boolean deleteProduct(String id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
```

Modify `src/main/java/com/apexflow/controller/ProductController.java`:
```java
package com.apexflow.controller;

import com.apexflow.model.Product;
import com.apexflow.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public ResponseEntity<List<Product>> getProducts(@RequestParam Map<String, String> allParams) {
        Page<Product> page = productService.getProducts(allParams);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Total-Count", String.valueOf(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return ResponseEntity.ok(productService.createProduct(product));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable String id, @RequestBody Product productDetails) {
        return productService.updateProduct(id, productDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        return productService.deleteProduct(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=ProductControllerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/apexflow/service/ProductService.java src/main/java/com/apexflow/controller/ProductController.java src/test/java/com/apexflow/controller/ProductControllerTest.java
git commit -m "feat(product): implement ProductService with pagination and X-Total-Count header"
```

---

### Task 7: User Service & Controller (Full CRUD, Login Compatibility)

**Files:**
- Create: `src/main/java/com/apexflow/service/UserService.java`
- Modify: `src/main/java/com/apexflow/controller/UserController.java`
- Create: `src/test/java/com/apexflow/controller/UserControllerTest.java`

- [ ] **Step 1: Write integration test for User CRUD and login compatibility**

Create `src/test/java/com/apexflow/controller/UserControllerTest.java`:
```java
package com.apexflow.controller;

import com.apexflow.model.User;
import com.apexflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        User u = new User();
        u.setEmail("admin@demo.com");
        u.setPassword("password");
        u.setName("Admin User");
        u.setRole("admin");
        userRepository.save(u);
    }

    @Test
    void testAuthLoginCompatibilityQuery() throws Exception {
        mockMvc.perform(get("/api/users?email=admin@demo.com&password=password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("admin@demo.com"))
                .andExpect(header().string("X-Total-Count", "1"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=UserControllerTest`
Expected: FAIL

- [ ] **Step 3: Create `UserService.java` and update `UserController.java`**

Create `src/main/java/com/apexflow/service/UserService.java`:
```java
package com.apexflow.service;

import com.apexflow.model.User;
import com.apexflow.repository.UserRepository;
import com.apexflow.util.QueryFilterParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public Page<User> getUsers(Map<String, String> params) {
        Pageable pageable = QueryFilterParser.getPageable(params);
        Specification<User> spec = QueryFilterParser.parse(params);
        return userRepository.findAll(spec, pageable);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public Optional<User> updateUser(Long id, User details) {
        return userRepository.findById(id).map(u -> {
            if (details.getName() != null) u.setName(details.getName());
            if (details.getEmail() != null) u.setEmail(details.getEmail());
            if (details.getPassword() != null && !details.getPassword().isBlank()) u.setPassword(details.getPassword());
            if (details.getRole() != null) u.setRole(details.getRole());
            if (details.getAvatar() != null) u.setAvatar(details.getAvatar());
            return userRepository.save(u);
        });
    }

    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
```

Modify `src/main/java/com/apexflow/controller/UserController.java`:
```java
package com.apexflow.controller;

import com.apexflow.model.User;
import com.apexflow.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getUsers(@RequestParam Map<String, String> allParams) {
        Page<User> page = userService.getUsers(allParams);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Total-Count", String.valueOf(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.ok(userService.createUser(user));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        return userService.updateUser(id, userDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=UserControllerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/apexflow/service/UserService.java src/main/java/com/apexflow/controller/UserController.java src/test/java/com/apexflow/controller/UserControllerTest.java
git commit -m "feat(user): add UserService, full CRUD and query support for login compatibility"
```

---

### Task 8: CustomerStats Service & Controller

**Files:**
- Create: `src/main/java/com/apexflow/service/CustomerStatsService.java`
- Modify: `src/main/java/com/apexflow/controller/CustomerStatsController.java`
- Create: `src/test/java/com/apexflow/controller/CustomerStatsControllerTest.java`

- [ ] **Step 1: Write integration test for CustomerStats**

Create `src/test/java/com/apexflow/controller/CustomerStatsControllerTest.java`:
```java
package com.apexflow.controller;

import com.apexflow.model.CustomerStats;
import com.apexflow.repository.CustomerStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        CustomerStats cs = new CustomerStats();
        cs.setId("CS-1");
        cs.setCustomerId("CUST-1");
        cs.setTotalRevenue(500.0);
        cs.setTotalOrders(2);
        statsRepository.save(cs);
    }

    @Test
    void testFilterByCustomerId() throws Exception {
        mockMvc.perform(get("/api/customerStats?customerId=CUST-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalRevenue").value(500.0));
    }
}
```

- [ ] **Step 2: Run test to verify it fails or needs updates**

Run: `./mvnw test -Dtest=CustomerStatsControllerTest`
Expected: FAIL (controller currently does not filter by parameters)

- [ ] **Step 3: Create `CustomerStatsService.java` and update `CustomerStatsController.java`**

Create `src/main/java/com/apexflow/service/CustomerStatsService.java`:
```java
package com.apexflow.service;

import com.apexflow.model.CustomerStats;
import com.apexflow.repository.CustomerStatsRepository;
import com.apexflow.util.QueryFilterParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class CustomerStatsService {

    @Autowired
    private CustomerStatsRepository statsRepository;

    public Page<CustomerStats> getStats(Map<String, String> params) {
        Pageable pageable = QueryFilterParser.getPageable(params);
        Specification<CustomerStats> spec = QueryFilterParser.parse(params);
        return statsRepository.findAll(spec, pageable);
    }

    public Optional<CustomerStats> getStatsById(String id) {
        return statsRepository.findById(id);
    }

    public CustomerStats saveStats(CustomerStats stats) {
        return statsRepository.save(stats);
    }

    public Optional<CustomerStats> updateStats(String id, CustomerStats details) {
        return statsRepository.findById(id).map(s -> {
            if (details.getTotalRevenue() != null) s.setTotalRevenue(details.getTotalRevenue());
            if (details.getTotalOrders() != null) s.setTotalOrders(details.getTotalOrders());
            if (details.getLastOrderDate() != null) s.setLastOrderDate(details.getLastOrderDate());
            return statsRepository.save(s);
        });
    }
}
```

Modify `src/main/java/com/apexflow/controller/CustomerStatsController.java`:
```java
package com.apexflow.controller;

import com.apexflow.model.CustomerStats;
import com.apexflow.service.CustomerStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customerStats")
public class CustomerStatsController {

    @Autowired
    private CustomerStatsService statsService;

    @GetMapping
    public ResponseEntity<List<CustomerStats>> getAllCustomerStats(@RequestParam Map<String, String> allParams) {
        Page<CustomerStats> page = statsService.getStats(allParams);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Total-Count", String.valueOf(page.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerStats> getCustomerStatsById(@PathVariable String id) {
        return statsService.getStatsById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CustomerStats> createStats(@RequestBody CustomerStats stats) {
        return ResponseEntity.ok(statsService.saveStats(stats));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CustomerStats> updateStats(@PathVariable String id, @RequestBody CustomerStats stats) {
        return statsService.updateStats(id, stats)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=CustomerStatsControllerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/apexflow/service/CustomerStatsService.java src/main/java/com/apexflow/controller/CustomerStatsController.java src/test/java/com/apexflow/controller/CustomerStatsControllerTest.java
git commit -m "feat(stats): add CustomerStatsService with filtering and pagination"
```

---

### Task 9: Subscription Scheduler (`@Scheduled`)

**Files:**
- Create: `src/main/java/com/apexflow/scheduler/SubscriptionScheduler.java`
- Modify: `src/main/java/com/apexflow/ApexFlowApplication.java`
- Create: `src/test/java/com/apexflow/scheduler/SubscriptionSchedulerTest.java`

- [ ] **Step 1: Write test for SubscriptionScheduler**

Create `src/test/java/com/apexflow/scheduler/SubscriptionSchedulerTest.java`:
```java
package com.apexflow.scheduler;

import com.apexflow.model.Customer;
import com.apexflow.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class SubscriptionSchedulerTest {

    @Autowired
    private SubscriptionScheduler scheduler;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void testUpdateExpiredSubscriptions() {
        Customer c = new Customer();
        c.setId("EXPIRED-CUST-1");
        c.setName("Past Due Customer");
        c.setStatus("Attivo");
        c.setSubscriptionEndDate(LocalDate.now().minusDays(2));
        customerRepository.save(c);

        scheduler.updateExpiredSubscriptions();

        Customer updated = customerRepository.findById("EXPIRED-CUST-1").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("Scaduto");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=SubscriptionSchedulerTest`
Expected: FAIL (`SubscriptionScheduler` does not exist)

- [ ] **Step 3: Implement `SubscriptionScheduler.java` and add `@EnableScheduling` to `ApexFlowApplication.java`**

Create `src/main/java/com/apexflow/scheduler/SubscriptionScheduler.java`:
```java
package com.apexflow.scheduler;

import com.apexflow.model.Customer;
import com.apexflow.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class SubscriptionScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionScheduler.class);

    @Autowired
    private CustomerRepository customerRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Running subscription expiration check on application startup...");
        updateExpiredSubscriptions();
    }

    @Scheduled(cron = "0 0 1 * * ?") // Runs daily at 01:00 AM
    @Transactional
    public void updateExpiredSubscriptions() {
        LocalDate today = LocalDate.now();
        List<Customer> activeCustomers = customerRepository.findAll((root, query, cb) ->
                cb.and(
                        cb.equal(root.get("status"), "Attivo"),
                        cb.isNotNull(root.get("subscriptionEndDate")),
                        cb.lessThan(root.get("subscriptionEndDate"), today)
                )
        );

        if (!activeCustomers.isEmpty()) {
            log.info("Found {} customer(s) with expired subscriptions. Updating status to 'Scaduto'...", activeCustomers.size());
            for (Customer customer : activeCustomers) {
                customer.setStatus("Scaduto");
                customerRepository.save(customer);
            }
            log.info("Expired subscriptions update complete.");
        } else {
            log.info("No expired subscriptions found.");
        }
    }
}
```

Modify `src/main/java/com/apexflow/ApexFlowApplication.java`:
```java
package com.apexflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApexFlowApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApexFlowApplication.class, args);
	}

}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=SubscriptionSchedulerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/apexflow/scheduler/SubscriptionScheduler.java src/main/java/com/apexflow/ApexFlowApplication.java src/test/java/com/apexflow/scheduler/SubscriptionSchedulerTest.java
git commit -m "feat(scheduler): implement daily and startup subscription expiration job"
```

---

### Task 10: Automatic Seeding from `db.json`

**Files:**
- Create: `src/main/resources/data/seed-db.json` (mirrored from `dashboard-tesi/db.json`)
- Modify: `src/main/java/com/apexflow/config/DataInitializer.java`
- Create: `src/test/java/com/apexflow/config/DataInitializerTest.java`

- [ ] **Step 1: Copy `db.json` into backend resources**

Copy `/Users/emanueledelmonte/Documents/Università/Tesi/dashboard-tesi/db.json` to `/Users/emanueledelmonte/Documents/Università/Tesi/backend-tesi/src/main/resources/data/seed-db.json`.

- [ ] **Step 2: Update `DataInitializer.java` to load `seed-db.json` if tables are empty**

Modify `src/main/java/com/apexflow/config/DataInitializer.java`:
Use Jackson `ObjectMapper` to parse `seed-db.json`, inserting `users`, `products`, `customers`, `orders` (with `items` set to each `order`), and `customerStats`.

- [ ] **Step 3: Write test to verify database gets populated from `seed-db.json`**

Create `src/test/java/com/apexflow/config/DataInitializerTest.java`:
Verify that `customerRepository.count() >= 8`, `productRepository.count() >= 8`, `orderRepository.count() >= 7`, and `userRepository.count() >= 2`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=DataInitializerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/data/seed-db.json src/main/java/com/apexflow/config/DataInitializer.java src/test/java/com/apexflow/config/DataInitializerTest.java
git commit -m "feat(data): seed complete dataset from seed-db.json on startup"
```

---

### Task 11: Global Exception Handling & Documentation Update

**Files:**
- Create: `src/main/java/com/apexflow/exception/GlobalExceptionHandler.java`
- Modify: `README.md`

- [ ] **Step 1: Create GlobalExceptionHandler**

Create `src/main/java/com/apexflow/exception/GlobalExceptionHandler.java` to return clean JSON errors for 400, 404, 500.

- [ ] **Step 2: Run full test suite**

Run: `./mvnw clean test`
Expected: ALL TESTS PASS

- [ ] **Step 3: Update `README.md` with complete API docs and run instructions**

Document all endpoints, query parameters (`_page`, `_limit`, `_sort`, `_order`, `_like`), CORS exposure, and how to run Angular with `npm run start:springboot`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/apexflow/exception/GlobalExceptionHandler.java README.md
git commit -m "docs: update README with API docs and add GlobalExceptionHandler"
```
