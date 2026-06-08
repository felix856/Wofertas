package com.example.demo.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Expõe a pasta de uploads para que as imagens sejam acessíveis via URL
        Path uploadDir = Paths.get("uploads");
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");

        // Expõe a pasta "view" para servir arquivos HTML, CSS e JS
        Path viewDir = Paths.get("view");
        String viewPath = viewDir.toFile().getAbsolutePath();

        registry.addResourceHandler("/**")
                .addResourceLocations("file:" + viewPath + "/")
                .setCachePeriod(0); // Desabilita cache para desenvolvimento
    }
}