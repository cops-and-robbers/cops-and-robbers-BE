package com.team.cops_and_robbers.common.config;

import com.team.cops_and_robbers.admin.presentation.interceptor.AdminInterceptor;
import com.team.cops_and_robbers.auth.presentation.interceptor.AuthInterceptor;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUserArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AdminInterceptor adminInterceptor;
    private final LoginUserArgumentResolver loginUserArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/user/check-nickname",
                        "/api/community-posts",
                        "/api/community-posts/{postId}",
                        "/actuator/health"
                );

        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/graphql");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/graphql")
                .allowedOrigins(
                        "http://localhost:3000",
                        "https://copsnro66ers.site",
                        "https://admin.copsnro66ers.site",
                        "https://dev-api.copsnro66ers.site",
                        "https://copsandrobbers.app",
                        "https://admin.copsandrobbers.app",
                        "https://dev-api.copsandrobbers.app"
                )
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);

        registry.addMapping("/api/auth/**")
                .allowedOrigins(
                        "http://localhost:3000",
                        "https://copsnro66ers.site",
                        "https://admin.copsnro66ers.site",
                        "https://dev-api.copsnro66ers.site",
                        "https://copsandrobbers.app",
                        "https://admin.copsandrobbers.app",
                        "https://dev-api.copsandrobbers.app"
                )
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);

        registry.addMapping("/api/notices/**")
                .allowedOrigins(
                        "http://localhost:3000",
                        "https://copsnro66ers.site",
                        "https://admin.copsnro66ers.site",
                        "https://dev-api.copsnro66ers.site"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);

        registry.addMapping("/api/community-posts/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserArgumentResolver);
    }
}
