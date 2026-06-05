package com.importorder.controller.site;

import com.importorder.model.SiteInfo;
import com.importorder.service.SiteService;
import com.importorder.util.AlertUtils;
import com.importorder.util.AppException;
import com.importorder.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class Site_ProfileController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private Label lblSiteCode;
    @FXML private Label lblSiteCodeVal;
    @FXML private TextField txtSiteName;
    @FXML private TextField txtCountry;
    @FXML private TextField txtEmail;
    @FXML private TextField txtShipDays;
    @FXML private TextField txtAirDays;
    @FXML private TextArea txtOtherInfo;
    @FXML private Label lblError;

    private final SiteService siteService = new SiteService();
    private String siteCode;
    private SiteInfo currentSite;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        siteCode = SessionManager.getSiteCode();
        lblSiteCode.setText("SITE: " + (siteCode != null ? siteCode : "--"));
        loadData();
    }

    private void loadData() {
        if (siteCode == null) return;
        currentSite = siteService.getByCode(siteCode);
        if (currentSite == null) return;

        lblSiteCodeVal.setText(currentSite.getSiteCode());
        txtSiteName.setText(currentSite.getSiteName() != null ? currentSite.getSiteName() : "");
        txtCountry.setText(currentSite.getCountry() != null ? currentSite.getCountry() : "");
        txtEmail.setText(currentSite.getContactEmail() != null ? currentSite.getContactEmail() : "");
        txtShipDays.setText(String.valueOf(currentSite.getShipDays()));
        txtAirDays.setText(String.valueOf(currentSite.getAirDays()));
        txtOtherInfo.setText(currentSite.getOtherInfo() != null ? currentSite.getOtherInfo() : "");
    }

    @FXML
    private void handleSave() {
        hideError();
        String siteName = txtSiteName.getText().trim();
        String country = txtCountry.getText().trim();
        String email = txtEmail.getText().trim();
        String otherInfo = txtOtherInfo.getText().trim();

        if (siteName.isBlank()) {
            showError("Tên site không được để trống.");
            return;
        }

        int shipDays, airDays;
        try {
            shipDays = Integer.parseInt(txtShipDays.getText().trim());
            airDays = Integer.parseInt(txtAirDays.getText().trim());
            if (shipDays <= 0 || airDays <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Số ngày vận chuyển phải là số nguyên dương.");
            return;
        }

        if (airDays >= shipDays) {
            boolean proceed = AlertUtils.showConfirm("Cảnh báo",
                "Thông thường hàng không nhanh hơn tàu. Xác nhận tiếp tục?");
            if (!proceed) return;
        }

        try {
            siteService.updateInfo(siteCode, siteName, country, email, shipDays, airDays, otherInfo);
            AlertUtils.showInfo("Thành công", "Cập nhật thông tin site thành công!");
            loadData();
        } catch (AppException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleReset() {
        loadData();
        hideError();
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    @FXML private void goDashboard() { navigateTo("/fxml/site/Site_Dashboard.fxml"); }
    @FXML private void goCatalog() { navigateTo("/fxml/site/Site_Catalog.fxml"); }
    @FXML private void goStock() { navigateTo("/fxml/site/Site_StockUpdate.fxml"); }
    @FXML private void goOrders() { navigateTo("/fxml/site/Site_OrderList.fxml"); }
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