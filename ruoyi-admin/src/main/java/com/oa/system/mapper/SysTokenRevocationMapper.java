package com.oa.system.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;

/**
 * 令牌吊销记录（sys_token_revocation）数据访问。
 *
 * <p>表上只有 revoke_key 一个主键，没有自增列、也没有逻辑删除字段，用不上 BaseMapper，
 * 因此直接写注解 SQL。时间一律由 Java 侧作为参数传入：SQLite 的 CURRENT_TIMESTAMP 返回 UTC，
 * 而 JDBC 绑定的是本地时间，两者混用会让比较整体偏移一个时区差（本项目实测相差 8 小时），
 * 所以这里不取数据库当前时间。</p>
 */
@Mapper
public interface SysTokenRevocationMapper {

    /** 查询仍未失效的单 Token 吊销记录到期时间；无记录或记录已过期时返回 null。 */
    @Select("SELECT expire_time FROM sys_token_revocation WHERE revoke_key = #{revokeKey} AND expire_time > #{now}")
    Date selectLiveExpireTime(@Param("revokeKey") String revokeKey, @Param("now") Date now);

    /** 查询仍未失效的用户级吊销时间；无记录或记录已过期时返回 null。 */
    @Select("SELECT revoked_at FROM sys_token_revocation WHERE revoke_key = #{revokeKey} AND expire_time > #{now}")
    Date selectLiveRevokedAt(@Param("revokeKey") String revokeKey, @Param("now") Date now);

    @Delete("DELETE FROM sys_token_revocation WHERE revoke_key = #{revokeKey}")
    int deleteByKey(@Param("revokeKey") String revokeKey);

    @Insert("INSERT INTO sys_token_revocation(revoke_key, revoked_at, expire_time) "
            + "VALUES(#{revokeKey}, #{revokedAt}, #{expireTime})")
    int insert(@Param("revokeKey") String revokeKey,
               @Param("revokedAt") Date revokedAt,
               @Param("expireTime") Date expireTime);

    @Delete("DELETE FROM sys_token_revocation WHERE expire_time <= #{now}")
    int deleteExpired(@Param("now") Date now);
}
