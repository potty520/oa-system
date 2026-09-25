package com.oa.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oa.common.core.domain.BaseEntity;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@TableName("sys_user")
public class SysUser extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long userId;

    @JsonProperty("userName")
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @JsonProperty("nickName")
    private String nickname;

    private String email;

    @JsonProperty("phonenumber")
    private String phone;

    private String avatar;
    private String sex;
    private Long deptId;
    private String status;

    /** 首次登录必须修改密码（0否 1是） */
    private String mustChangePwd;

    private String loginIp;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date loginDate;

    @TableField(exist = false)
    private SysDept dept;

    @TableField(exist = false)
    private List<Long> roleIds = new ArrayList<>();
    @TableField(exist = false)
    private List<Long> postIds = new ArrayList<>();
}