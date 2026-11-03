package com.smartambulance.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartAmbulanceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI-Driven Smart Ambulance Navigation API")
                        .description("Academic project prototype: Real-Time Traffic-Aware Dynamic Route Optimization & ESP32 Signal Priority System. M. Kumarasamy College of Engineering, Karur.")
                        .version("1.0.0")
                        .contact(new Contact().name("AI & Data Science Department").email("smartambulance@mkce.ac.in")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components().addSecuritySchemes("BearerAuth",
                        new SecurityScheme()
                                .name("BearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
