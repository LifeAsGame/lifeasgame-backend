package online.lifeasgame.system.bootstrap.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.platform.security.jwt.JwtAuthenticationFilter;
import online.lifeasgame.platform.security.jwt.JwtProvider;
import online.lifeasgame.user.application.internal.UserAuthApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final UserAuthApi userAuthApi;
    private final WebCorsProperties webCorsProperties;

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, cause) ->
                                response.sendError(
                                        HttpServletResponse.SC_UNAUTHORIZED
                                )
                        ))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/users/register").permitAll()
                        .requestMatchers(
                                "/api/v1/users/availability/**",
                                "/actuator/health").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/sse/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider, userAuthApi),
                        UsernamePasswordAuthenticationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(webCorsProperties.allowedOrigins());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Idempotency-Key",
                "X-Correlation-Id"
        ));
        cfg.setAllowCredentials(true);
        cfg.validateAllowCredentials();
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
