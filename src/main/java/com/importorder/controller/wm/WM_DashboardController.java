package com.importorder.controller.wm;

import com.importorder.service.SiteOrderService;
import com.importorder.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import com.importorder.util.NavigationHelper;

import java.net.URL;
import java.util.ResourceBundle;

public class WM_DashboardController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private Label lblPending;
    @FXML private Label lblPartial;
    @FXML private Label lblDiscrepancy;

    private final SiteOrderService siteOrderService = new SiteOrderService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        loadStats();
    }

    private void loadStats() {
        var all = siteOrderService.getAll();
        lblPending.setText(String.valueOf(
            all.stream().filter(s -> "SENT".equals(s.getStatus())).count()));
        lblPartial.setText(String.valueOf(
            all.stream().filter(s -> "PARTIALLY_RECEIVED".equals(s.getStatus())).count()));
        lblDiscrepancy.setText(String.valueOf(
            all.stream().filter(s -> "DISCREPANCY".equals(s.getStatus())).count()));
    }

    @FXML private void goInspection() { navigateTo("/fxml/wm/WM_Inspection.fxml"); }
    @FXML private void goConfirmReceived() { navigateTo("/fxml/wm/WM_ConfirmReceived.fxml"); }
    @FXML private void goOrderList() { navigateTo("/fxml/wm/WM_OrderList.fxml"); }
    @FXML private void handleLogout() { SessionManager.logout(); navigateTo("/fxml/Login.fxml"); }

    private void navigateTo(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/css/global.css").toExternalForm());
            Stage stage = (Stage) lblUserName.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            com.importorder.util.AlertUtils.showError("Lỗi", e.getMessage());
        }
    }
    @FXML
    private void handleChangePassword() {
        NavigationHelper.openChangePassword(
            (Stage) lblUserName.getScene().getWindow());
    }
}