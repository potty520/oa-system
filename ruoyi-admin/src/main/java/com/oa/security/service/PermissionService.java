package com.oa.security.service;

import com.oa.security.model.LoginUser;
import com.oa.security.utils.SecurityUtils;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component("ss")
public class PermissionService {

    public boolean hasPermi(String permission) {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user == null || user.getPermissions() == null) {
            return false;
        }
        if (SecurityUtils.isAdmin(user.getUserId())) {
            return true;
        }
        Set<String> permissions = user.getPermissions();
        return permissions.contains("*:*:*") || permissions.contains(permission);
    }

    public boolean hasAnyPermi(String permissions) {
        if (permissions == null || permissions.trim().isEmpty()) {
            return false;
        }
        for (String permission : permissions.split(",")) {
            if (hasPermi(permission.trim())) {
                return true;
            }
        }
        return false;
    }
}