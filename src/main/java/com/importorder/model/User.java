package com.importorder.model;

import org.bson.types.ObjectId;
import java.time.LocalDateTime;

public class User {
    private ObjectId id;
    private String username;
    private String passwordHash;
    private String role; // SD | OOD | SITE | WM | ADMIN
    private String fullName;
    private String siteCode;       // chỉ dùng khi role = SITE
    private boolean isActive;      // false = tài khoản bị khóa
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User() {}

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}