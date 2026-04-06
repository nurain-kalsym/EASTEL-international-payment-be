package com.kalsym.internationalPayment.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
/**
 *
 * @author 7cu
 */
@Configuration
@ConditionalOnExpression(value = "${useSwagger:false}")
public class SwaggerConfig {

        public static final String AUTHORIZATION_HEADER = "Authorization";

        @Value("${server.servlet.context-path}")
        private String contextPath;

        @Bean
        public OpenAPI productServiceApi() {

                return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("Bearer",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .name(AUTHORIZATION_HEADER)
                        ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .info(new Info()
                        .title("eastel-international-payment-service")
                        .version("1.0.0")
                        .description("This service is for Eastel's International Payment service.")
                );

        }

        // @Bean
        // public UiConfiguration uiConfig() {
        //         return UiConfigurationBuilder
        //                         .builder()
        //                         .operationsSorter(OperationsSorter.METHOD)
        //                         .build();
        // }

        // private ApiInfo apiInfo() {
        //         return new ApiInfoBuilder()
        //                         .title("ekedai-java-service")
        //                         .description("Used to manage users, roles, authorities for clients.")
        //                         .termsOfServiceUrl("not added yet")
        //                         .license("not added yet")
        //                         .licenseUrl("").version("2.5.2").build();
        // }

        // public static final String DEFAULT_INCLUDE_PATTERN = "/.*";

        // public SecurityContext securityContext() {
        //         return SecurityContext.builder()
        //                         .securityReferences(defaultAuth())
        //                         .forPaths(PathSelectors.regex(DEFAULT_INCLUDE_PATTERN))
        //                         .build();
        // }

        // List<SecurityReference> defaultAuth() {
        //         AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        //         AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        //         authorizationScopes[0] = authorizationScope;
        //         return Lists.newArrayList(
        //                         // new SecurityReference(HttpHeaders.COOKIE, authorizationScopes)
        //                         // new SecurityReference(HttpHeaders.COOKIE, authorizationScopes)
        //                         new SecurityReference("Bearer", authorizationScopes));
        // }
}
