package com.smartambulance.service;

import com.smartambulance.config.JwtUtil;
import com.smartambulance.domain.User;
import com.smartambulance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public Map<String, Object> login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        User user = userOpt.get();
        // Support {noop} prefixes in dev seeds or standard BCrypt
        boolean matches = false;
        if (user.getPasswordHash().startsWith("{noop}")) {
            matches = user.getPasswordHash().substring(6).equals(password);
        } else {
            matches = passwordEncoder.matches(password, user.getPasswordHash());
        }

        if (!matches) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

        Map<String, Object> res = new HashMap<>();
        res.put("token", token);
        res.put("username", user.getUsername());
        res.put("role", user.getRole().name());
        res.put("full_name", user.getFullName());
        return res;
    }

    public Map<String, Object> getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        Map<String, Object> res = new HashMap<>();
        res.put("id", user.getId());
        res.put("username", user.getUsername());
        res.put("role", user.getRole().name());
        res.put("full_name", user.getFullName());
        return res;
    }
}
