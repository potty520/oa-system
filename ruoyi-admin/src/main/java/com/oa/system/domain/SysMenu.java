package com.oa.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.oa.common.core.domain.BaseEntity;
import lombok.Data;

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
}
