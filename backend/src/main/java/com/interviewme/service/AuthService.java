package com.interviewme.service;

import com.interviewme.common.dto.AuthResponse;
import com.interviewme.common.dto.LoginRequest;
import com.interviewme.common.dto.RegisterRequest;
import com.interviewme.common.dto.UserInfoResponse;
import com.interviewme.model.Tenant;
import com.interviewme.model.User;
import com.interviewme.repository.TenantRepository;
import com.interviewme.repository.UserRepository;
import com.interviewme.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Slf4j
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       TenantRepository tenantRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Lazy AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.email());

        // Check if user already exists
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create new tenant
        Tenant tenant = new Tenant();
        tenant.setName(request.tenantName());
        tenant.setCreatedAt(OffsetDateTime.now());
        tenant = tenantRepository.save(tenant);
        log.info("Created tenant: {} with ID: {}", tenant.getName(), tenant.getId());

        // Create new user
        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setTenantId(tenant.getId());
        user.setCreatedAt(OffsetDateTime.now());
        user = userRepository.save(user);
        log.info("Created user: {} with ID: {}", user.getEmail(), user.getId());

        // Generate JWT token
        String token = jwtService.generateToken(user.getEmail(), user.getTenantId());

        return new AuthResponse(token, user.getEmail(), user.getTenantId());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.email());

        // Authenticate
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        // Get user details
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Generate JWT token
        String token = jwtService.generateToken(user.getEmail(), user.getTenantId());

        log.info("User logged in successfully: {}", user.getEmail());

        return new AuthResponse(token, user.getEmail(), user.getTenantId());
    }

    @Transactional(readOnly = true)
    public UserInfoResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new UserInfoResponse(
                user.getId(),
                user.getEmail(),
                user.getTenantId(),
                user.getCreatedAt().toString()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }
}
