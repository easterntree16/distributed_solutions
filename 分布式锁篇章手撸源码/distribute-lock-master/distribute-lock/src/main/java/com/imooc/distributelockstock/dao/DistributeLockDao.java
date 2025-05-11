package com.imooc.distributelockstock.dao;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DistributeLockDao {
    @Select("select lock_name from distribute_lock where lock_name = #{lockName} for update")
    List<String> queryLockNameForUpdate(@Param("lockName") String lockName);

    @Insert("insert into record_lock (lock_name, uuid) values (#{lockName}, #{uuid})")
    Integer insert(@Param("lockName") String lockName, @Param("uuid") String uuid);

    @Delete("delete from record_lock where lock_name = #{lockName} and uuid = #{uuid}")
    Integer delete(@Param("lockName") String lockName, @Param("uuid") String uuid);
}
