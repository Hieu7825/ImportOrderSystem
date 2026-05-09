package com.importorder.controller.ood;

import com.importorder.service.SiteInvitationService;
import com.importorder.util.AlertUtils;
import com.importorder.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class OOD_SiteInviteFormController implements Initializable {

    @FXML private Label  lblUserName;
    @FXML private TextField txtSiteCode;
    @FXML private TextField txtSiteName;
    @FXML private TextField txtCountry;
    @FXML private Label  lblSiteCodeErr;
    @FXML private Label  lblSiteNameErr;
    @FXML private Label  lblError;

    private final SiteInvitationService inviteService = new SiteInvitationService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
    }

    @FXML
    private void handleSend() {
        clearErrors();
        boolean hasError = false;

        if (txtSiteCode.getText().isBlank()) {
            showFieldErr(lblSiteCodeErr, "Mã site không được để trống.");
            hasError = true;
        }
        if (txtSiteName.getText().isBlank()) {
            showFieldErr(lblSiteNameErr, "Tên site không được để trống.");
            hasError = true;
        }
        if (hasError) return;

        try {
            var inv = inviteService.sendInvite(
                txtSiteCode.getText().trim(),
                txtSiteName.getText().trim(),
                txtCountry.getText().trim()
            );
            AlertUtils.showInfo("Đã gửi lời mời",
                "Lời mời liên doanh đã được gửi tới site '"
                + inv.getSiteCode() + "'.\n\n"
                + "Mã lời mời: " + inv.getInviteId() + "\n\n"
                + "Tiếp theo:\n"
                + "1. Admin tạo tài khoản SITE với mã '" + inv.getSiteCode() + "'\n"
                + "2. SITE đăng nhập và chấp nhận lời mời\n"
                + "3. Site sẽ xuất hiện trong danh sách khi SITE đã xác nhận");
            goSites();
        } catch (Exception e) {
            showErr(e.getMessage());
        }
    }

    @FXML private void handleCancel() { goSites(); }

    @FXML private void goSites()          { navigateTo("/fxml/ood/OOD_SiteList.fxml"); }
    @FXML private void goDashboard()      { navigateTo("/fxml/ood/OOD_Dashboard.fxml"); }
    @FXML private void goOrderRequests()  { navigateTo("/fxml/ood/OOD_OrderRequestList.fxml"); }
    @FXML private void goSiteOrders()     { navigateTo("/fxml/ood/OOD_SiteOrderList.fxml"); }
    @FXML private void goCancelRequests() { navigateTo("/fxml/ood/OOD_CancelRequestList.fxml"); }
    @FXML private void handleLogout()     { SessionManager.logout(); navigateTo("/fxml/Login.fxml"); }

    private void navigateTo(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(
                getClass().getResource("/css/global.css").toExternalForm());
            Stage stage = (Stage) lblUserName.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }

    private void clearErrors() {
        lblSiteCodeErr.setVisible(false); lblSiteCodeErr.setManaged(false);
        lblSiteNameErr.setVisible(false); lblSiteNameErr.setManaged(false);
        lblError.setVisible(false);       lblError.setManaged(false);
    }

    private void showFieldErr(Label lbl, String msg) {
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
    }

    private void showErr(String msg) {
        lblError.setText(msg); lblError.setVisible(true); lblError.setManaged(true);
    }
}