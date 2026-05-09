package com.importorder.controller.ood;

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

public class OOD_CancelRequestListController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private TableView<SiteOrder> tblCancelReqs;
    @FXML private TableColumn<SiteOrder, String> colSiteOrderId;
    @FXML private TableColumn<SiteOrder, String> colSite;
    @FXML private TableColumn<SiteOrder, String> colReason;
    @FXML private TableColumn<SiteOrder, String> colRequestedAt;
    @FXML private TableColumn<SiteOrder, Void> colActions;

    private final SiteOrderService siteOrderService = new SiteOrderService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        setupTable();
        loadData();
    }

    private void setupTable() {
        colSiteOrderId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSiteOrderId()));
        colSite.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSiteCode()));
        colReason.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCancelRequestReason()));
        colRequestedAt.setCellValueFactory(c -> new SimpleStringProperty(
            DateUtils.formatDateTime(c.getValue().getCancelRequestedAt())));
        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btnApprove = new Button("✓ Duyệt");
            final Button btnReject = new Button("✕ Từ chối");
            final HBox box = new HBox(6, btnApprove, btnReject);
            {
                btnApprove.setStyle("-fx-background-color: rgba(34,197,94,0.15); -fx-text-fill: #22C55E; -fx-background-radius: 6px; -fx-cursor: hand; -fx-font-size: 11px;");
                btnReject.setStyle("-fx-background-color: rgba(239,68,68,0.15); -fx-text-fill: #EF4444; -fx-background-radius: 6px; -fx-cursor: hand; -fx-font-size: 11px;");
                btnApprove.setOnAction(e -> {
                    SiteOrder so = getTableView().getItems().get(getIndex());
                    handleApprove(so);
                });
                btnReject.setOnAction(e -> {
                    SiteOrder so = getTableView().getItems().get(getIndex());
                    handleReject(so);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadData() {
        tblCancelReqs.setItems(FXCollections.observableArrayList(
            siteOrderService.getCancelRequests()));
    }

    private void handleApprove(SiteOrder so) {
        if (AlertUtils.showConfirm("Duyệt hủy", "Xác nhận duyệt hủy đơn " + so.getSiteOrderId() + "?")) {
            try {
                siteOrderService.approveCancelRequest(so.getSiteOrderId());
                loadData();
            } catch (Exception e) {
                AlertUtils.showError("Lỗi", e.getMessage());
            }
        }
    }

    private void handleReject(SiteOrder so) {
        if (AlertUtils.showConfirm("Từ chối", "Từ chối yêu cầu hủy đơn " + so.getSiteOrderId() + "?")) {
            try {
                siteOrderService.rejectCancelRequest(so.getSiteOrderId());
                loadData();
            } catch (Exception e) {
                AlertUtils.showError("Lỗi", e.getMessage());
            }
        }
    }

    @FXML private void goDashboard() { navigateTo("/fxml/ood/OOD_Dashboard.fxml"); }
    @FXML private void goOrderRequests() { navigateTo("/fxml/ood/OOD_OrderRequestList.fxml"); }
    @FXML private void goSiteOrders() { navigateTo("/fxml/ood/OOD_SiteOrderList.fxml"); }
    @FXML private void goSites() { navigateTo("/fxml/ood/OOD_SiteList.fxml"); }
    @FXML private void handleLogout() { SessionManager.logout(); navigateTo("/fxml/Login.fxml"); }

    private void navigateTo(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/css/global.css").toExternalForm());
            Stage stage = (Stage) tblCancelReqs.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }
}