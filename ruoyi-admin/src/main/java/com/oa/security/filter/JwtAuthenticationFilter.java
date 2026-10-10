package com.oa.security.filter;

import com.oa.common.constant.Constants;
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

    /** 自助改密接口：SysProfileController#updatePwd */
    private static final String PATH_UPDATE_PWD = "/system/user/profile/updatePwd";

    /** 前端每次导航都会调用的用户信息接口：SysLoginController#getInfo */
    private static final String PATH_USER_INFO = "/getInfo";

    /** 退出登录 */
    private static final String PATH_LOGOUT = "/logout";

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
                    // 首次登录强制改密必须在服务端拦截，只靠前端弹窗等于没有约束：
                    // 临时口令签发的 Token 在改密前只能访问改密、取用户信息和退出登录这三个接口
                    if (Constants.MUST_CHANGE_PWD_YES.equals(loginUser.getMustChangePwd())
                            && !isAllowedBeforePasswordChange(request.getServletPath())) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"code\":403,\"msg\":\"首次登录必须修改密码后才能继续使用\"}");
                        return;
                    }
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 改密前放行的路径。
     *
     * <p>用 {@code getServletPath()} 而不是 {@code getRequestURI()}：后者含上下文路径，
     * 部署在非根上下文时会把放行判断全部落空，把首次登录的用户彻底锁在门外。
     * 精确匹配之外再按前缀匹配，兼容带子路径的调用。</p>
     */
    private boolean isAllowedBeforePasswordChange(String servletPath) {
        if (servletPath == null) {
            return false;
        }
        return matchesPath(servletPath, PATH_UPDATE_PWD)
                || matchesPath(servletPath, PATH_USER_INFO)
                || matchesPath(servletPath, PATH_LOGOUT);
    }

    private boolean matchesPath(String servletPath, String allowedPath) {
        return allowedPath.equals(servletPath) || servletPath.startsWith(allowedPath + "/");
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}
