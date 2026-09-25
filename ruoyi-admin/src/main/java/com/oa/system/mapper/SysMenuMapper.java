package com.oa.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oa.system.domain.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("SELECT menu_id, menu_name, parent_id, order_num, path, component, menu_type, " +
            "visible, status, perms, icon FROM sys_menu " +
            "WHERE del_flag = '0' ORDER BY parent_id, order_num, menu_id")
    List<SysMenu> selectMenuListForTree();
}
