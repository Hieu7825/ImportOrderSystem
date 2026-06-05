package com.importorder.uc1_confirm_import;

import com.importorder.model.SiteOrder;
import com.importorder.service.SiteOrderService;
import com.importorder.util.AlertUtils;
import com.importorder.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class WM_ConfirmReceivedController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private TableView<SiteOrder> tblOrders;
    @FXML private TableColumn<SiteOrder, String> colOrderId;
    @FXML private TableColumn<SiteOrder, String> colBatchId;
    @FXML private TableColumn<SiteOrder, String> colSiteCode;
    @FXML private TableColumn<SiteOrder, String> colMeans;
    @FXML private TableColumn<SiteOrder, String> colDate;
    @FXML private TableColumn<SiteOrder, String> colStatus;
    @FXML private TableColumn<SiteOrder, Void> colAction;

    private final SiteOrderService siteOrderService = new SiteOrderService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        setupTable();
        loadData();
    }

    private void setupTable() {
        colOrderId.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getSiteOrderId()));
        colBatchId.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getBatchId() != null ? c.getValue().getBatchId() : "-"));
        colSiteCode.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getSiteCode()));
        colMeans.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getDeliveryMeans()));
        colDate.setCellValueFactory(c -> {
            LocalDateTime dt = c.getValue().getCreatedAt();
            String date = dt != null ? dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-";
            return new SimpleStringProperty(date);
        });
        colStatus.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getStatus()));

        // Action button
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnConfirm = new Button("Xác nhận");

            {
                btnConfirm.setStyle(
                    "-fx-background-color: #10B981; -fx-text-fill: #fff; " +
                    "-fx-padding: 6 12; -fx-font-size: 11px; -fx-cursor: hand;");
                btnConfirm.setOnAction(e -> {
                    int index = getIndex();
                    if (index >= 0 && index < getTableView().getItems().size()) {
                        SiteOrder order = getTableView().getItems().get(index);
                        confirmOrder(order);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(btnConfirm);
                    hbox.setAlignment(Pos.CENTER);
                    setGraphic(hbox);
                }
            }
        });
    }

    private void loadData() {
        try {
            // Lấy danh sách đơn có status = SENT
            List<SiteOrder> orders = siteOrderService.getByStatus("SENT");
            tblOrders.setItems(FXCollections.observableArrayList(orders));
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", "Không thể tải danh sách: " + e.getMessage());
        }
    }

    private void confirmOrder(SiteOrder order) {
        // Hiển thị dialog xác nhận
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận");
        alert.setHeaderText("Xác nhận đơn hàng nhập kho");
        alert.setContentText("Bạn xác nhận đơn " + order.getSiteOrderId() + " đã nhập kho?\n\n" +
            "Site: " + order.getSiteCode() + "\n" +
            "Vận chuyển: " + order.getDeliveryMeans());

        if (alert.showAndWait().orElse(null) == ButtonType.OK) {
            try {
                // Cập nhật status SENT → RECEIVED_PENDING_INSPECTION (chưa kiểm kê)
                siteOrderService.updateOrderStatus(order.getSiteOrderId(), "RECEIVED_PENDING_INSPECTION");
                AlertUtils.showInfo("Thành công", "Đơn hàng đã được xác nhận nhập kho!");
                
                // Reload danh sách
                loadData();

            } catch (Exception e) {
                AlertUtils.showError("Lỗi", e.getMessage());
            }
        }
    }

    @FXML private void goDashboard()   { navigateTo("/fxml/wm/WM_Dashboard.fxml"); }
    @FXML private void goConfirmReceived() { navigateTo("/fxml/wm/WM_ConfirmReceived.fxml"); }
    @FXML private void goInspection()   { navigateTo("/fxml/wm/WM_Inspection.fxml"); }
    @FXML private void goOrderList()    { navigateTo("/fxml/wm/WM_OrderList.fxml"); }
    @FXML private void handleLogout()   { SessionManager.logout(); navigateTo("/fxml/Login.fxml"); }

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
