package com.importorder.controller.site;

import com.importorder.model.SiteInvitation;
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

/**
 * Màn hình SITE thấy lời mời liên doanh từ OOD.
 * SITE điền thêm thông tin (ship/air days, email) rồi chấp nhận hoặc từ chối.
 */
public class Site_InvitationController implements Initializable {

    @FXML private Label      lblUserName;
    @FXML private Label      lblSiteCode;
    @FXML private Label      lblInviteSiteCode;
    @FXML private Label      lblSiteName;
    @FXML private Label      lblCountry;
    @FXML private Label      lblInvitedBy;
    @FXML private TextField  txtShipDays;
    @FXML private TextField  txtAirDays;
    @FXML private TextField  txtEmail;
    @FXML private TextArea   txtOtherInfo;
    @FXML private Label      lblShipErr;
    @FXML private Label      lblAirErr;
    @FXML private Label      lblError;
    @FXML private Label      lblNoInvite;       // hiện khi không có lời mời
    @FXML private javafx.scene.layout.VBox vboxForm; // ẩn/hiện theo có lời mời không

    private final SiteInvitationService inviteService = new SiteInvitationService();
    private SiteInvitation currentInvitation;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        String siteCode = SessionManager.getSiteCode();
        lblSiteCode.setText("SITE: " + (siteCode != null ? siteCode : "--"));
        loadInvitation();
    }

    private void loadInvitation() {
        currentInvitation = inviteService.getMyInvitation();

        if (currentInvitation == null) {
            // Không có lời mời PENDING
            if (vboxForm != null)    { vboxForm.setVisible(false); vboxForm.setManaged(false); }
            if (lblNoInvite != null) { lblNoInvite.setVisible(true); lblNoInvite.setManaged(true); }
            return;
        }

        if (vboxForm != null)    { vboxForm.setVisible(true); vboxForm.setManaged(true); }
        if (lblNoInvite != null) { lblNoInvite.setVisible(false); lblNoInvite.setManaged(false); }

        lblInviteSiteCode.setText(currentInvitation.getSiteCode());
        lblSiteName.setText(currentInvitation.getSiteName());
        lblCountry.setText(currentInvitation.getCountry() != null
            ? currentInvitation.getCountry() : "--");
        lblInvitedBy.setText("Gửi bởi: " + currentInvitation.getInvitedBy());
    }

    @FXML
    private void handleAccept() {
        clearErrors();
        if (currentInvitation == null) return;

        int shipDays, airDays;
        try {
            shipDays = Integer.parseInt(txtShipDays.getText().trim());
            if (shipDays <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showFieldErr(lblShipErr, "Số ngày tàu phải là số nguyên dương.");
            return;
        }
        try {
            airDays = Integer.parseInt(txtAirDays.getText().trim());
            if (airDays <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showFieldErr(lblAirErr, "Số ngày hàng không phải là số nguyên dương.");
            return;
        }

        if (airDays >= shipDays) {
            boolean proceed = AlertUtils.showConfirm("Cảnh báo",
                "Thông thường hàng không nhanh hơn tàu. Xác nhận tiếp tục?");
            if (!proceed) return;
        }

        try {
            inviteService.acceptInvite(
                currentInvitation.getInviteId(),
                shipDays, airDays,
                txtEmail.getText().trim(),
                txtOtherInfo.getText().trim()
            );
            AlertUtils.showInfo("Chào mừng!",
                "Bạn đã chấp nhận liên doanh thành công!\n"
                + "Site '" + currentInvitation.getSiteCode()
                + "' hiện đang ACTIVE và sẵn sàng nhận đơn hàng.");
            // Chuyển về dashboard
            navigateTo("/fxml/site/Site_Dashboard.fxml");
        } catch (Exception e) {
            showErr(e.getMessage());
        }
    }

    @FXML
    private void handleReject() {
        if (currentInvitation == null) return;
        String reason = AlertUtils.showConfirmWithReason(
            "Từ chối lời mời",
            "Nhập lý do từ chối liên doanh với hệ thống này:");
        if (reason != null) {
            try {
                inviteService.rejectInvite(currentInvitation.getInviteId(), reason);
                AlertUtils.showInfo("Đã từ chối",
                    "Bạn đã từ chối lời mời liên doanh.\n"
                    + "OOD sẽ được thông báo.");
                loadInvitation();
            } catch (Exception e) {
                showErr(e.getMessage());
            }
        }
    }

    @FXML private void goDashboard() { navigateTo("/fxml/site/Site_Dashboard.fxml"); }
    @FXML private void goProfile()   { navigateTo("/fxml/site/Site_Profile.fxml"); }
    @FXML private void goCatalog()   { navigateTo("/fxml/site/Site_Catalog.fxml"); }
    @FXML private void goStock()     { navigateTo("/fxml/site/Site_StockUpdate.fxml"); }
    @FXML private void goOrders()    { navigateTo("/fxml/site/Site_OrderList.fxml"); }
    @FXML private void handleLogout(){ SessionManager.logout(); navigateTo("/fxml/Login.fxml"); }

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
        lblShipErr.setVisible(false); lblShipErr.setManaged(false);
        lblAirErr.setVisible(false);  lblAirErr.setManaged(false);
        lblError.setVisible(false);   lblError.setManaged(false);
    }

    private void showFieldErr(Label lbl, String msg) {
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
    }

    private void showErr(String msg) {
        lblError.setText(msg); lblError.setVisible(true); lblError.setManaged(true);
    }
}