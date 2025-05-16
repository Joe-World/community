package org.wgd.community.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.wgd.community.model.pojo.User;

@Mapper
public interface UserMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(User record);

    User selectByPrimaryKey(Integer id);

    List<User> selectAll();

    int updateByPrimaryKey(User record);

    User selectByName(String username);

    User selectByEmail(String email);

    int updateStatusById(int id, int status);

    int updateHeaderById(int id, String headerUrl);

    int updatePassword(@Param("userId") int userId, @Param("password") String password, @Param("salt") String salt);
}