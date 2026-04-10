package org.omt.labelmanager.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .spa()
                        .ignoringRequestMatchers("/api/auth/register")
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/api/auth/register").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .successHandler(spaAuthSuccessHandler())
                        .failureHandler(spaAuthFailureHandler())
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessHandler(spaLogoutSuccessHandler())
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                );

        return http.build();
    }

    @Bean
    public SpaAuthSuccessHandler spaAuthSuccessHandler() {
        return new SpaAuthSuccessHandler();
    }

    @Bean
    public SpaAuthFailureHandler spaAuthFailureHandler() {
        return new SpaAuthFailureHandler();
    }

    @Bean
    public SpaLogoutSuccessHandler spaLogoutSuccessHandler() {
        return new SpaLogoutSuccessHandler();
    }

    @Bean
    public SpaApiAuthenticationEntryPoint authenticationEntryPoint() {
        return new SpaApiAuthenticationEntryPoint();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
