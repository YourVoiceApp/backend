package com.love.yourvoiceback.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ACCESS_TOKEN_SCHEME = "accessToken";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(
                                ACCESS_TOKEN_SCHEME,
                                new SecurityScheme()
                                        .name(ACCESS_TOKEN_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ));
    }

    @Bean
    public OpenApiCustomizer securedApiCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().forEach((path, pathItem) -> {
                if (isPublicPath(path) || pathItem == null) {
                    return;
                }

                applySecurity(pathItem.getGet());
                applySecurity(pathItem.getPost());
                applySecurity(pathItem.getPut());
                applySecurity(pathItem.getPatch());
                applySecurity(pathItem.getDelete());
                applySecurity(pathItem.getHead());
                applySecurity(pathItem.getOptions());
                applySecurity(pathItem.getTrace());
            });
        };
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/auth");
    }

    private void applySecurity(Operation operation) {
        if (operation == null) {
            return;
        }

        if (operation.getSecurity() == null || operation.getSecurity().isEmpty()) {
            operation.addSecurityItem(new SecurityRequirement().addList(ACCESS_TOKEN_SCHEME));
        }
    }
}
