package com.example.mssqll.service.impl;

import com.example.mssqll.dto.response.UserResponseDto;
import com.example.mssqll.dto.response.UserSearchDto;
import com.example.mssqll.models.Role;
import com.example.mssqll.models.User;
import com.example.mssqll.repository.UserRepository;
import com.example.mssqll.service.UserService;
import com.example.mssqll.specifications.UserSpecification;
import com.example.mssqll.utiles.exceptions.AdminNotEditException;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private EntityManager entityManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<UserSearchDto> searchUsers(Map<String, Object> filters) {
        log.info("Searching users with filters: {} (by {})", filters, getCurrentUsername());
        Specification<User> spec = UserSpecification.getSpecifications(filters);
        List<User> users = userRepository.findAll(spec);
        log.info("Found {} users matching filters (by {})", users.size(), getCurrentUsername());
        return users.stream().map(this::mapToSearchDto).collect(Collectors.toList());
    }

    private UserResponseDto mapToDto(User user) {
        return UserResponseDto.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .updatedAt(user.getUpdatedAt())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .id(user.getId())
                .build();
    }

    private UserSearchDto mapToSearchDto(User user) {
        return UserSearchDto.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .id(user.getId())
                .build();
    }

    @Override
    public UserResponseDto updateUser(User user, Long id) {
        log.info("Attempting to update user with ID: {} (by {})", id, getCurrentUsername());
        log.debug("Update request - Email: {}, Role: {} (by {})", user.getEmail(), user.getRole(), getCurrentUsername());

        Optional<User> user12 = userRepository.findById(id);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User userDetails = (User) authentication.getPrincipal();
        log.debug("Update requested by user ID: {}, Role: {} (by {})", userDetails.getId(), userDetails.getRole(), getCurrentUsername());

        if (user12.isEmpty()) {
            log.warn("Update failed - user not found with ID: {} (by {})", id, getCurrentUsername());
            throw new UsernameNotFoundException("User not found");
        } else {
            User user1 = user12.get();
            log.debug("Found user to update - Current email: {}, Current role: {} (by {})", user1.getEmail(), user1.getRole(), getCurrentUsername());

            if (userDetails.getRole() == user1.getRole() && !user1.getId().equals(userDetails.getId())) {
                log.warn("Update denied - attempt to update another admin by user ID: {} (by {})", userDetails.getId(), getCurrentUsername());
                throw new AdminNotEditException("You cannot update an admin");
            }

            user1.setFirstName(user.getFirstName());
            user1.setLastName(user.getLastName());
            if (user.getPassword() != (null)) {
                log.debug("Updating password for user ID: {} (by {})", id, getCurrentUsername());
                user1.setPassword(passwordEncoder.encode(user.getPassword()));
            }
            user1.setEmail(user.getEmail());
            user1.setRole(user.getRole());
            user1.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user1);

            log.info("User updated successfully - ID: {}, Email: {} (by {})", user1.getId(), user1.getEmail(), getCurrentUsername());

            return mapToDto(user1);
        }
    }

    @Override
    public UserResponseDto deleteUser(Long id) {
        log.info("Attempting to soft-delete user with ID: {} (by {})", id, getCurrentUsername());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User userDetails = (User) authentication.getPrincipal();
        log.debug("Delete requested by user ID: {}, Role: {} (by {})", userDetails.getId(), userDetails.getRole(), getCurrentUsername());

        Optional<User> user = userRepository.findById(id);
        User user1;

        if (user.isEmpty()) {
            log.warn("Delete failed - user not found with ID: {} (by {})", id, getCurrentUsername());
            throw new UsernameNotFoundException("User not found");
        }

        user1 = user.get();
        log.debug("Found user to delete - Email: {}, Role: {} (by {})", user1.getEmail(), user1.getRole(), getCurrentUsername());

        if (user1.getRole() == Role.ROLE_ADMIN) {
            log.warn("Delete denied - attempt to delete admin user ID: {} by user ID: {} (by {})", id, userDetails.getId(), getCurrentUsername());
            throw new AdminNotEditException("You can't edit admin from Role " + user1.getRole() +
                    " to role " + Role.SOFT_DELETED);
        }

        user1.setRole(Role.SOFT_DELETED);
        user1 = userRepository.save(user1);
        log.info("User soft-deleted successfully - ID: {}, Email: {} (by {})", user1.getId(), user1.getEmail(), getCurrentUsername());

        return mapToDto(user1);
    }

}
