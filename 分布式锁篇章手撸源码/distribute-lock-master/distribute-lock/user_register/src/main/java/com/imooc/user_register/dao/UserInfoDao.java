package com.imooc.user_register.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserInfoDao {
    @Insert("insert into user_info (phone, name) values(#{phone}, #{name})")
    Integer insert(@Param("phone") String phone, @Param("name") String name);

    @Select("select count(*) from user_info where phone = #{phone}")
    Integer selectByPhone(@Param("phone") String phone);

    @Select("select count(*) from user_info where name = #{name}")
    Integer selectByName(@Param("name") String name);
}
