package com.importorder.util;

import com.importorder.model.User;

public class SessionManager {

    private static User currentUser;

    public static void login(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static String getRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }

    public static boolean isRole(String role) {
        return role != null && role.equals(getRole());
    }

    public static String getUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }

    public static String getSiteCode() {
        // Chỉ có giá trị khi role = SITE
        return currentUser != null ? currentUser.getSiteCode() : null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}