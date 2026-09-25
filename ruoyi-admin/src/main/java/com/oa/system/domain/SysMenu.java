package com.oa.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.oa.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.List;

@Data
@TableName("sys_menu")
public class SysMenu extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long menuId;
    private String menuName;
    private Long parentId;
    private Integer orderNum;
    private String path;
    private String component;
    private String menuType;
    private String visible;
    private String status;
    private String perms;
    private String icon;

    /** 子菜单，仅用于组装授权树返回前端，不映射数据库列。 */
    @TableField(exist = false)
    private List<SysMenu> children;
}
