package com.importorder.controller;

import com.importorder.service.AuthService;
import com.importorder.util.AlertUtils;
import com.importorder.util.AppException;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

public class ChangePasswordController {

    @FXML private PasswordField txtOldPassword;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirm;
    @FXML private Label lblOldErr;
    @FXML private Label lblNewErr;
    @FXML private Label lblConfirmErr;
    @FXML private Label lblError;

    private final AuthService authService = new AuthService();
    private Runnable onSuccess;

    public void setOnSuccess(Runnable callback) {
        this.onSuccess = callback;
    }

    @FXML
    private void handleSave() {
        clearErrors();
        boolean hasError = false;

        if (txtOldPassword.getText().isBlank()) {
            showFieldErr(lblOldErr, "Vui lòng nhập mật khẩu cũ.");
            hasError = true;
        }
        if (txtNewPassword.getText().length() < 8) {
            showFieldErr(lblNewErr, "Mật khẩu mới tối thiểu 8 ký tự.");
            hasError = true;
        }
        if (!txtNewPassword.getText().equals(txtConfirm.getText())) {
            showFieldErr(lblConfirmErr, "Mật khẩu xác nhận không khớp.");
            hasError = true;
        }
        if (hasError) return;

        try {
            authService.changePassword(
                txtOldPassword.getText(),
                txtNewPassword.getText(),
                txtConfirm.getText()
            );
            AlertUtils.showInfo("Thành công", "Đổi mật khẩu thành công!");
            if (onSuccess != null) onSuccess.run();
            handleClose();
        } catch (AppException e) {
            showErr(e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) txtOldPassword.getScene().getWindow();
        stage.close();
    }

    private void clearErrors() {
        lblOldErr.setVisible(false); lblOldErr.setManaged(false);
        lblNewErr.setVisible(false); lblNewErr.setManaged(false);
        lblConfirmErr.setVisible(false); lblConfirmErr.setManaged(false);
        lblError.setVisible(false); lblError.setManaged(false);
    }

    private void showFieldErr(Label lbl, String msg) {
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
    }

    private void showErr(String msg) {
        lblError.setText(msg); lblError.setVisible(true); lblError.setManaged(true);
    }
}