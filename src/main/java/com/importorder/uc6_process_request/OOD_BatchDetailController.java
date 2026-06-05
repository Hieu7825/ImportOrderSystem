package com.importorder.uc6_process_request;

import com.importorder.model.OrderItem;
import com.importorder.model.OrderRequest;
import com.importorder.model.SiteOrder;
import com.importorder.model.SubBatch;
import com.importorder.repository.SubBatchRepository;
import com.importorder.service.OrderRequestService;
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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class OOD_BatchDetailController implements Initializable {

    // ── Header ─────────────────────────────────────────────────────────────────
    @FXML private Label lblUserName;
    @FXML private Label lblBatchId;
    @FXML private Label lblCreatedBy;
    @FXML private Label lblCreatedAt;
    @FXML private Label lblDisplayStatus;

    // ── Mặt hàng gốc ──────────────────────────────────────────────────────────
    @FXML private TableView<OrderItem> tblOriginalItems;
    @FXML private TableColumn<OrderItem, String> colItemCode;
    @FXML private TableColumn<OrderItem, String> colItemName;
    @FXML private TableColumn<OrderItem, String> colQty;
    @FXML private TableColumn<OrderItem, String> colUnit;
    @FXML private TableColumn<OrderItem, String> colDate;

    // ── Lịch sử sub-batch ─────────────────────────────────────────────────────
    @FXML private VBox vboxSubBatches;

    private final OrderRequestService orderService     = new OrderRequestService();
    private final SiteOrderService    siteOrderService = new SiteOrderService();
    private final SubBatchRepository  subBatchRepo     = new SubBatchRepository();

    private String batchId;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        setupOriginalItemsTable();
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
        loadData();
    }

    private void setupOriginalItemsTable() {
        colItemCode.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getItemCode()));
        colItemName.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getItemName() != null
                ? c.getValue().getItemName() : "--"));
        colQty.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getQuantityOrdered())));
        colUnit.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getUnit()));
        colDate.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getDesiredDeliveryDate() != null
                ? DateUtils.formatDate(c.getValue().getDesiredDeliveryDate()) : "--"));
    }

    private void loadData() {
        OrderRequest req = orderService.getByBatchId(batchId);
        if (req == null) return;

        lblBatchId.setText(req.getBatchId());
        lblCreatedBy.setText(req.getCreatedBy());
        lblCreatedAt.setText(DateUtils.formatDateTime(req.getCreatedAt()));

        String ds = req.getDisplayStatus();
        lblDisplayStatus.setText(ds);
        lblDisplayStatus.setStyle(switch (ds != null ? ds : "") {
            case "PENDING"    -> "-fx-text-fill: #F59E0B; -fx-font-weight: 600;";
            case "PROCESSING" -> "-fx-text-fill: #38BDF8; -fx-font-weight: 600;";
            case "COMPLETED"  -> "-fx-text-fill: #22C55E; -fx-font-weight: 600;";
            case "CANCELLED"  -> "-fx-text-fill: #EF4444; -fx-font-weight: 600;";
            default           -> "-fx-text-fill: #8892A8;";
        });

        if (req.getItems() != null)
            tblOriginalItems.setItems(
                FXCollections.observableArrayList(req.getItems()));

        buildSubBatchHistory(req.getSubBatches());
    }

    private void buildSubBatchHistory(List<SubBatch> subBatches) {
        if (vboxSubBatches == null) return;
        vboxSubBatches.getChildren().clear();

        if (subBatches == null || subBatches.isEmpty()) {
            Label empty = new Label("Chưa có lần xử lý nào.");
            empty.setStyle("-fx-text-fill: #4A5368; -fx-font-size: 12px;");
            vboxSubBatches.getChildren().add(empty);
            return;
        }

        for (SubBatch sb : subBatches) {
            VBox card = new VBox(8);
            card.setStyle(
                "-fx-background-color: #13161E; -fx-border-color: #252A3A; " +
                "-fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12;");

            String typeLabel = "ORIGINAL".equals(sb.getType())
                ? "📋 Lần xử lý gốc" : "🔄 Phương án thay thế";
            String replacingInfo = sb.getReplacingOrderId() != null
                ? "  (thay cho: " + sb.getReplacingOrderId() + ")" : "";

            Label subHeader = new Label(
                typeLabel + " — " + sb.getSubBatchId() + replacingInfo);
            subHeader.setStyle(
                "-fx-text-fill: #E8ECF5; -fx-font-size: 13px; -fx-font-weight: 600;");

            String sbStatus = sb.getStatus() != null ? sb.getStatus() : "--";
            Label subStatus = new Label(sbStatus);
            subStatus.setStyle(switch (sbStatus) {
                case "PROCESSING" -> "-fx-text-fill: #38BDF8; -fx-font-size: 12px;";
                case "COMPLETED"  -> "-fx-text-fill: #22C55E; -fx-font-size: 12px;";
                case "CANCELLED"  -> "-fx-text-fill: #EF4444; -fx-font-size: 12px;";
                default           -> "-fx-text-fill: #8892A8; -fx-font-size: 12px;";
            });

            Label subCreated = new Label("Tạo lúc: "
                + DateUtils.formatDateTime(sb.getCreatedAt())
                + "  |  Bởi: " + (sb.getCreatedBy() != null ? sb.getCreatedBy() : "--"));
            subCreated.setStyle("-fx-text-fill: #8892A8; -fx-font-size: 11px;");

            card.getChildren().addAll(subHeader, subStatus, subCreated);

            List<SiteOrder> orders = siteOrderService.getBySubBatch(sb.getSubBatchId());
            if (!orders.isEmpty()) {
                TableView<SiteOrder> tblOrders = buildOrdersTable(orders);
                card.getChildren().add(tblOrders);
            } else {
                Label noOrders = new Label("  Chưa có đơn hàng nào.");
                noOrders.setStyle("-fx-text-fill: #4A5368; -fx-font-size: 11px;");
                card.getChildren().add(noOrders);
            }

            if ("PROCESSING".equals(sb.getStatus())) {
                Button btnContinue = new Button("▶ Tiếp tục xử lý sub-batch này");
                btnContinue.setStyle(
                    "-fx-background-color: rgba(79,110,247,0.15); -fx-text-fill: #4F6EF7; " +
                    "-fx-background-radius: 6px; -fx-cursor: hand; -fx-font-size: 12px;");
                final String sbId = sb.getSubBatchId();
                btnContinue.setOnAction(e -> goToSupplyDashboard(sb.getParentBatchId(), sbId));
                card.getChildren().add(btnContinue);
            }

            vboxSubBatches.getChildren().add(card);
        }
    }

    private TableView<SiteOrder> buildOrdersTable(List<SiteOrder> orders) {
        TableView<SiteOrder> tbl = new TableView<>();
        tbl.setStyle(
            "-fx-background-color: #0D0F14; -fx-border-color: #252A3A; " +
            "-fx-border-radius: 6px; -fx-background-radius: 6px;");
        tbl.setPrefHeight(Math.min(35 + orders.size() * 40.0, 200));

        TableColumn<SiteOrder, String> cId = new TableColumn<>("MÃ ĐƠN");
        cId.setPrefWidth(200);
        cId.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getSiteOrderId()));

        TableColumn<SiteOrder, String> cSite = new TableColumn<>("SITE");
        cSite.setPrefWidth(80);
        cSite.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getSiteCode()));

        TableColumn<SiteOrder, String> cMeans = new TableColumn<>("VẬN CHUYỂN");
        cMeans.setPrefWidth(110);
        cMeans.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getDeliveryMeans()));

        TableColumn<SiteOrder, String> cArrival = new TableColumn<>("DỰ KIẾN VỀ");
        cArrival.setPrefWidth(110);
        cArrival.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getEstimatedArrival() != null
                ? DateUtils.formatDate(c.getValue().getEstimatedArrival()) : "--"));

        TableColumn<SiteOrder, String> cStatus = new TableColumn<>("TRẠNG THÁI");
        cStatus.setPrefWidth(130);
        cStatus.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getStatus()));

        TableColumn<SiteOrder, Void> cAction = new TableColumn<>("");
        cAction.setPrefWidth(80);
        cAction.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("Xem");
            {
                btn.setStyle(
                    "-fx-background-color: rgba(79,110,247,0.15); -fx-text-fill: #4F6EF7; " +
                    "-fx-background-radius: 6px; -fx-cursor: hand; -fx-font-size: 10px;");
                btn.setOnAction(e -> {
                    SiteOrder so = getTableView().getItems().get(getIndex());
                    goToOrderDetail(so.getSiteOrderId());
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });

        tbl.getColumns().addAll(cId, cSite, cMeans, cArrival, cStatus, cAction);
        tbl.setItems(FXCollections.observableArrayList(orders));
        return tbl;
    }

    private void goToSupplyDashboard(String parentBatchId, String subBatchId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/ood/OOD_SupplyDashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(
                getClass().getResource("/css/global.css").toExternalForm());
            com.importorder.uc4_create_request.OOD_SupplyDashboardController ctrl =
                loader.getController();
            ctrl.setBatchId(parentBatchId);
            ctrl.setSubBatchId(subBatchId);
            Stage stage = (Stage) lblUserName.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }

    private void goToOrderDetail(String siteOrderId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/ood/OOD_SiteOrderDetail.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(
                getClass().getResource("/css/global.css").toExternalForm());
            com.importorder.uc2_edit_order.OOD_SiteOrderDetailController ctrl =
                loader.getController();
            ctrl.setSiteOrderId(siteOrderId);
            Stage stage = (Stage) lblUserName.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }

    @FXML private void goBack()           { navigateTo("/fxml/ood/OOD_OrderRequestList.fxml"); }
    @FXML private void goDashboard()      { navigateTo("/fxml/ood/OOD_Dashboard.fxml"); }
    @FXML private void goOrderRequests()  { navigateTo("/fxml/ood/OOD_OrderRequestList.fxml"); }
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
            Stage stage = (Stage) lblUserName.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }
}
