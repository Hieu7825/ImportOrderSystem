package com.importorder.controller.ood;

import com.importorder.model.OrderRequest;
import com.importorder.service.OrderRequestService;
import com.importorder.util.AlertUtils;
import com.importorder.util.DateUtils;
import com.importorder.util.SessionManager;
import com.importorder.util.PaginationHelper;
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
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class OOD_OrderRequestListController implements Initializable {

    @FXML private Label              lblUserName;
    @FXML private ComboBox<String>   cmbStatus;
    @FXML private TextField          txtSearch;
    @FXML private TableView<OrderRequest> tblRequests;
    @FXML private TableColumn<OrderRequest, String> colBatchId;
    @FXML private TableColumn<OrderRequest, String> colCreatedBy;
    @FXML private TableColumn<OrderRequest, String> colItems;
    @FXML private TableColumn<OrderRequest, String> colStatus;      // displayStatus
    @FXML private TableColumn<OrderRequest, String> colSubBatches;  // ← thêm: số lần xử lý
    @FXML private TableColumn<OrderRequest, String> colCreatedAt;
    @FXML private TableColumn<OrderRequest, Void>   colActions;
    @FXML private Label lblPageInfo;

    private PaginationHelper<OrderRequest> pagination;
    private final OrderRequestService orderService = new OrderRequestService();
    private List<OrderRequest> allRequests;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        cmbStatus.setItems(FXCollections.observableArrayList(
            "Tất cả", "PENDING", "PROCESSING", "COMPLETED", "CANCELLED"));
        cmbStatus.setValue("Tất cả");
        setupTable();
        pagination = new PaginationHelper<>(tblRequests, lblPageInfo);
        loadData();
    }

    private void setupTable() {
        colBatchId.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getBatchId()));
        colCreatedBy.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getCreatedBy()));
        colItems.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getItems() != null
                ? c.getValue().getItems().size() + " mặt hàng" : "0"));

        // Dùng displayStatus (= status sub-batch mới nhất) thay vì status gốc
        colStatus.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getDisplayStatus()));

        // Số lần xử lý (sub-batches)
        if (colSubBatches != null) {
            colSubBatches.setCellValueFactory(c -> {
                int count = c.getValue().getSubBatches() != null
                    ? c.getValue().getSubBatches().size() : 0;
                return new SimpleStringProperty(
                    count == 0 ? "--"
                    : count + " lần" + (count > 1 ? " (có thay thế)" : ""));
            });
        }

        colCreatedAt.setCellValueFactory(c ->
            new SimpleStringProperty(DateUtils.formatDateTime(c.getValue().getCreatedAt())));

        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btnDetail  = new Button("Lịch sử");  // ← xem batch detail
            final Button btnProcess = new Button("Xử lý");
            final HBox   box        = new HBox(6, btnDetail, btnProcess);
            {
                btnDetail.setStyle(
                    "-fx-background-color: rgba(79,110,247,0.15); -fx-text-fill: #4F6EF7; " +
                    "-fx-background-radius: 6px; -fx-cursor: hand; -fx-font-size: 11px;");
                btnProcess.setStyle(
                    "-fx-background-color: rgba(34,197,94,0.15); -fx-text-fill: #22C55E; " +
                    "-fx-background-radius: 6px; -fx-cursor: hand; -fx-font-size: 11px;");

                btnDetail.setOnAction(e -> {
                    OrderRequest r = getTableView().getItems().get(getIndex());
                    goToBatchDetail(r.getBatchId());
                });
                btnProcess.setOnAction(e -> {
                    OrderRequest r = getTableView().getItems().get(getIndex());
                    goToProcess(r);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                OrderRequest r = getTableView().getItems().get(getIndex());
                String ds = r.getDisplayStatus();
                boolean canProcess = "PENDING".equals(ds) || "PROCESSING".equals(ds);
                btnProcess.setVisible(canProcess);
                btnProcess.setManaged(canProcess);
                btnProcess.setText("PROCESSING".equals(ds) ? "Tiếp tục →" : "Xử lý");
                setGraphic(box);
            }
        });
    }

    private void loadData() {
        allRequests = orderService.getRequestsForCurrentUser();
        applyFilter();
    }

    @FXML private void handleFilter() { applyFilter(); }

    private void applyFilter() {
        String status = cmbStatus.getValue();
        String search = txtSearch.getText().toLowerCase().trim();

        List<OrderRequest> filtered = allRequests.stream()
            .filter(r -> "Tất cả".equals(status)
                || status.equals(r.getDisplayStatus()))
            .filter(r -> search.isEmpty()
                || r.getBatchId().toLowerCase().contains(search)
                || (r.getCreatedBy() != null
                    && r.getCreatedBy().toLowerCase().contains(search)))
            .collect(Collectors.toList());

        pagination.setItems(filtered);
    }

    /** Xem lịch sử batch lớn (tất cả sub-batches + đơn hàng) */
    private void goToBatchDetail(String batchId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/ood/OOD_BatchDetail.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(
                getClass().getResource("/css/global.css").toExternalForm());
            OOD_BatchDetailController ctrl = loader.getController();
            ctrl.setBatchId(batchId);
            Stage stage = (Stage) tblRequests.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }

    private void goToProcess(OrderRequest r) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/ood/OOD_SupplyDashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(
                getClass().getResource("/css/global.css").toExternalForm());
            OOD_SupplyDashboardController ctrl = loader.getController();
            ctrl.setBatchId(r.getBatchId());
            Stage stage = (Stage) tblRequests.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }

    @FXML private void handlePrevPage()   { pagination.prevPage(); }
    @FXML private void handleNextPage()   { pagination.nextPage(); }
    @FXML private void goDashboard()      { navigateTo("/fxml/ood/OOD_Dashboard.fxml"); }
    @FXML private void goSiteOrders()     { navigateTo("/fxml/ood/OOD_SiteOrderList.fxml"); }
    @FXML private void goCancelRequests() { navigateTo("/fxml/ood/OOD_CancelRequestList.fxml"); }
    @FXML private void goSites()          { navigateTo("/fxml/ood/OOD_SiteList.fxml"); }
    @FXML private void handleLogout()     { SessionManager.logout(); navigateTo("/fxml/Login.fxml"); }

    private void navigateTo(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(
                getClass().getResource("/css/global.css").toExternalForm());
            Stage stage = (Stage) tblRequests.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }
}