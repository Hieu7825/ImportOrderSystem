package com.importorder.controller.ood;

import com.importorder.service.OrderRequestService;
import com.importorder.service.SiteOrderService;
import com.importorder.service.SiteService;
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

public class OOD_DashboardController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private Label lblPending;
    @FXML private Label lblProcessing;
    @FXML private Label lblActiveSites;
    @FXML private Label lblCancelReqs;

    private final OrderRequestService orderService = new OrderRequestService();
    private final SiteService siteService = new SiteService();
    private final SiteOrderService siteOrderService = new SiteOrderService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        loadStats();
    }

    private void loadStats() {
        var allReqs = orderService.getRequestsForCurrentUser();
        lblPending.setText(String.valueOf(
            allReqs.stream().filter(r -> "PENDING".equals(r.getStatus())).count()));
        lblProcessing.setText(String.valueOf(
            allReqs.stream().filter(r -> "PROCESSING".equals(r.getStatus())).count()));
        lblActiveSites.setText(String.valueOf(siteService.getActiveSites().size()));
        lblCancelReqs.setText(String.valueOf(siteOrderService.getCancelRequests().size()));
    }

    @FXML private void goOrderRequests() { navigateTo("/fxml/ood/OOD_OrderRequestList.fxml"); }
    @FXML private void goSiteOrders() { navigateTo("/fxml/ood/OOD_SiteOrderList.fxml"); }
    @FXML private void goCancelRequests() { navigateTo("/fxml/ood/OOD_CancelRequestList.fxml"); }
    @FXML private void goSites() { navigateTo("/fxml/ood/OOD_SiteList.fxml"); }
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