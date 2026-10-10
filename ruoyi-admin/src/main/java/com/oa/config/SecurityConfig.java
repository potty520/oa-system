package com.oa.config;

import com.oa.security.filter.JwtAuthenticationFilter;
import com.oa.security.service.JwtTokenBlacklist;
import com.oa.security.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private JwtTokenBlacklist jwtTokenBlacklist;

    @Value("${app.cors.allowed-origins:http://localhost:18081}")
    private String allowedOrigins;

    @Value("${app.security.public-api-docs:false}")
    private boolean publicApiDocs;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors().and()
            .csrf().disable()
            .logout()
            .logoutUrl("/logout")
            .deleteCookies("OA-Token")
            .logoutSuccessHandler((request, response, authentication) -> {
                String token = resolveToken(request);
                if (token != null && jwtUtils.validateToken(token)) {
                    jwtTokenBlacklist.revoke(token, jwtUtils.parseToken(token).getExpiration().getTime());
                }
                response.setStatus(200);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":200,\"msg\":\"退出成功\"}");
            })
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            .antMatchers("/login", "/captcha/**").permitAll()
            .antMatchers(publicApiDocPatterns()).permitAll()
            .antMatchers("/oa/announcement/published/**").permitAll()
            .anyRequest().authenticated()
            .and()
            // 区分“未认证”与“已认证但无权限”：
            // 不加 AuthenticationEntryPoint 时 Spring Security 默认用 Http403ForbiddenEntryPoint，
            // 未登录/令牌过期也会返回 403，前端只能把它当成“没有操作权限”，无法触发重新登录。
            .exceptionHandling()
            .authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"msg\":\"登录状态已失效，请重新登录\"}");
            })
            .accessDeniedHandler((request, response, accessDeniedException) -> {
                response.setStatus(javax.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"msg\":\"没有操作权限\"}");
            })
            .and()
            .headers().frameOptions().sameOrigin()
            .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split("\\s*,\\s*")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private String resolveToken(javax.servlet.http.HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String[] publicApiDocPatterns() {
        if (publicApiDocs) {
            return new String[]{"/swagger-resources/**", "/swagger-ui/**", "/v3/api-docs/**"};
        }
        return new String[]{"/__api-docs-disabled__"};
    }
}
