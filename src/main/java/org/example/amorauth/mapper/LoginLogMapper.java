package org.example.amorauth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.amorauth.entity.LoginLog;
import org.example.amorauth.dto.LoginLogDto;

import java.util.List;

@Mapper
public interface LoginLogMapper {

    int insertLoginLog(LoginLog loginLog);

    List<LoginLogDto> findByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    List<LoginLogDto> findRecentLogs(@Param("limit") Integer limit);

    long countByUserId(@Param("userId") Long userId);

    LoginLog findLatestByUserId(@Param("userId") Long userId);
}
