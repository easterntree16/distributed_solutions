package com.example.mysqlexpiretask.dao;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RecordLockDao {

    @Select("select id from record_lock where modify_time < #{expireTime}")
    List<Integer> selectExpireLock(@Param("expireTime") String expireTime);

    @Delete("delete from record_lock where id = #{id}")
    Integer delete(@Param("id") Integer id);
}
