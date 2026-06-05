package com.importorder.uc2_edit_order;

import com.importorder.model.SiteInfo;
import com.importorder.model.SiteOrder;
import com.importorder.service.SiteOrderService;
import com.importorder.service.SiteService;
import com.importorder.util.AlertUtils;
import com.importorder.util.DateUtils;
import com.importorder.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class OOD_SiteOrderEditController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private Label lblSiteOrderId;
    @FXML private Label lblSite;
    @FXML private ComboBox<String> cmbMeans;
    @FXML private Label lblArrivalPreview;
    @FXML private Label lblWarning;
    @FXML private Label lblError;

    private final SiteOrderService siteOrderService = new SiteOrderService();
    private final SiteService siteService = new SiteService();
    private SiteOrder currentOrder;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        cmbMeans.setItems(FXCollections.observableArrayList("ship", "air"));
        cmbMeans.setOnAction(e -> updateArrivalPreview());
    }

    public void setSiteOrderId(String siteOrderId) {
        currentOrder = siteOrderService.getAll().stream()
            .filter(s -> s.getSiteOrderId().equals(siteOrderId))
            .findFirst().orElse(null);
        if (currentOrder == null) return;

        lblSiteOrderId.setText(currentOrder.getSiteOrderId());
        lblSite.setText(currentOrder.getSiteCode());
        cmbMeans.setValue(currentOrder.getDeliveryMeans());

        if (currentOrder.isConfirmedBySite()) {
            showError("Site đã xác nhận đơn này, không thể sửa.");
            cmbMeans.setDisable(true);
        }

        updateArrivalPreview();
    }

    private void updateArrivalPreview() {
        if (currentOrder == null || cmbMeans.getValue() == null) return;
        SiteInfo site = siteService.getByCode(currentOrder.getSiteCode());
        if (site == null) return;

        int days = "ship".equals(cmbMeans.getValue()) ? site.getShipDays() : site.getAirDays();
        LocalDate arrival = LocalDate.now().plusDays(days);
        lblArrivalPreview.setText("Dự kiến về: " + DateUtils.formatDate(arrival)
            + " (+" + days + " ngày)");

        if (!cmbMeans.getValue().equals(currentOrder.getDeliveryMeans())) {
            lblWarning.setText("⚠ Bạn đang thay đổi phương tiện từ "
                + currentOrder.getDeliveryMeans() + " sang " + cmbMeans.getValue());
            lblWarning.setVisible(true);
            lblWarning.setManaged(true);
        } else {
            lblWarning.setVisible(false);
            lblWarning.setManaged(false);
        }
    }

    @FXML
    private void handleSave() {
        if (currentOrder == null) return;
        if (currentOrder.isConfirmedBySite()) {
            showError("Site đã xác nhận đơn này, không thể sửa.");
            return;
        }
        try {
            siteOrderService.editSiteOrder(currentOrder.getSiteOrderId(), cmbMeans.getValue());
            AlertUtils.showInfo("Thành công", "Cập nhật đơn hàng thành công!");
            goSiteOrders();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    @FXML private void goSiteOrders() { navigateTo("/fxml/ood/OOD_SiteOrderList.fxml"); }
    @FXML private void goDashboard()  { navigateTo("/fxml/ood/OOD_Dashboard.fxml"); }
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
