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
	 * 
	 * Padrões específicos para evitar conflito com controllers da API
	 */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// Servir arquivos HTML
		registry.addResourceHandler("/*.html")
				.addResourceLocations("file:./view/")
				.setCachePeriod(0); // Sem cache para HTMLs
		
		// Servir CSS
		registry.addResourceHandler("*.css")
				.addResourceLocations("file:./view/")
				.setCachePeriod(3600);
		
		// Servir JavaScript
		registry.addResourceHandler("*.js")
				.addResourceLocations("file:./view/")
				.setCachePeriod(3600);
		
		// Servir imagens
		registry.addResourceHandler("/imagens/**")
				.addResourceLocations("file:./view/imagens/")
				.setCachePeriod(86400); // Cache de 1 dia para imagens
		
		// Servir outras pastas estáticas (se existirem)
		registry.addResourceHandler("/js/**")
				.addResourceLocations("file:./view/js/")
				.setCachePeriod(3600);
		
		// Fallback para qualquer outro arquivo que não seja API
		registry.addResourceHandler("/**")
				.addResourceLocations("file:./view/", "classpath:/static/", "classpath:/public/")
				.setCachePeriod(3600);
	}
}
