package com.codewalnut.resolvehub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.codewalnut.resolvehub.exception.ApiErrorFactory;
import com.codewalnut.resolvehub.security.SecurityErrorHandler;
import com.codewalnut.resolvehub.security.AuthenticatedUsernameFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.savedrequest.NullRequestCache;
import com.codewalnut.resolvehub.security.LengthCheckedPasswordEncoder;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper, ApiErrorFactory errorFactory) throws Exception {
        var errors = new SecurityErrorHandler(mapper, errorFactory);
        var csrfFilter = new CsrfFilter(new CookieCsrfTokenRepository());
        csrfFilter.setAccessDeniedHandler((request, response, exception) -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                errors.commence(request, response, new InsufficientAuthenticationException("Authentication required"));
            } else {
                errors.handle(request, response, exception);
            }
        });
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/info", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint(errors))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .addFilterAfter(new AuthenticatedUsernameFilter(), BasicAuthenticationFilter.class)
                .addFilterAfter(csrfFilter, AuthenticatedUsernameFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new LengthCheckedPasswordEncoder(new BCryptPasswordEncoder());
    }
}
