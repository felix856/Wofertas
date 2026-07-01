package com.example.demo.config;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
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

    private static final List<String> DEFAULT_ALLOWED_ORIGIN_PATTERNS = List.of(
            "https://wofertas.vercel.app",
            "https://backend-s8by1200-felix856s-projects.vercel.app",
            "https://*.vercel.app",
            "https://wofertas-production.up.railway.app"
    );

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(headers -> headers
                .contentTypeOptions(contentType -> {})
                .frameOptions(frame -> frame.sameOrigin())
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                // preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // autenticação
                .requestMatchers("/auth/**", "/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()

                // páginas HTML públicas
                .requestMatchers(
                    "/",
                    "/error",
                    "/index.html",
                    "/login.html",
                    "/mercado-cadastro.html",
                    "/reset-senha.html",
                    "/mercadoHome.html",
                    "/criar-oferta.html",
                    "/dashboard-analytics.html",
                    "/dashboard-pro.html",
                    "/historico.html",
                    "/perfil_mercado.html",
                    "/privacy-policy",
                    "/privacy-policy.html",
                    "/termos",
                    "/termos.html",
                    "/excluir-conta",
                    "/excluir-conta.html"
                ).permitAll()

                // arquivos estáticos
                .requestMatchers(
                    "/*.css",
                    "/*.js",
                    "/*.ico",
                    "/*.png",
                    "/*.jpg",
                    "/*.svg",
                    "/js/**",
                    "/imagens/**",
                    "/assets/**",
                    "/uploads/**"
                ).permitAll()

                // endpoints públicos POST
                .requestMatchers(
                    HttpMethod.POST,
                    "/usuarios",
                    "/api/usuarios",
                    "/mercados",
                    "/api/mercados",
                    "/api/mercado/cadastro",
                    "/mercado/cadastro",
                    "/privacy/public/deletion-request",
                    "/api/privacy/public/deletion-request"
                ).permitAll()

                // endpoints públicos GET
                .requestMatchers(
                    HttpMethod.GET,
                    "/ofertas/**",
                    "/mercados/**",
                    "/encartes",
                    "/encartes/**",
                    "/api/ofertas/**",
                    "/api/mercados/**",
                    "/api/encartes",
                    "/api/encartes/**",
                    "/privacy/legal",
                    "/api/privacy/legal"
                ).permitAll()

                // JWT obrigatório para o resto
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(parseAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private List<String> parseAllowedOrigins() {
        List<String> configuredOrigins = allowedOrigins == null || allowedOrigins.isBlank()
                ? List.of()
                : List.of(allowedOrigins.split(","))
                .stream()
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();

        if (configuredOrigins.contains("*")) {
            return configuredOrigins;
        }

        return Stream.concat(configuredOrigins.stream(), DEFAULT_ALLOWED_ORIGIN_PATTERNS.stream())
                .distinct()
                .toList();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
