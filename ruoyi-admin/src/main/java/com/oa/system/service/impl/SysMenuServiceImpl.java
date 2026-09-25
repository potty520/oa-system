package com.oa.system.service.impl;

import com.oa.system.domain.SysMenu;
import com.oa.system.mapper.SysMenuMapper;
import com.oa.system.service.ISysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysMenuServiceImpl implements ISysMenuService {

    @Autowired
    private SysMenuMapper menuMapper;

    @Override
    public List<SysMenu> selectMenuTree() {
        List<SysMenu> menus = menuMapper.selectMenuListForTree();
        Map<Long, SysMenu> indexed = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            menu.setChildren(new ArrayList<>());
            if (menu.getMenuId() != null) {
                indexed.put(menu.getMenuId(), menu);
            }
        }
        List<SysMenu> roots = new ArrayList<>();
        for (SysMenu menu : menus) {
            SysMenu parent = menu.getParentId() == null ? null : indexed.get(menu.getParentId());
            // 父节点缺失或指向自身时按根节点处理，避免菜单在授权树里凭空消失
            if (parent == null || parent == menu) {
                roots.add(menu);
            } else {
                parent.getChildren().add(menu);
            }
        }
        return roots;
    }
}
