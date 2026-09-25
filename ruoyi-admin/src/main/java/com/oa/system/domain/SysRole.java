package com.oa.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oa.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.List;

@Data
@TableName("sys_role")
public class SysRole extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long roleId;
    private String roleName;
    private String roleKey;
    private Integer roleSort;
    private String dataScope;
    private String status;

    /** 自定义数据范围的部门 ID；null 表示本次请求未提交该字段，保留原有配置。 */
    @TableField(exist = false)
    private List<Long> deptIds;

    /** 角色可访问的菜单 ID；null 表示本次请求未提交该字段，保留原有配置。 */
    @TableField(exist = false)
    private List<Long> menuIds;
}