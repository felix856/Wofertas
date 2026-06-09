package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@Configuration
public class DemoApplication implements WebMvcConfigurer {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	/**
	 * Configuração para servir arquivos estáticos (HTML, CSS, JS, imagens)
	 * Mapeia a pasta ./view para ser servida como conteúdo estático
	 */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// Servir arquivos da pasta ./view (onde estão os HTMLs, CSS, JS, etc)
		registry.addResourceHandler("/**")
				.addResourceLocations("file:./view/", "classpath:/static/", "classpath:/public/")
				.setCachePeriod(3600); // Cache de 1 hora
	}
}
