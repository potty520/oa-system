package com.oa.common.constant;

public class Constants {
    /** 通用成功标识 */
    public static final String SUCCESS = "200";

    /** 通用失败标识 */
    public static final String FAIL = "500";

    /** 登录成功 */
    public static final String LOGIN_SUCCESS = "Success";

    /** 登录失败 */
    public static final String LOGIN_FAIL = "Error";

    /** Token前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** Token请求头 */
    public static final String HEADER = "Authorization";

    /** 用户ID */
    public static final String SYS_USER_ID = "currentUserId";

    /** 用户名 */
    public static final String SYS_USERNAME = "currentUserName";

    /** 用户角色 */
    public static final String SYS_USER_ROLE = "currentUserRole";

    /** 状态正常 */
    public static final String STATUS_NORMAL = "0";

    /** 状态停用 */
    public static final String STATUS_DISABLE = "1";

    /** 删除标志：存在 */
    public static final String DEL_FLAG = "0";

    /** 删除标志：已删除 */
    public static final String DEL_FLAG_DELETED = "2";
}
