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

        Path uploadDir = Paths.get("uploads");
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        // Pasta de uploads (imagens)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");

        Path viewDir = Paths.get("view");
        String viewPath = viewDir.toFile().getAbsolutePath();

        // Arquivos CSS e JS na raiz do view
        registry.addResourceHandler("/*.css", "/*.js", "/*.ico", "/*.png", "/*.jpg", "/*.svg")
                .addResourceLocations("file:" + viewPath + "/")
                .setCachePeriod(0);

        // Subpastas do view (js/, imagens/, etc)
        registry.addResourceHandler("/js/**", "/imagens/**", "/assets/**")
                .addResourceLocations("file:" + viewPath + "/")
                .setCachePeriod(0);

        // Páginas HTML
        registry.addResourceHandler("/*.html")
                .addResourceLocations("file:" + viewPath + "/")
                .setCachePeriod(0);

        // Raiz → index.html
        registry.addResourceHandler("/")
                .addResourceLocations("file:" + viewPath + "/")
                .setCachePeriod(0);
    }
}
