package com.oa.system.service;

import com.oa.system.domain.SysMenu;

import java.util.List;

public interface ISysMenuService {

    /** 返回按父子关系组装好的完整菜单树，供角色授权界面勾选。 */
    List<SysMenu> selectMenuTree();
}
