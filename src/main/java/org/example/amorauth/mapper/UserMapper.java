package org.example.amorauth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.amorauth.entity.User;

@Mapper
public interface UserMapper {

    User findByGoogleId(@Param("googleId") String googleId);

    User findByEmail(@Param("email") String email);

    int insertUser(User user);

    int updateUser(User user);

    User findById(@Param("id") Long id);
}
