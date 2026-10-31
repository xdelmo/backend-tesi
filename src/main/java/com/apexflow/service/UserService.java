package com.apexflow.service;

import com.apexflow.model.User;
import com.apexflow.repository.UserRepository;
import com.apexflow.util.QueryFilterParser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Page<User> getUsers(Map<String, String> params) {
        Pageable pageable = QueryFilterParser.getPageable(params);
        Specification<User> spec = QueryFilterParser.parse(params);
        return userRepository.findAll(spec, pageable);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User createUser(User user) {
        return userRepository.save(user);
    }

    @Transactional
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

    @Transactional
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
