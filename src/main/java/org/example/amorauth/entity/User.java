package org.example.amorauth.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String googleId;
    private String email;
    private String name;
    private String picture;
    private String locale;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void setLastLoginAt(LocalDateTime now) {

    }
}
