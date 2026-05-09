package com.importorder.service;

import com.importorder.model.User;
import com.importorder.repository.UserRepository;
import com.importorder.util.AppException;
import com.importorder.util.SessionManager;

import java.time.LocalDateTime;
import java.util.List;

public class UserManagementService {

    private final UserRepository userRepo = new UserRepository();

    public User createUser(String username, String fullName, String role,
                           String siteCode, String tempPassword) {
        if (username == null || username.isBlank())
            throw new AppException("Username không được để trống.");
        if (userRepo.findByUsername(username) != null)
            throw new AppException("Username '" + username + "' đã tồn tại.");
        if ("SITE".equals(role) && (siteCode == null || siteCode.isBlank()))
            throw new AppException("Tài khoản Site phải được liên kết với một site.");
        if (tempPassword == null || tempPassword.length() < 8)
            throw new AppException("Mật khẩu tạm phải có ít nhất 8 ký tự.");

        User user = new User();
        user.setUsername(username.trim());
        user.setFullName(fullName);
        user.setRole(role);
        user.setSiteCode("SITE".equals(role) ? siteCode : null);
        user.setPasswordHash(AuthService.hashPassword(tempPassword));
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userRepo.save(user);
        return user;
    }

    public void updateUser(String username, String fullName, String role, String siteCode) {
        User user = userRepo.findByUsername(username);
        if (user == null) throw new AppException("Không tìm thấy user: " + username);
        if ("SITE".equals(role) && (siteCode == null || siteCode.isBlank()))
            throw new AppException("Tài khoản Site phải được liên kết với một site.");

        userRepo.updateInfo(username, fullName, role, siteCode);
    }

    public void toggleActive(String username) {
        String currentUsername = SessionManager.getUsername();
        if (username.equals(currentUsername))
            throw new AppException("Không thể khóa tài khoản đang sử dụng.");

        User user = userRepo.findByUsername(username);
        if (user == null) throw new AppException("Không tìm thấy user: " + username);

        userRepo.setActive(username, !user.isActive());
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public User getUserByUsername(String username) {
        return userRepo.findByUsername(username);
    }
}