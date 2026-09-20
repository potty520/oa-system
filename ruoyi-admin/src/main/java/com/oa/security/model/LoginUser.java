package com.oa.security.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class LoginUser implements UserDetails {
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 用户密码 */
    private String password;

    /** 用户真实姓名 */
    private String nickname;

    /** 用户头像 */
    private String avatar;

    /** 角色权限集合 */
    private Set<String> permissions;

    /** 部门ID */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    /** 账号状态（0正常 1停用） */
    private String status;

    public LoginUser() {}

    public LoginUser(Long userId, String username, String password, String nickname,
                     String avatar, Long deptId, String deptName, String status) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.avatar = avatar;
        this.deptId = deptId;
        this.deptName = deptName;
        this.status = status;
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (permissions == null) {
            return java.util.Collections.emptyList();
        }
        return permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return "0".equals(this.status);
    }
}
