package com.unimobili.agenda.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API de Agenda Corporativa",
                version = "0.0.1",
                description = """
                        API REST para gerenciamento de agendas corporativas (Unimobili).
                        Autenticação via JWT: faça login em POST /auth/login, copie o accessToken
                        e use o botão 'Authorize' (Bearer) para acessar os endpoints protegidos.
                        Papéis: GERENTE (acesso total), INTERNO (cria/gere eventos), EXTERNO (vê a própria agenda).
                        """),
        servers = @Server(url = "/", description = "Servidor local"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
}
