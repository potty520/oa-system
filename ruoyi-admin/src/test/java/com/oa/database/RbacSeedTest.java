package com.oa.database;

import com.oa.system.mapper.SysUserMapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验初始化脚本里的菜单/角色授权种子是自洽的，并且权限查询会跳过停用与已删除的角色、菜单。
 */
class RbacSeedTest {

    @Test
    void seededRoleMenusShouldBeConsistentAndGrantExpectedPermissions() {
        JdbcTemplate jdbc = newSchema("rbac_seed_test");

        Integer orphans = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_role_menu rm LEFT JOIN sys_menu m ON m.menu_id = rm.menu_id " +
                        "WHERE m.menu_id IS NULL", Integer.class);
        assertEquals(0, orphans, "sys_role_menu 存在指向不存在菜单的记录");

        Integer menuTotal = jdbc.queryForObject("SELECT COUNT(*) FROM sys_menu", Integer.class);
        Integer adminGranted = jdbc.queryForObject("SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1", Integer.class);
        assertEquals(menuTotal, adminGranted, "超级管理员角色应拥有全部菜单");
        assertTrue(menuTotal > 0, "菜单种子不应为空");

        List<String> commonPerms = permsOfRole(jdbc, 2L);
        assertTrue(commonPerms.contains("oa:leave:add"), "普通用户应能提交请假");
        assertFalse(commonPerms.contains("oa:leave:approve"), "普通用户不应拥有请假审批权限");
        assertFalse(commonPerms.contains("oa:reimbursement:approve"), "普通用户不应拥有报销审批权限");
        assertFalse(commonPerms.contains("system:role:edit"), "普通用户不应拥有角色管理权限");
        assertFalse(commonPerms.contains("system:user:resetPwd"), "普通用户不应拥有重置密码权限");

        List<String> managerPerms = permsOfRole(jdbc, 3L);
        assertTrue(managerPerms.contains("oa:leave:approve"), "部门经理应拥有请假审批权限");
        assertTrue(managerPerms.contains("oa:reimbursement:approve"), "部门经理应拥有报销审批权限");
        assertTrue(managerPerms.containsAll(commonPerms), "部门经理权限应是普通用户权限的超集");
    }

    @Test
    void permissionsShouldSkipDisabledOrDeletedRolesAndMenus() {
        JdbcTemplate jdbc = newSchema("rbac_revocation_test");

        List<String> granted = permsOfUser(jdbc, 2L);
        assertFalse(granted.isEmpty(), "普通用户初始应拥有权限");
        assertTrue(granted.contains("oa:leave:add"), "普通用户初始应能提交请假");

        jdbc.update("UPDATE sys_role SET status = '1' WHERE role_id = 2");
        assertTrue(permsOfUser(jdbc, 2L).isEmpty(), "角色停用后不应再下发任何权限");

        jdbc.update("UPDATE sys_role SET status = '0' WHERE role_id = 2");
        jdbc.update("UPDATE sys_role SET del_flag = '1' WHERE role_id = 2");
        assertTrue(permsOfUser(jdbc, 2L).isEmpty(), "角色被逻辑删除后不应再下发任何权限");

        jdbc.update("UPDATE sys_role SET del_flag = '0' WHERE role_id = 2");
        jdbc.update("UPDATE sys_menu SET del_flag = '1' WHERE perms = 'oa:leave:add'");
        assertFalse(permsOfUser(jdbc, 2L).contains("oa:leave:add"), "菜单被逻辑删除后对应权限不应下发");
    }

    private JdbcTemplate newSchema(String databaseName) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + databaseName + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db/h2/schema.sql"));
        populator.setContinueOnError(false);
        populator.execute(dataSource);
        return new JdbcTemplate(dataSource);
    }

    private List<String> permsOfUser(JdbcTemplate jdbc, Long userId) {
        return jdbc.queryForList(selectPermsSql().replace("#{userId}", "?"), String.class, userId);
    }

    private List<String> permsOfRole(JdbcTemplate jdbc, Long roleId) {
        Long userId = jdbc.queryForObject("SELECT user_id FROM sys_user_role WHERE role_id = ?", Long.class, roleId);
        assertNotNull(userId, "角色 " + roleId + " 应至少绑定一个种子用户");
        return permsOfUser(jdbc, userId);
    }

    /** 直接复用 Mapper 上的 SQL，避免测试与实现各写一份而逐渐走偏。 */
    private String selectPermsSql() {
        try {
            Method method = SysUserMapper.class.getMethod("selectPermsByUserId", Long.class);
            Select select = method.getAnnotation(Select.class);
            assertNotNull(select, "selectPermsByUserId 必须由 @Select 提供 SQL");
            return String.join(" ", select.value());
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("找不到 SysUserMapper.selectPermsByUserId", e);
        }
    }
}
