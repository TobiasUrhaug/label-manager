package org.omt.labelmanager.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/css/**", "/js/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
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
    public AuthenticationEntryPoint authenticationEntryPoint() {
        SpaApiAuthenticationEntryPoint apiEntryPoint = new SpaApiAuthenticationEntryPoint();
        LoginUrlAuthenticationEntryPoint loginEntryPoint = new LoginUrlAuthenticationEntryPoint("/login");
        return (request, response, authException) -> {
            if (request.getRequestURI().startsWith("/api/")) {
                apiEntryPoint.commence(request, response, authException);
            } else {
                loginEntryPoint.commence(request, response, authException);
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
