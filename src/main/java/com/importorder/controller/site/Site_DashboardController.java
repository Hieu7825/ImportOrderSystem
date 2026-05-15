package com.importorder.controller.site;

import com.importorder.model.SiteInvitation;
import com.importorder.service.SiteInvitationService;
import com.importorder.service.SiteOrderService;
import com.importorder.service.SiteService;
import com.importorder.util.AlertUtils;
import com.importorder.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.importorder.util.NavigationHelper;

import java.net.URL;
import java.util.ResourceBundle;

public class Site_DashboardController implements Initializable {

    @FXML private Label  lblUserName;
    @FXML private Label  lblSiteCode;
    @FXML private Label  lblNewOrders;
    @FXML private Label  lblUnconfirmed;
    @FXML private Label  lblCatalogCount;

    // ← thêm mới: banner thông báo lời mời liên doanh đang chờ
    @FXML private VBox   vboxInviteBanner;
    @FXML private Label  lblInviteMsg;
    @FXML private Button btnGoInvite;

    private final SiteOrderService      siteOrderService = new SiteOrderService();
    private final SiteService           siteService      = new SiteService();
    private final SiteInvitationService inviteService    = new SiteInvitationService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        String siteCode = SessionManager.getSiteCode();
        lblSiteCode.setText("SITE: " + (siteCode != null ? siteCode : "--"));
        loadStats(siteCode);
        checkPendingInvitation();
    }

    private void loadStats(String siteCode) {
        if (siteCode == null) return;
        var orders = siteOrderService.getBySite(siteCode);
        lblNewOrders.setText(String.valueOf(
            orders.stream().filter(o -> "SENT".equals(o.getStatus())).count()));
        lblUnconfirmed.setText(String.valueOf(
            orders.stream()
                .filter(o -> "SENT".equals(o.getStatus()) && !o.isConfirmedBySite())
                .count()));
        var site = siteService.getByCode(siteCode);
        lblCatalogCount.setText(site != null && site.getCatalogItems() != null
            ? String.valueOf(site.getCatalogItems().size()) : "0");
    }

    /**
     * Kiểm tra xem site có lời mời liên doanh đang chờ không.
     * Nếu có → hiện banner nhắc SITE vào xem và chấp nhận.
     */
    private void checkPendingInvitation() {
        if (vboxInviteBanner == null) return;

        SiteInvitation inv = inviteService.getMyInvitation();
        if (inv != null && "PENDING".equals(inv.getStatus())) {
            vboxInviteBanner.setVisible(true);
            vboxInviteBanner.setManaged(true);
            if (lblInviteMsg != null)
                lblInviteMsg.setText(
                    "📨 Bạn có lời mời liên doanh từ hệ thống (mã: "
                    + inv.getInviteId() + ").\n"
                    + "Vui lòng điền thông tin và chấp nhận để bắt đầu nhận đơn hàng.");
        } else {
            vboxInviteBanner.setVisible(false);
            vboxInviteBanner.setManaged(false);
        }
    }

    @FXML private void goToInvitation() { navigateTo("/fxml/site/Site_Invitation.fxml"); }
    @FXML private void goProfile()      { navigateTo("/fxml/site/Site_Profile.fxml"); }
    @FXML private void goCatalog()      { navigateTo("/fxml/site/Site_Catalog.fxml"); }
    @FXML private void goStock()        { navigateTo("/fxml/site/Site_StockUpdate.fxml"); }
    @FXML private void goOrders()       { navigateTo("/fxml/site/Site_OrderList.fxml"); }
    @FXML private void handleLogout()   { SessionManager.logout(); navigateTo("/fxml/Login.fxml"); }

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

    @FXML
    private void handleChangePassword() {
        NavigationHelper.openChangePassword((Stage) lblUserName.getScene().getWindow());
    }
}