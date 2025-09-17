package org.example.amorauth.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LoginLogDto {
    private Long id;
    private Long userId;
    private LocalDateTime loginTime;
    private String ipAddress;
    private String userAgent;
    private String loginType;
    private Boolean success;
    private String errorMessage;
    private String userName;
    private String userEmail;
}
