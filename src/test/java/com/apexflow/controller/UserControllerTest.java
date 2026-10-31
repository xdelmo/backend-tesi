package com.apexflow.controller;

import com.apexflow.model.User;
import com.apexflow.repository.UserRepository;
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
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private Long testUserId;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        User u = new User();
        u.setEmail("admin@demo.com");
        u.setPassword("password");
        u.setName("Admin User");
        u.setRole("admin");
        User saved = userRepository.save(u);
        testUserId = saved.getId();
    }

    @Test
    void testAuthLoginCompatibilityQuery() throws Exception {
        mockMvc.perform(get("/api/users?email=admin@demo.com&password=password"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$[0].email").value("admin@demo.com"))
                .andExpect(jsonPath("$[0].name").value("Admin User"));
    }

    @Test
    void testCreateUserReturns201() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"new@user.com\",\"password\":\"pwd\",\"name\":\"New User\",\"role\":\"user\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value("new@user.com"))
                .andExpect(jsonPath("$.name").value("New User"));
    }

    @Test
    void testGetUserById_Success() throws Exception {
        mockMvc.perform(get("/api/users/" + testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserId))
                .andExpect(jsonPath("$.email").value("admin@demo.com"))
                .andExpect(jsonPath("$.name").value("Admin User"));
    }

    @Test
    void testGetUserById_NotFound() throws Exception {
        mockMvc.perform(get("/api/users/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateUser_Success() throws Exception {
        mockMvc.perform(patch("/api/users/" + testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Updated Admin\",\"role\":\"superadmin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Admin"))
                .andExpect(jsonPath("$.role").value("superadmin"))
                .andExpect(jsonPath("$.email").value("admin@demo.com"));
    }

    @Test
    void testUpdateUser_NotFound() throws Exception {
        mockMvc.perform(patch("/api/users/999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ghost\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteUser_SuccessAndNotFound() throws Exception {
        mockMvc.perform(delete("/api/users/" + testUserId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/users/" + testUserId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetUsersPaginationAndFilter() throws Exception {
        User u2 = new User();
        u2.setEmail("user2@demo.com");
        u2.setPassword("secret");
        u2.setName("Standard User");
        u2.setRole("user");
        userRepository.save(u2);

        // Filter by role
        mockMvc.perform(get("/api/users?role=admin"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$[0].email").value("admin@demo.com"));

        // Pagination
        mockMvc.perform(get("/api/users?_page=1&_limit=10"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$.length()").value(2));
    }
}
