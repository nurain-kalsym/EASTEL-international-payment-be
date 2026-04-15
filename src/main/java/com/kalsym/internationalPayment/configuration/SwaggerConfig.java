package com.kalsym.internationalPayment.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(info = @Info(title = "International Payment API", version = "1.0.0", description = "API documentation for Eastel International Payment Service"), servers = {
        @Server(description = "International Payment URL", url = "${protocol.subdomain}${server.servlet.context-path}"), }, security = @SecurityRequirement(name = "bearerAuth"))

@SecurityScheme(name = "bearerAuth", description = "JWT auth description", scheme = "bearer", type = SecuritySchemeType.HTTP, bearerFormat = "JWT", in = SecuritySchemeIn.HEADER)
@Configuration
@ConditionalOnExpression("${useSwagger:true}")
public class SwaggerConfig {
}
