package com.importorder.service;

import com.importorder.model.User;
import com.importorder.repository.UserRepository;
import com.importorder.util.AppException;
import com.importorder.util.SessionManager;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {

    private final UserRepository userRepo;

    // Constructor for Dependency Injection (for testing)
    public AuthService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    // Default Constructor (for production)
    public AuthService() {
        this.userRepo = new UserRepository();
    }

    public User login(String username, String password) {
        if (username == null || username.isBlank() ||
            password == null || password.isBlank()) {
            throw new AppException("Vui lòng nhập đầy đủ username và mật khẩu.");
        }

        User user = userRepo.findByUsername(username.trim());

        // Không tiết lộ username hay password cái nào sai
        if (user == null || !BCrypt.checkpw(password, user.getPasswordHash())) {
            throw new AppException("Thông tin đăng nhập không đúng.");
        }

        if (!user.isActive()) {
            throw new AppException("Tài khoản đã bị khóa. Vui lòng liên hệ Admin.");
        }

        SessionManager.login(user);
        return user;
    }

    public void logout() {
        SessionManager.logout();
    }

    public void changePassword(String oldPassword, String newPassword, String confirmPassword) {
        User current = SessionManager.getCurrentUser();
        if (current == null) throw new AppException("Chưa đăng nhập.");

        if (!BCrypt.checkpw(oldPassword, current.getPasswordHash())) {
            throw new AppException("Mật khẩu cũ không đúng.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new AppException("Mật khẩu mới tối thiểu 8 ký tự.");
        }
        if (!newPassword.matches(".*[a-zA-Z].*") || !newPassword.matches(".*[0-9].*")) {
            throw new AppException("Mật khẩu mới phải có cả chữ cái và số.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new AppException("Mật khẩu xác nhận không khớp.");
        }
        if (BCrypt.checkpw(newPassword, current.getPasswordHash())) {
            throw new AppException("Mật khẩu mới không được trùng mật khẩu cũ.");
        }

        String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        userRepo.updatePassword(current.getUsername(), newHash);
        current.setPasswordHash(newHash); // cập nhật session
    }

    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }
}