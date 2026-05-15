package com.importorder.controller.sd;

import com.importorder.model.OrderItem;
import com.importorder.model.OrderRequest;
import com.importorder.service.OrderRequestService;
import com.importorder.util.AlertUtils;
import com.importorder.util.DateUtils;
import com.importorder.util.SessionManager;
import javafx.beans.property.SimpleIntegerProperty;
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
import java.util.ResourceBundle;

public class SD_OrderRequestDetailController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private Label lblBatchId;
    @FXML private Label lblStatus;
    @FXML private Label lblCreatedBy;
    @FXML private Label lblCreatedAt;
    @FXML private VBox vboxCancelInfo;
    @FXML private Label lblCancelledBy;
    @FXML private Label lblCancelledAt;
    @FXML private Label lblCancelReason;
    @FXML private Button btnEdit;
    @FXML private Button btnCancel;
    @FXML private TableView<OrderItem> tblItems;
    @FXML private TableColumn<OrderItem, Number> colNo;
    @FXML private TableColumn<OrderItem, String> colCode;
    @FXML private TableColumn<OrderItem, String> colName;
    @FXML private TableColumn<OrderItem, Number> colQty;
    @FXML private TableColumn<OrderItem, String> colUnit;
    @FXML private TableColumn<OrderItem, String> colDate;

    private final OrderRequestService orderService = new OrderRequestService();
    private String batchId;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        setupTable();
    }

    private void setupTable() {
        colNo.setCellValueFactory(c ->
            new SimpleIntegerProperty(tblItems.getItems().indexOf(c.getValue()) + 1));
        colCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItemCode()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItemName()));
        colQty.setCellValueFactory(c ->
            new SimpleIntegerProperty(c.getValue().getQuantityOrdered()));
        colUnit.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUnit()));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getDesiredDeliveryDate() != null
                ? DateUtils.formatDate(c.getValue().getDesiredDeliveryDate()) : "--"));
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
        loadData();
    }

    private void loadData() {
        OrderRequest req = orderService.getByBatchId(batchId);
        if (req == null) {
            AlertUtils.showError("Lỗi", "Không tìm thấy batch: " + batchId);
            return;
        }

        lblBatchId.setText(req.getBatchId());
        lblCreatedBy.setText(req.getCreatedBy());
        lblCreatedAt.setText(DateUtils.formatDateTime(req.getCreatedAt()));

        // Status badge
        String status = req.getStatus();
        lblStatus.setText(status);
        lblStatus.setStyle(switch (status) {
            case "PENDING"    -> "-fx-text-fill: #F59E0B; -fx-font-size: 13px; -fx-font-weight: 600;";
            case "PROCESSING" -> "-fx-text-fill: #38BDF8; -fx-font-size: 13px; -fx-font-weight: 600;";
            case "COMPLETED"  -> "-fx-text-fill: #22C55E; -fx-font-size: 13px; -fx-font-weight: 600;";
            case "CANCELLED"  -> "-fx-text-fill: #EF4444; -fx-font-size: 13px; -fx-font-weight: 600;";
            default           -> "-fx-text-fill: #8892A8; -fx-font-size: 13px;";
        });

        // Cancel info
        if ("CANCELLED".equals(status)) {
            vboxCancelInfo.setVisible(true);
            vboxCancelInfo.setManaged(true);
            lblCancelledBy.setText("Hủy bởi: " + (req.getCancelledBy() != null ? req.getCancelledBy() : "--"));
            lblCancelledAt.setText("Lúc: " + DateUtils.formatDateTime(req.getCancelledAt()));
            lblCancelReason.setText("Lý do: " + (req.getCancelReason() != null ? req.getCancelReason() : "--"));
        }

        // Buttons — chỉ hiện khi PENDING và là người tạo
        boolean isPending = "PENDING".equals(status);
        boolean isOwner = SessionManager.getUsername().equals(req.getCreatedBy());
        btnEdit.setVisible(isPending && isOwner);
        btnEdit.setManaged(isPending && isOwner);
        btnCancel.setVisible(isPending && isOwner);
        btnCancel.setManaged(isPending && isOwner);

        // Items
        if (req.getItems() != null)
            tblItems.setItems(FXCollections.observableArrayList(req.getItems()));
    }

    @FXML
    private void handleEdit() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/sd/SD_OrderRequestForm.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(
                getClass().getResource("/css/global.css").toExternalForm());
            SD_OrderRequestFormController ctrl = loader.getController();
            ctrl.setEditMode(batchId);
            Stage stage = (Stage) lblBatchId.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        String reason = AlertUtils.showConfirmWithReason(
            "Hủy yêu cầu", "Xác nhận hủy batch " + batchId + "?");
        if (reason != null) {
            try {
                orderService.cancelRequest(batchId, reason);
                loadData(); // reload để cập nhật UI
            } catch (Exception e) {
                AlertUtils.showError("Lỗi", e.getMessage());
            }
        }
    }

    @FXML private void goBack() { navigateTo("/fxml/sd/SD_OrderRequestList.fxml"); }
    @FXML private void goDashboard() { navigateTo("/fxml/sd/SD_Dashboard.fxml"); }
    @FXML private void goMerchandise() { navigateTo("/fxml/sd/SD_MerchandiseList.fxml"); }
    @FXML private void handleLogout() { SessionManager.logout(); navigateTo("/fxml/Login.fxml"); }

    private void navigateTo(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(
                getClass().getResource("/css/global.css").toExternalForm());
            Stage stage = (Stage) lblBatchId.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }
}