package com.college.placement.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.college.placement.entity.AppUser;
import com.college.placement.enums.Role;
import com.college.placement.repository.UserRepository;

@Configuration
@Profile("dev")
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner seedUsers(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            seed(users, encoder, "Admin User", "admin@example.com", "AdminPass123", Role.ADMIN);
            seed(users, encoder, "Placement Officer", "placement@example.com", "PlacementPass123",
                    Role.PLACEMENT_OFFICER);
            seed(users, encoder, "Recruiter User", "recruiter@example.com", "RecruiterPass123", Role.RECRUITER);
        };
    }

    private void seed(UserRepository users, PasswordEncoder encoder, String name, String email, String password,
            Role role) {
        if (!users.existsByEmail(email)) {
            users.save(new AppUser(name, email, encoder.encode(password), role));
            log.info("{} account created: {}", role, email);
        }
    }
}
