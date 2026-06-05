package com.importorder.controller;

import com.importorder.model.User;
import com.importorder.service.AuthService;
import com.importorder.util.AppException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;
    @FXML private Label lblUsernameError;
    @FXML private Label lblPasswordError;
    @FXML private Button btnLogin;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleLogin() {
        // Reset errors
        hideError();

        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        // Client-side validate
        boolean hasError = false;
        if (username.isEmpty()) {
            showFieldError(lblUsernameError, "Vui lòng nhập username.");
            hasError = true;
        }
        if (password.isEmpty()) {
            showFieldError(lblPasswordError, "Vui lòng nhập mật khẩu.");
            hasError = true;
        }
        if (hasError) return;

        try {
            User user = authService.login(username, password);
            navigateToDashboard(user);
        } catch (AppException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Không thể kết nối hệ thống. Vui lòng thử lại sau.");
        }
    }

    private void navigateToDashboard(User user) {
        try {
            String fxmlPath = switch (user.getRole()) {
                case "SD"    -> "/fxml/sd/SD_Dashboard.fxml";
                case "OOD"   -> "/fxml/ood/OOD_Dashboard.fxml";
                case "SITE"  -> "/fxml/site/Site_Dashboard.fxml";
                case "WM"    -> "/fxml/wm/WM_Dashboard.fxml";
                case "ADMIN" -> "/fxml/admin/Admin_Dashboard.fxml";
                default -> throw new AppException("Role không hợp lệ: " + user.getRole());
            };

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(
                getClass().getResource("/css/global.css").toExternalForm());

            Stage stage = (Stage) btnLogin.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Import Order System — " + user.getFullName());

        } catch (Exception e) {
            showError("Lỗi khi tải giao diện: " + e.getMessage());
        }
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void showFieldError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
        lblUsernameError.setVisible(false);
        lblUsernameError.setManaged(false);
        lblPasswordError.setVisible(false);
        lblPasswordError.setManaged(false);
    }
}