package com.importorder.controller.site;

import com.importorder.model.SiteOrder;
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
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class Site_OrderListController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private Label lblSiteCode;
    @FXML private TableView<SiteOrder> tblOrders;
    @FXML private TableColumn<SiteOrder, String> colId;
    @FXML private TableColumn<SiteOrder, String> colBatch;
    @FXML private TableColumn<SiteOrder, String> colMeans;
    @FXML private TableColumn<SiteOrder, String> colArrival;
    @FXML private TableColumn<SiteOrder, String> colConfirmed;
    @FXML private TableColumn<SiteOrder, String> colStatus;
    @FXML private TableColumn<SiteOrder, Void> colActions;

    private final SiteOrderService siteOrderService = new SiteOrderService();
    private String siteCode;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        siteCode = SessionManager.getSiteCode();
        lblSiteCode.setText("SITE: " + (siteCode != null ? siteCode : "--"));
        setupTable();
        loadData();
    }

    private void setupTable() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSiteOrderId()));
        colBatch.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBatchId()));
        colMeans.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDeliveryMeans()));
        colArrival.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getEstimatedArrival() != null
                ? DateUtils.formatDate(c.getValue().getEstimatedArrival()) : "--"));
        colConfirmed.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().isConfirmedBySite() ? "✅ Đã xác nhận" : "⏳ Chưa xác nhận"));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btnConfirm = new Button("✓ Xác nhận");
            final Button btnCancel = new Button("Yêu cầu hủy");
            final HBox box = new HBox(6, btnConfirm, btnCancel);
            {
                btnConfirm.setStyle("-fx-background-color: rgba(34,197,94,0.15); " +
                    "-fx-text-fill: #22C55E; -fx-background-radius: 6px; -fx-cursor: hand; -fx-font-size: 11px;");
                btnCancel.setStyle("-fx-background-color: rgba(239,68,68,0.15); " +
                    "-fx-text-fill: #EF4444; -fx-background-radius: 6px; -fx-cursor: hand; -fx-font-size: 11px;");

                btnConfirm.setOnAction(e -> {
                    SiteOrder so = getTableView().getItems().get(getIndex());
                    handleConfirm(so);
                });
                btnCancel.setOnAction(e -> {
                    SiteOrder so = getTableView().getItems().get(getIndex());
                    handleRequestCancel(so);
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                SiteOrder so = getTableView().getItems().get(getIndex());
                boolean isSent = "SENT".equals(so.getStatus()) || "CONFIRMED".equals(so.getStatus());
                btnConfirm.setVisible(!so.isConfirmedBySite() && isSent);
                btnConfirm.setManaged(!so.isConfirmedBySite() && isSent);
                btnCancel.setVisible(isSent && so.getCancelRequestedAt() == null);
                btnCancel.setManaged(isSent && so.getCancelRequestedAt() == null);
                setGraphic(box);
            }
        });
    }

    private void loadData() {
        if (siteCode == null) return;
        tblOrders.setItems(FXCollections.observableArrayList(
            siteOrderService.getBySite(siteCode)));
    }

    private void handleConfirm(SiteOrder so) {
        if (AlertUtils.showConfirm("Xác nhận đơn",
                "Xác nhận đã nhận đơn hàng " + so.getSiteOrderId() + "?")) {
            try {
                siteOrderService.confirmBySite(so.getSiteOrderId());
                loadData();
            } catch (Exception e) {
                AlertUtils.showError("Lỗi", e.getMessage());
            }
        }
    }

    private void handleRequestCancel(SiteOrder so) {
        String reason = AlertUtils.showConfirmWithReason(
            "Yêu cầu hủy", "Nhập lý do yêu cầu hủy đơn " + so.getSiteOrderId() + ":");
        if (reason != null) {
            try {
                siteOrderService.requestCancel(so.getSiteOrderId(), reason);
                AlertUtils.showInfo("Đã gửi", "Yêu cầu hủy đã được gửi tới OOD.");
                loadData();
            } catch (Exception e) {
                AlertUtils.showError("Lỗi", e.getMessage());
            }
        }
    }

    @FXML private void goDashboard() { navigateTo("/fxml/site/Site_Dashboard.fxml"); }
    @FXML private void goProfile() { navigateTo("/fxml/site/Site_Profile.fxml"); }
    @FXML private void goCatalog() { navigateTo("/fxml/site/Site_Catalog.fxml"); }
    @FXML private void goStock() { navigateTo("/fxml/site/Site_StockUpdate.fxml"); }
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