package com.importorder.service;

import com.importorder.model.User;
import com.importorder.repository.SiteOrderRepository;
import com.importorder.repository.UserRepository;
import com.importorder.util.AppException;
import com.importorder.util.SessionManager;

import java.time.LocalDateTime;
import java.util.List;

public class UserManagementService {

    private final UserRepository      userRepo;
    private final SiteOrderRepository siteOrderRepo;

    // Constructor for Dependency Injection (for testing)
    public UserManagementService(UserRepository userRepo, SiteOrderRepository siteOrderRepo) {
        this.userRepo = userRepo;
        this.siteOrderRepo = siteOrderRepo;
    }

    // Default Constructor (for production)
    public UserManagementService() {
        this.userRepo = new UserRepository();
        this.siteOrderRepo = new SiteOrderRepository();
    }

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
        if (user == null)
            throw new AppException("Không tìm thấy user: " + username);
        if ("SITE".equals(role) && (siteCode == null || siteCode.isBlank()))
            throw new AppException("Tài khoản Site phải được liên kết với một site.");
        userRepo.updateInfo(username, fullName, role, siteCode);
    }

    /**
     * Edge Case E1: Cảnh báo nếu khóa tài khoản SITE đang có đơn hàng SENT.
     * Trả về số đơn đang chờ để UI hiển thị cảnh báo.
     */
    public int countPendingSiteOrders(String username) {
        User user = userRepo.findByUsername(username);
        if (user == null || !"SITE".equals(user.getRole()) || user.getSiteCode() == null)
            return 0;
        return (int) siteOrderRepo.findBySite(user.getSiteCode()).stream()
            .filter(so -> "SENT".equals(so.getStatus())
                       || "CONFIRMED".equals(so.getStatus()))
            .count();
    }

    /**
     * Toggle khóa/mở khóa tài khoản.
     * Nếu là SITE user có đơn SENT → caller phải gọi countPendingSiteOrders
     * trước và hiển thị cảnh báo; service vẫn cho phép khóa sau khi xác nhận.
     */
    public void toggleActive(String username) {
        String currentUsername = SessionManager.getUsername();
        if (username.equals(currentUsername))
            throw new AppException("Không thể khóa tài khoản đang sử dụng.");

        User user = userRepo.findByUsername(username);
        if (user == null)
            throw new AppException("Không tìm thấy user: " + username);

        userRepo.setActive(username, !user.isActive());
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public User getUserByUsername(String username) {
        return userRepo.findByUsername(username);
    }
}