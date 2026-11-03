package com.guilherme.entrevistaia.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Habilita o botão "Authorize" no Swagger UI: cole o token JWT (sem o
// prefixo "Bearer ", o springdoc já adiciona) e todas as chamadas feitas
// pela UI passam a incluir o header Authorization automaticamente.
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    // A versão vem do pom (via build-info do spring-boot-maven-plugin). Sem o
    // build-info, como ao rodar pela IDE sem passar pelo Maven, fica "dev".
    @Bean
    public OpenAPI customOpenAPI(ObjectProvider<BuildProperties> buildProperties) {
        BuildProperties build = buildProperties.getIfAvailable();
        String versao = build != null ? build.getVersion() : "dev";
        return new OpenAPI()
            .info(new Info().title("Entrevista IA API").version(versao))
            .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
            .components(new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                    .name(SECURITY_SCHEME_NAME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
