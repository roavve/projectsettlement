package com.example.mssqll.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static com.example.mssqll.utiles.SecurityUtils.getCurrentUsername;

@Slf4j
@Configuration
public class PasswordConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("Configuring BCryptPasswordEncoder for password encoding (by {})", getCurrentUsername());
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        log.info("BCryptPasswordEncoder configured successfully (by {})", getCurrentUsername());
        return encoder;
    }
}