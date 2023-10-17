package com.cmj.publisher.configuration

import com.cmj.publisher.auth.AuthInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableWebMvc
class WebMvcConfig(val authInterceptor: AuthInterceptor) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**") // 모든 경로에 대해
            .allowedOrigins(
            "http://localhost:5500",
            "http://127.0.0.1:5500",
            "http://localhost:5000"
        ).allowedMethods("*") // 모든메서드허용(겟, 포스트, 델리트 등)
    }
    // 인증처리용 인터셉터 추가
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authInterceptor)
    }
}