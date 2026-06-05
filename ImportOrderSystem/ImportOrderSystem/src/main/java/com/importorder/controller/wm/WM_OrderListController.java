package com.importorder.controller.wm;

import com.importorder.model.SiteOrder;
import com.importorder.service.SiteOrderService;
import com.importorder.util.AlertUtils;
import com.importorder.util.DateUtils;
import com.importorder.util.SessionManager;
import com.importorder.util.PaginationHelper;
import com.importorder.uc1_confirm_import.WM_OrderDetailController;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class WM_OrderListController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private TableView<SiteOrder> tblOrders;
    @FXML private TableColumn<SiteOrder, String> colId;
    @FXML private TableColumn<SiteOrder, String> colBatch;
    @FXML private TableColumn<SiteOrder, String> colSite;
    @FXML private TableColumn<SiteOrder, String> colMeans;
    @FXML private TableColumn<SiteOrder, String> colArrival;
    @FXML private TableColumn<SiteOrder, String> colStatus;
    @FXML private Label lblPageInfo;

    private PaginationHelper<SiteOrder> pagination;
    private final SiteOrderService siteOrderService = new SiteOrderService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        setupTable();
        pagination = new PaginationHelper<>(tblOrders, lblPageInfo);
        loadData();
    }

    private void setupTable() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSiteOrderId()));
        colBatch.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBatchId()));
        colSite.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSiteCode()));
        colMeans.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDeliveryMeans()));
        colArrival.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getEstimatedArrival() != null
                ? DateUtils.formatDate(c.getValue().getEstimatedArrival()) : "--"));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        // Cột actions
        TableColumn<SiteOrder, Void> colActions = new TableColumn<>("THAO TÁC");
        colActions.setPrefWidth(100);
        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("Xem");
            {
                btn.setStyle("-fx-background-color: rgba(79,110,247,0.15); -fx-text-fill: #4F6EF7; -fx-background-radius: 6px; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    SiteOrder so = getTableView().getItems().get(getIndex());
                    try {
                        FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/fxml/wm/WM_OrderDetail.fxml"));
                        Scene scene = new Scene(loader.load(), 1280, 720);
                        scene.getStylesheets().add(
                            getClass().getResource("/css/global.css").toExternalForm());
                        WM_OrderDetailController ctrl = loader.getController();
                        ctrl.setSiteOrderId(so.getSiteOrderId());
                        Stage stage = (Stage) tblOrders.getScene().getWindow();
                        stage.setScene(scene);
                    } catch (Exception ex) {
                        AlertUtils.showError("Lỗi", ex.getMessage());
                    }
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });
        tblOrders.getColumns().add(colActions);
    }

    private void loadData() {
        // WM không xem các đơn đã bị HỦY (CANCELLED)
        List<SiteOrder> visible = siteOrderService.getAll().stream()
            .filter(so -> !"CANCELLED".equals(so.getStatus()))
            .collect(java.util.stream.Collectors.toList());
        pagination.setItems(visible);
    }

    // ── Pagination ────────────────────────────────────────────────────────────
    @FXML private void handlePrevPage() { pagination.prevPage(); }
    @FXML private void handleNextPage() { pagination.nextPage(); }

    // ── Navigation ────────────────────────────────────────────────────────────
    @FXML private void goDashboard()  { navigateTo("/fxml/wm/WM_Dashboard.fxml"); }
    @FXML private void goInspection() { navigateTo("/fxml/wm/WM_Inspection.fxml"); }
    @FXML private void handleLogout() { SessionManager.logout(); navigateTo("/fxml/Login.fxml"); }

    private void navigateTo(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/css/global.css").toExternalForm());
            Stage stage = (Stage) tblOrders.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }
}