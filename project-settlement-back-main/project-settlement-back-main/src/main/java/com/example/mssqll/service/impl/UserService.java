package com.example.mssqll.service.impl;

import com.example.mssqll.dto.response.UserSearchDto;
import com.example.mssqll.models.User;
import com.example.mssqll.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserDetailsService userDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) {
                log.debug("Loading user by username: {} (by {})", username, getCurrentUsername());

                return userRepository.findByEmail(username)
                        .map(user -> {
                            log.debug("User found - ID: {}, Email: {} (by {})", user.getId(), user.getEmail(), getCurrentUsername());
                            return user;
                        })
                        .orElseThrow(() -> {
                            log.warn("User not found with username: {} (by {})", username, getCurrentUsername());
                            return new UsernameNotFoundException("User not found");
                        });
            }
        };
    }

    public User save(User newUser) {
        log.info("Attempting to save user with email: {} (by {})", newUser.getEmail(), getCurrentUsername());
        log.debug("User details - FirstName: {}, LastName: {}, Role: {} (by {})",
                  newUser.getFirstName(), newUser.getLastName(), newUser.getRole(), getCurrentUsername());

        if (userRepository.findByEmail(newUser.getEmail()).isPresent()) {
            log.warn("Save failed - user already exists with email: {} (by {})", newUser.getEmail(), getCurrentUsername());
            throw new RuntimeException("User such id or email already exist");
        }

        if (newUser.getId() == null) {
            log.debug("Setting creation timestamp for new user (by {})", getCurrentUsername());
            newUser.setCreatedAt(LocalDateTime.now());
        } else {
            log.debug("Updating existing user with ID: {} (by {})", newUser.getId(), getCurrentUsername());
        }

        newUser.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(newUser);
        log.info("User saved successfully with ID: {}, Email: {} (by {})", savedUser.getId(), savedUser.getEmail(), getCurrentUsername());

        return savedUser;
    }

}