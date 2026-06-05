package com.importorder.controller.ood;

import com.importorder.model.SiteInfo;
import com.importorder.service.SiteService;
import com.importorder.util.AlertUtils;
import com.importorder.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * OOD chỉ XEM thông tin site — không sửa, không thêm catalog.
 * Mọi chỉnh sửa là do SITE tự thực hiện qua Site_Profile / Site_Catalog.
 */
public class OOD_SiteDetailController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private Label lblSiteCode;
    @FXML private Label lblSiteName;
    @FXML private Label lblCountry;
    @FXML private Label lblEmail;
    @FXML private Label lblShipDays;
    @FXML private Label lblAirDays;
    @FXML private Label lblPartnerStatus;
    @FXML private Label lblStatus;
    @FXML private Button btnDeactivate;
    @FXML private ListView<String> lstCatalog;

    private final SiteService siteService = new SiteService();
    private String siteCode;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
    }

    public void setSiteCode(String siteCode) {
        this.siteCode = siteCode;
        loadData();
    }

    private void loadData() {
        SiteInfo site = siteService.getByCode(siteCode);
        if (site == null) return;

        lblSiteCode.setText(site.getSiteCode());
        lblSiteName.setText(site.getSiteName() != null ? site.getSiteName() : "--");
        lblCountry.setText(site.getCountry() != null ? site.getCountry() : "--");
        lblEmail.setText(site.getContactEmail() != null ? site.getContactEmail() : "--");
        lblShipDays.setText(site.getShipDays() > 0 ? site.getShipDays() + " ngày" : "Chưa cập nhật");
        lblAirDays.setText(site.getAirDays() > 0 ? site.getAirDays() + " ngày" : "Chưa cập nhật");

        // Partner status
        String ps = site.getPartnerStatus();
        String psLabel = switch (ps != null ? ps : "") {
            case "INVITED"  -> "📨 Chờ SITE phản hồi";
            case "ACTIVE"   -> "✅ Đang liên doanh";
            case "INACTIVE" -> "⛔ Ngừng liên doanh";
            default         -> ps != null ? ps : "--";
        };
        lblPartnerStatus.setText(psLabel);

        boolean isActive = "ACTIVE".equals(site.getStatus())
                        && "ACTIVE".equals(site.getPartnerStatus());
        lblStatus.setText(isActive ? "✅ Đang hoạt động" : "⛔ Không hoạt động");
        lblStatus.setStyle(isActive
            ? "-fx-text-fill: #22C55E; -fx-font-size: 13px; -fx-font-weight: 600;"
            : "-fx-text-fill: #EF4444; -fx-font-size: 13px; -fx-font-weight: 600;");

        btnDeactivate.setVisible(isActive);
        btnDeactivate.setManaged(isActive);

        // Catalog — chỉ hiển thị
        lstCatalog.setItems(FXCollections.observableArrayList(
            site.getCatalogItems() != null ? site.getCatalogItems() : new ArrayList<>()));
    }

    @FXML
    private void handleDeactivate() {
        String reason = AlertUtils.showConfirmWithReason("Ngừng liên doanh",
            "Xác nhận ngừng liên doanh với " + siteCode + "?");
        if (reason != null) {
            try {
                try {
                    siteService.deactivate(siteCode);
                } catch (com.importorder.util.AppException ex) {
                    if (ex.getMessage().startsWith("WARN:")) {
                        if (AlertUtils.showConfirm("Cảnh báo",
                                ex.getMessage().substring(5)))
                            siteService.deactivateForced(siteCode);
                        else return;
                    } else throw ex;
                }
                loadData();
            } catch (Exception e) {
                AlertUtils.showError("Lỗi", e.getMessage());
            }
        }
    }

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
}