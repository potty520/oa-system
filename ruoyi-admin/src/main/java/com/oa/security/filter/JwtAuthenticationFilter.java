package com.oa.security.filter;

import com.oa.security.model.LoginUser;
import com.oa.security.service.JwtTokenBlacklist;
import com.oa.security.service.LoginUserCache;
import com.oa.security.utils.JwtUtils;
import com.oa.system.service.ISysUserService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private LoginUserCache loginUserCache;

    @Autowired
    private JwtTokenBlacklist jwtTokenBlacklist;

    private static final String TOKEN_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && !jwtTokenBlacklist.isRevoked(token) && jwtUtils.validateToken(token)) {
            Claims claims = jwtUtils.parseToken(token);
            String username = claims.getSubject();
            // 改密/重置密码/停用之后旧 Token 必须立刻失效，这里按签发时间再校验一次用户级吊销
            if (username != null && !jwtTokenBlacklist.isUserRevoked(username, claims.getIssuedAt())) {
                LoginUser loginUser = loginUserCache.get(username, () -> userService.selectUserByUsername(username));
                if (loginUser != null && loginUser.isEnabled()) {
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    if (loginUser.getPermissions() != null) {
                        for (String perm : loginUser.getPermissions()) {
                            authorities.add(new SimpleGrantedAuthority(perm));
                        }
                    }
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}
