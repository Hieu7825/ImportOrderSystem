package com.importorder.controller.admin;

import com.importorder.model.SiteInfo;
import com.importorder.model.User;
import com.importorder.service.SiteService;
import com.importorder.service.UserManagementService;
import com.importorder.util.AlertUtils;
import com.importorder.util.AppException;
import com.importorder.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class Admin_UserFormController implements Initializable {

    @FXML private Label      lblUserName;
    @FXML private Label      lblTitle;
    @FXML private TextField  txtUsername;
    @FXML private TextField  txtFullName;
    @FXML private ComboBox<String> cmbRole;
    @FXML private VBox       vboxSiteCode;
    @FXML private ComboBox<String> cmbSiteCode;
    @FXML private Label      lblSiteHint;        // ← thêm: gợi ý về trạng thái site
    @FXML private PasswordField txtPassword;
    @FXML private Label      lblPasswordLabel;
    @FXML private Button     btnSave;
    @FXML private Label      lblUsernameErr;
    @FXML private Label      lblFullNameErr;
    @FXML private Label      lblRoleErr;
    @FXML private Label      lblSiteErr;
    @FXML private Label      lblPasswordErr;
    @FXML private Label      lblError;

    private final UserManagementService userService = new UserManagementService();
    private final SiteService           siteService = new SiteService();
    private User editingUser = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        cmbRole.setItems(FXCollections.observableArrayList(
            "SD", "OOD", "SITE", "WM", "ADMIN"));
        cmbRole.setOnAction(e -> handleRoleChange());
    }

    public void setEditMode(User user) {
        this.editingUser = user;
        lblTitle.setText("Sửa tài khoản");
        btnSave.setText("Lưu thay đổi");

        txtUsername.setText(user.getUsername());
        txtUsername.setDisable(true);
        txtFullName.setText(user.getFullName());
        cmbRole.setValue(user.getRole());
        if (user.getSiteCode() != null) cmbSiteCode.setValue(user.getSiteCode());

        txtPassword.setVisible(false);
        txtPassword.setManaged(false);
        lblPasswordLabel.setVisible(false);
        lblPasswordLabel.setManaged(false);

        handleRoleChange();
    }

    @FXML
    private void handleRoleChange() {
        boolean isSite = "SITE".equals(cmbRole.getValue());
        vboxSiteCode.setVisible(isSite);
        vboxSiteCode.setManaged(isSite);

        if (isSite) {
            loadSiteCodes();
        }
    }

    /**
     * Khi tạo mới: chỉ hiện site có partnerStatus = INVITED (chờ liên doanh).
     * Khi sửa: hiện tất cả site (kể cả ACTIVE).
     */
    private void loadSiteCodes() {
        cmbSiteCode.getItems().clear();

        if (editingUser == null) {
            // Tạo mới: chỉ lấy site INVITED
            List<SiteInfo> invitedSites = siteService.getInvitedSites();
            if (invitedSites.isEmpty()) {
                cmbSiteCode.setPromptText("Không có site nào đang chờ liên kết");
                if (lblSiteHint != null) {
                    lblSiteHint.setText(
                        "⚠ Chưa có site nào được OOD mời liên doanh.\n"
                        + "OOD cần gửi lời mời liên doanh trước khi tạo tài khoản SITE.");
                    lblSiteHint.setVisible(true);
                    lblSiteHint.setManaged(true);
                }
            } else {
                invitedSites.forEach(s ->
                    cmbSiteCode.getItems().add(
                        s.getSiteCode() + " — " + s.getSiteName()));
                if (lblSiteHint != null) {
                    lblSiteHint.setText(
                        "Chọn site đã được OOD mời liên doanh (đang ở trạng thái INVITED).");
                    lblSiteHint.setVisible(true);
                    lblSiteHint.setManaged(true);
                }
            }
        } else {
            // Sửa: hiện tất cả site
            siteService.getAllSites().forEach(s ->
                cmbSiteCode.getItems().add(s.getSiteCode()));
            if (editingUser.getSiteCode() != null)
                cmbSiteCode.setValue(editingUser.getSiteCode());
            if (lblSiteHint != null) {
                lblSiteHint.setVisible(false);
                lblSiteHint.setManaged(false);
            }
        }
    }

    @FXML
    private void handleSave() {
        clearErrors();
        boolean hasError = false;

        if (txtFullName.getText().isBlank()) {
            showFieldErr(lblFullNameErr, "Họ tên không được để trống.");
            hasError = true;
        }
        if (cmbRole.getValue() == null) {
            showFieldErr(lblRoleErr, "Vui lòng chọn role.");
            hasError = true;
        }
        if ("SITE".equals(cmbRole.getValue()) && cmbSiteCode.getValue() == null) {
            showFieldErr(lblSiteErr, "Vui lòng chọn site.");
            hasError = true;
        }
        if (editingUser == null && txtPassword.getText().length() < 8) {
            showFieldErr(lblPasswordErr, "Mật khẩu tối thiểu 8 ký tự.");
            hasError = true;
        }
        if (hasError) return;

        // Lấy siteCode thuần (bỏ phần " — Tên site" nếu có)
        String siteCodeRaw = cmbSiteCode.getValue();
        String siteCode = siteCodeRaw != null && siteCodeRaw.contains(" — ")
            ? siteCodeRaw.split(" — ")[0].trim()
            : siteCodeRaw;

        try {
            if (editingUser == null) {
                if (txtUsername.getText().isBlank()) {
                    showFieldErr(lblUsernameErr, "Username không được để trống.");
                    return;
                }
                userService.createUser(
                    txtUsername.getText().trim(),
                    txtFullName.getText().trim(),
                    cmbRole.getValue(),
                    siteCode,
                    txtPassword.getText()
                );
                AlertUtils.showInfo("Thành công",
                    "Tạo tài khoản '" + txtUsername.getText() + "' thành công!");
            } else {
                userService.updateUser(
                    editingUser.getUsername(),
                    txtFullName.getText().trim(),
                    cmbRole.getValue(),
                    siteCode
                );
                AlertUtils.showInfo("Thành công", "Cập nhật tài khoản thành công!");
            }
            goUserList();
        } catch (AppException e) {
            showErr(e.getMessage());
        }
    }

    @FXML private void goUserList()  { navigateTo("/fxml/admin/Admin_UserList.fxml"); }
    @FXML private void goDashboard() { navigateTo("/fxml/admin/Admin_Dashboard.fxml"); }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        navigateTo("/fxml/Login.fxml");
    }

    private void navigateTo(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(
                getClass().getResource("/css/global.css").toExternalForm());
            Stage stage = (Stage) btnSave.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }

    private void clearErrors() {
        lblUsernameErr.setVisible(false); lblUsernameErr.setManaged(false);
        lblFullNameErr.setVisible(false); lblFullNameErr.setManaged(false);
        lblRoleErr.setVisible(false);     lblRoleErr.setManaged(false);
        lblSiteErr.setVisible(false);     lblSiteErr.setManaged(false);
        lblPasswordErr.setVisible(false); lblPasswordErr.setManaged(false);
        lblError.setVisible(false);       lblError.setManaged(false);
    }

    private void showFieldErr(Label lbl, String msg) {
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
    }

    private void showErr(String msg) {
        lblError.setText(msg); lblError.setVisible(true); lblError.setManaged(true);
    }
}