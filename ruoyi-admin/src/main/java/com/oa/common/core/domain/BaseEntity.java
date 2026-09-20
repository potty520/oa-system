package com.oa.common.core.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
public class BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 创建者 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新者 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /** 删除标志（0存在 2删除） */
    @TableLogic
    @JsonIgnore
    private String delFlag;

    /** 备注 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String remark;

    /** 搜索值（非数据库字段） */
    @TableField(exist = false)
    private String searchValue;

    /** 请求参数 */
    @TableField(exist = false)
    private Map<String, Object> params = new HashMap<>();
}
