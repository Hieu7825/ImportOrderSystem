package com.importorder.controller.ood;

import com.importorder.model.Merchandise;
import com.importorder.model.SiteInfo;
import com.importorder.service.MerchandiseService;
import com.importorder.service.SiteService;
import com.importorder.util.AlertUtils;
import com.importorder.util.AppException;
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
import java.util.List;
import java.util.ResourceBundle;

public class OOD_SiteDetailController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private Label lblSiteCode;
    @FXML private Label lblStatus;
    @FXML private TextField txtSiteName;
    @FXML private TextField txtCountry;
    @FXML private TextField txtEmail;
    @FXML private TextField txtShipDays;
    @FXML private TextField txtAirDays;
    @FXML private Button btnDeactivate;
    @FXML private ListView<String> lstCatalog;

    private final SiteService siteService = new SiteService();
    private final MerchandiseService merchService = new MerchandiseService();
    private String siteCode;
    private SiteInfo currentSite;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
    }

    public void setSiteCode(String siteCode) {
        this.siteCode = siteCode;
        loadData();
    }

    private void loadData() {
        currentSite = siteService.getByCode(siteCode);
        if (currentSite == null) return;

        lblSiteCode.setText(currentSite.getSiteCode());
        txtSiteName.setText(currentSite.getSiteName() != null ? currentSite.getSiteName() : "");
        txtCountry.setText(currentSite.getCountry() != null ? currentSite.getCountry() : "");
        txtEmail.setText(currentSite.getContactEmail() != null ? currentSite.getContactEmail() : "");
        txtShipDays.setText(String.valueOf(currentSite.getShipDays()));
        txtAirDays.setText(String.valueOf(currentSite.getAirDays()));

        boolean isActive = "ACTIVE".equals(currentSite.getStatus());
        lblStatus.setText(isActive ? "✅ Đang hoạt động" : "⛔ Ngừng liên doanh");
        lblStatus.setStyle(isActive
            ? "-fx-text-fill: #22C55E; -fx-font-size: 13px; -fx-font-weight: 600;"
            : "-fx-text-fill: #EF4444; -fx-font-size: 13px; -fx-font-weight: 600;");
        btnDeactivate.setVisible(isActive);
        btnDeactivate.setManaged(isActive);

        // Catalog
        List<String> catalog = currentSite.getCatalogItems() != null
            ? currentSite.getCatalogItems() : new ArrayList<>();
        lstCatalog.setItems(FXCollections.observableArrayList(catalog));
    }

    @FXML
    private void handleSaveInfo() {
        try {
            int ship = Integer.parseInt(txtShipDays.getText().trim());
            int air = Integer.parseInt(txtAirDays.getText().trim());
            siteService.updateInfo(siteCode, txtSiteName.getText().trim(),
                txtCountry.getText().trim(), txtEmail.getText().trim(), ship, air, "");
            AlertUtils.showInfo("Thành công", "Cập nhật thông tin site thành công!");
            loadData();
        } catch (NumberFormatException e) {
            AlertUtils.showError("Lỗi", "Số ngày phải là số nguyên dương.");
        } catch (AppException e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }

    @FXML
    private void handleAddCatalog() {
        List<String> existing = currentSite.getCatalogItems() != null
            ? new ArrayList<>(currentSite.getCatalogItems()) : new ArrayList<>();
        List<String> available = merchService.getAllActive().stream()
            .map(Merchandise::getItemCode)
            .filter(c -> !existing.contains(c))
            .toList();

        if (available.isEmpty()) {
            AlertUtils.showInfo("Thông báo", "Tất cả mặt hàng đã có trong catalog.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(available.get(0), available);
        dialog.setTitle("Thêm mặt hàng");
        dialog.setHeaderText("Chọn mã hàng thêm vào catalog:");
        dialog.setContentText("Mã hàng:");
        dialog.showAndWait().ifPresent(code -> {
            try {
                existing.add(code);
                siteService.updateCatalog(siteCode, existing);
                loadData();
            } catch (Exception e) {
                AlertUtils.showError("Lỗi", e.getMessage());
            }
        });
    }

    @FXML
    private void handleDeactivate() {
        String reason = AlertUtils.showConfirmWithReason("Ngừng liên doanh",
            "Xác nhận ngừng liên doanh với " + siteCode + "?");
        if (reason != null) {
            try {
                try {
                    siteService.deactivate(siteCode);
                } catch (AppException ex) {
                    if (ex.getMessage().startsWith("WARN:")) {
                        if (AlertUtils.showConfirm("Cảnh báo", ex.getMessage().substring(5)))
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

    @FXML private void goSites() { navigateTo("/fxml/ood/OOD_SiteList.fxml"); }
    @FXML private void goDashboard() { navigateTo("/fxml/ood/OOD_Dashboard.fxml"); }
    @FXML private void goOrderRequests() { navigateTo("/fxml/ood/OOD_OrderRequestList.fxml"); }
    @FXML private void goSiteOrders() { navigateTo("/fxml/ood/OOD_SiteOrderList.fxml"); }
    @FXML private void goCancelRequests() { navigateTo("/fxml/ood/OOD_CancelRequestList.fxml"); }
    @FXML private void handleLogout() { SessionManager.logout(); navigateTo("/fxml/Login.fxml"); }

    private void navigateTo(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/css/global.css").toExternalForm());
            Stage stage = (Stage) lblUserName.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }
}