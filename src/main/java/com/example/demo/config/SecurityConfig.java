package com.example.demo.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.demo.security.JwtAuthFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {

        http

        .csrf(csrf -> csrf.disable())

        .cors(cors ->
                cors.configurationSource(
                        corsConfigurationSource()
                )
        )

        .sessionManagement(session ->
                session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS
                )
        )

        .authorizeHttpRequests(auth -> auth

                // preflight
                .requestMatchers(
                        HttpMethod.OPTIONS,
                        "/**"
                ).permitAll()

                // autenticação
                .requestMatchers(
                        "/auth/**",
                        "/api/auth/**"
                ).permitAll()

                // páginas públicas
                .requestMatchers(
                        "/",
                        "/error",
                        "/*.html"
                ).permitAll()

                // arquivos estáticos
                .requestMatchers(
                        "/css/**",
                        "/js/**",
                        "/assets/**",
                        "/imagens/**",
                        "/uploads/**",
                        "/*.css",
                        "/*.js",
                        "/*.png",
                        "/*.jpg",
                        "/*.ico",
                        "/*.svg"
                ).permitAll()

                // endpoints públicos
                .requestMatchers(
                        HttpMethod.POST,
                        "/usuarios",
                        "/api/usuarios",
                        "/mercados",
                        "/api/mercados",
                        "/api/mercado/cadastro",
                        "/mercado/cadastro"
                ).permitAll()

                .requestMatchers(
                        HttpMethod.GET,
                        "/ofertas/**",
                        "/mercados/**",
                        "/encartes/**",
                        "/api/ofertas/**",
                        "/api/mercados/**",
                        "/api/encartes/**"
                ).permitAll()

                // JWT obrigatório
                .anyRequest()
                .authenticated()
        )

        .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config =
                new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        config.setAllowedHeaders(
                List.of("*")
        );

        config.setExposedHeaders(
                List.of("Authorization")
        );

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                config
        );

        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }
}
