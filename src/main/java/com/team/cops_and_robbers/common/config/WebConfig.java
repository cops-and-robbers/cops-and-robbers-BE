package com.team.cops_and_robbers.common.config;

import com.team.cops_and_robbers.admin.presentation.interceptor.AdminInterceptor;
import com.team.cops_and_robbers.auth.presentation.interceptor.AuthInterceptor;
import com.team.cops_and_robbers.auth.presentation.resolver.LoginUserArgumentResolver;
import com.team.cops_and_robbers.common.presentation.interceptor.QueryParameterInterceptor;
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

    private static final String[] ALLOWED_ORIGINS = {
            "http://localhost:3000",
            "https://copsnro66ers.site",
            "https://admin.copsnro66ers.site",
            "https://dev-api.copsnro66ers.site",
            "https://copsandrobbers.app",
            "https://admin.copsandrobbers.app",
            "https://dev-api.copsandrobbers.app"
    };

    private final AuthInterceptor authInterceptor;
    private final AdminInterceptor adminInterceptor;
    private final QueryParameterInterceptor queryParameterInterceptor;
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

        registry.addInterceptor(queryParameterInterceptor)
                .addPathPatterns("/api/**");

        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/graphql");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/graphql")
                .allowedOrigins(ALLOWED_ORIGINS)
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);

        registry.addMapping("/api/auth/**")
                .allowedOrigins(ALLOWED_ORIGINS)
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);

        registry.addMapping("/api/notices/**")
                .allowedOrigins(ALLOWED_ORIGINS)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);

        registry.addMapping("/api/community-posts/**")
                .allowedOrigins(ALLOWED_ORIGINS)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserArgumentResolver);
    }
}
