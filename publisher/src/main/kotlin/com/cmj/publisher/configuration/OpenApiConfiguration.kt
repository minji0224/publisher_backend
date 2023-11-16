package com.cmj.publisher.configuration

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import io.swagger.v3.oas.models.security.SecurityScheme


@Configuration
class OpenApiConfiguration {

    @Bean
    fun openAPI(): OpenAPI? {
        return OpenAPI()
            .info(
                Info()
                    .title("출판사서비스 application API")
                    .version("v1.0")
                    .description("Publisher application API")
            )
            .components(
                Components().addSecuritySchemes(
                    "bearer-key",
                    SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                )
            )
    }
}