package com.oa.common.core.page;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TableDataInfo<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 总记录数 */
    private long total;

    /** 列表数据 */
    private List<T> rows;

    /** 消息状态码 */
    private int code;

    /** 消息 */
    private String msg;

    public TableDataInfo() {}

    public TableDataInfo(List<T> list, long total) {
        this.rows = list;
        this.total = total;
        this.code = 200;
        this.msg = "查询成功";
    }
}
