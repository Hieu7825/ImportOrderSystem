package com.importorder.controller.ood;

import com.importorder.model.FinalOrder;
import com.importorder.model.SiteOrder;
import com.importorder.model.SubBatch;
import com.importorder.service.SiteOrderService;
import com.importorder.util.AlertUtils;
import com.importorder.util.DateUtils;
import com.importorder.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class OOD_SiteOrderDetailController implements Initializable {

    @FXML private Label  lblUserName;
    @FXML private Label  lblSiteOrderId;
    @FXML private Label  lblBatchId;
    @FXML private Label  lblSiteCode;
    @FXML private Label  lblMeans;
    @FXML private Label  lblArrival;
    @FXML private Label  lblStatus;
    @FXML private Label  lblConfirmed;
    @FXML private Button btnCancel;
    @FXML private Button btnFindReplacement;   // ← thêm mới: tìm phương án thay thế
    @FXML private TableView<FinalOrder> tblItems;
    @FXML private TableColumn<FinalOrder, String> colCode;
    @FXML private TableColumn<FinalOrder, String> colName;
    @FXML private TableColumn<FinalOrder, String> colQty;
    @FXML private TableColumn<FinalOrder, String> colUnit;
    @FXML private TableColumn<FinalOrder, String> colMeans;
    @FXML private TableColumn<FinalOrder, String> colItemStatus;

    private final SiteOrderService siteOrderService = new SiteOrderService();
    private String siteOrderId;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        setupTable();
    }

    private void setupTable() {
        colCode.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getItemCode()));
        colName.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getItemName() != null
                ? c.getValue().getItemName() : "--"));
        colQty.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getQuantityOrdered())));
        colUnit.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getUnit()));
        colMeans.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getDeliveryMeans()));
        colItemStatus.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getStatus()));
    }

    public void setSiteOrderId(String siteOrderId) {
        this.siteOrderId = siteOrderId;
        loadData();
    }

    private void loadData() {
        SiteOrder so = siteOrderService.getAll().stream()
            .filter(s -> s.getSiteOrderId().equals(siteOrderId))
            .findFirst().orElse(null);
        if (so == null) return;

        lblSiteOrderId.setText(so.getSiteOrderId());
        lblBatchId.setText(so.getBatchId()
            + (so.getSubBatchId() != null ? " / " + so.getSubBatchId() : ""));
        lblSiteCode.setText(so.getSiteCode());
        lblMeans.setText(so.getDeliveryMeans());
        lblArrival.setText(so.getEstimatedArrival() != null
            ? DateUtils.formatDate(so.getEstimatedArrival()) : "--");

        String status = so.getStatus();
        lblStatus.setText(status);
        lblStatus.setStyle(switch (status) {
            case "SENT"        -> "-fx-text-fill: #38BDF8; -fx-font-size: 13px; -fx-font-weight: 600;";
            case "CONFIRMED"   -> "-fx-text-fill: #4F6EF7; -fx-font-size: 13px; -fx-font-weight: 600;";
            case "RECEIVED"    -> "-fx-text-fill: #22C55E; -fx-font-size: 13px; -fx-font-weight: 600;";
            case "DISCREPANCY" -> "-fx-text-fill: #F59E0B; -fx-font-size: 13px; -fx-font-weight: 600;";
            case "CANCELLED"   -> "-fx-text-fill: #EF4444; -fx-font-size: 13px; -fx-font-weight: 600;";
            default            -> "-fx-text-fill: #8892A8; -fx-font-size: 13px;";
        });

        lblConfirmed.setText(so.isConfirmedBySite()
            ? "✅ Site đã xác nhận lúc " + DateUtils.formatDateTime(so.getConfirmedAt())
            : "⏳ Chưa xác nhận");

        // Nút Hủy: chỉ hiện khi SENT và chưa xác nhận
        boolean canCancel = "SENT".equals(status) && !so.isConfirmedBySite();
        btnCancel.setVisible(canCancel);
        btnCancel.setManaged(canCancel);

        // Nút Tìm phương án thay thế: chỉ hiện khi CANCELLED
        boolean canReplace = "CANCELLED".equals(status);
        if (btnFindReplacement != null) {
            btnFindReplacement.setVisible(canReplace);
            btnFindReplacement.setManaged(canReplace);
        }

        tblItems.setItems(FXCollections.observableArrayList(
            siteOrderService.getItemsOfSiteOrder(siteOrderId)));
    }

    @FXML
    private void handleCancel() {
        String reason = AlertUtils.showConfirmWithReason(
            "Hủy đơn hàng", "Xác nhận hủy đơn " + siteOrderId + "?");
        if (reason != null) {
            try {
                siteOrderService.cancelSiteOrder(siteOrderId, reason);
                loadData();
            } catch (Exception e) {
                AlertUtils.showError("Lỗi", e.getMessage());
            }
        }
    }

    /**
     * PHASE 5: OOD nhấn "Tìm phương án thay thế" trên đơn CANCELLED.
     * → Tạo sub-batch REPLACEMENT → chuyển sang SupplyDashboard để xử lý.
     */
    @FXML
    private void handleFindReplacement() {
        boolean confirm = AlertUtils.showConfirm(
            "Tìm phương án thay thế",
            "Xác nhận tạo phương án thay thế cho đơn " + siteOrderId + "?\n\n"
            + "Hệ thống sẽ tạo một lần xử lý mới (sub-batch) "
            + "chứa các mặt hàng trong đơn bị hủy.");
        if (!confirm) return;

        try {
            SubBatch replacement = siteOrderService.startReplacement(siteOrderId);
            AlertUtils.showInfo("Đã tạo phương án thay thế",
                "Sub-batch thay thế: " + replacement.getSubBatchId()
                + "\nVui lòng tra cứu tồn kho và sinh đơn mới.");

            // Chuyển sang SupplyDashboard với batchId gốc + subBatchId
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/ood/OOD_SupplyDashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(
                getClass().getResource("/css/global.css").toExternalForm());
            OOD_SupplyDashboardController ctrl = loader.getController();
            ctrl.setBatchId(replacement.getParentBatchId());
            ctrl.setSubBatchId(replacement.getSubBatchId());
            Stage stage = (Stage) lblUserName.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }

    @FXML private void goSiteOrders()     { navigateTo("/fxml/ood/OOD_SiteOrderList.fxml"); }
    @FXML private void goDashboard()      { navigateTo("/fxml/ood/OOD_Dashboard.fxml"); }
    @FXML private void goOrderRequests()  { navigateTo("/fxml/ood/OOD_OrderRequestList.fxml"); }
    @FXML private void goCancelRequests() { navigateTo("/fxml/ood/OOD_CancelRequestList.fxml"); }
    @FXML private void goSites()          { navigateTo("/fxml/ood/OOD_SiteList.fxml"); }
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