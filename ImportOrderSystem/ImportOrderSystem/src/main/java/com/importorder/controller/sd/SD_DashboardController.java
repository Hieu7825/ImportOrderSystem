package com.importorder.controller.sd;

import com.importorder.service.OrderRequestService;
import com.importorder.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.net.URL;
import java.util.ResourceBundle;
import com.importorder.util.NavigationHelper;

public class SD_DashboardController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private Label lblPending;
    @FXML private Label lblProcessing;
    @FXML private Label lblCompleted;

    private final OrderRequestService orderService = new OrderRequestService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        loadStats();
    }

    private void loadStats() {
        var myRequests = orderService.getRequestsForCurrentUser();
        lblPending.setText(String.valueOf(
            myRequests.stream().filter(r -> "PENDING".equals(r.getStatus())).count()));
        lblProcessing.setText(String.valueOf(
            myRequests.stream().filter(r -> "PROCESSING".equals(r.getStatus())).count()));
        lblCompleted.setText(String.valueOf(
            myRequests.stream().filter(r -> "COMPLETED".equals(r.getStatus())).count()));
    }

    @FXML private void goOrderList() { navigateTo("/fxml/sd/SD_OrderRequestList.fxml"); }
    @FXML private void goMerchandise() { navigateTo("/fxml/sd/SD_MerchandiseList.fxml"); }

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