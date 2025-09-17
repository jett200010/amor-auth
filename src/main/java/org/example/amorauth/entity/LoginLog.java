package org.example.amorauth.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LoginLog {
    private Long id;
    private Long userId;
    private LocalDateTime loginTime;
    private String ipAddress;
    private String userAgent;
    private String loginType;
    private Boolean success;
    private String errorMessage;
}
