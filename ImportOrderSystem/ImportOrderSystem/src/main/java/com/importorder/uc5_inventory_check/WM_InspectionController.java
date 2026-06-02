package com.importorder.uc5_inventory_check;

import com.importorder.model.FinalOrder;
import com.importorder.model.SiteOrder;
import com.importorder.service.SiteOrderService;
import com.importorder.service.WarehouseService;
import com.importorder.util.AlertUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class WM_InspectionController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private ListView<SiteOrder> lstSiteOrders;
    @FXML private Label lblSelectedOrder;
    @FXML private Label lblCancelledWarning;
    @FXML private TableView<FinalOrder> tblInspection;
    @FXML private TableColumn<FinalOrder, String> colCode;
    @FXML private TableColumn<FinalOrder, String> colExpected;
    @FXML private TableColumn<FinalOrder, String> colUnit;
    @FXML private TableColumn<FinalOrder, String> colActual;
    @FXML private TableColumn<FinalOrder, String> colActualCode;
    @FXML private TableColumn<FinalOrder, String> colNote;
    @FXML private Button btnConfirm;

    private final SiteOrderService siteOrderService = new SiteOrderService();
    private final WarehouseService warehouseService  = new WarehouseService();

    private final Map<String, Map<String, Object>> inspectionData = new HashMap<>();
    private SiteOrder selectedOrder;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        if (lblCancelledWarning != null) {
            lblCancelledWarning.setVisible(false);
            lblCancelledWarning.setManaged(false);
        }
        setupList();
        setupTable();
    }

    private void setupList() {
        // Chỉ load đơn SENT / PARTIALLY_RECEIVED — KHÔNG bao gồm CANCELLED (D3)
        var pending = warehouseService.getPendingInspection();
        lstSiteOrders.setItems(FXCollections.observableArrayList(pending));
        lstSiteOrders.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SiteOrder so, boolean empty) {
                super.updateItem(so, empty);
                if (empty || so == null) { setText(null); return; }
                setText(so.getSiteOrderId() + "\n"
                    + so.getSiteCode() + " | " + so.getDeliveryMeans()
                    + " | " + so.getStatus());
                setStyle("-fx-text-fill: #E8ECF5; -fx-font-size: 12px; -fx-padding: 8;");
            }
        });
        lstSiteOrders.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, sel) -> {
                if (sel != null) loadInspectionTable(sel);
            });
    }

    private void setupTable() {
        colCode.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getItemCode()));
        colExpected.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getQuantityOrdered())));
        colUnit.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getUnit()));

        colActual.setCellValueFactory(c -> {
            Map<String, Object> d = inspectionData.computeIfAbsent(
                c.getValue().getItemCode(), k -> new HashMap<>());
            Object v = d.get("actualQty");
            return new SimpleStringProperty(v != null ? v.toString() : "");
        });
        colActual.setCellFactory(col -> new EditableCell("actualQty"));

        colActualCode.setCellValueFactory(c -> {
            Map<String, Object> d = inspectionData.computeIfAbsent(
                c.getValue().getItemCode(), k -> new HashMap<>());
            Object v = d.getOrDefault("itemCodeReceived", c.getValue().getItemCode());
            return new SimpleStringProperty(v.toString());
        });
        colActualCode.setCellFactory(col -> new EditableCell("itemCodeReceived"));

        colNote.setCellValueFactory(c -> {
            Map<String, Object> d = inspectionData.computeIfAbsent(
                c.getValue().getItemCode(), k -> new HashMap<>());
            Object v = d.getOrDefault("description", "");
            return new SimpleStringProperty(v.toString());
        });
        colNote.setCellFactory(col -> new EditableCell("description"));
    }

    private void loadInspectionTable(SiteOrder so) {
        // Edge Case D3: đơn đã bị hủy → hiển thị cảnh báo, không cho xác nhận
        if ("CANCELLED".equals(so.getStatus())) {
            selectedOrder = null;
            tblInspection.getItems().clear();
            inspectionData.clear();
            btnConfirm.setDisable(true);
            lblSelectedOrder.setText("⚠ Đơn " + so.getSiteOrderId() + " đã bị HỦY");

            if (lblCancelledWarning != null) {
                lblCancelledWarning.setText(
                    "Đơn hàng này đã bị hủy trước khi hàng về. Nếu hàng vẫn đến kho, "
                    + "vui lòng báo cáo thủ công cho OOD/SITE để phối hợp xử lý trả hàng.");
                lblCancelledWarning.setVisible(true);
                lblCancelledWarning.setManaged(true);
            }
            return;
        }

        if (lblCancelledWarning != null) {
            lblCancelledWarning.setVisible(false);
            lblCancelledWarning.setManaged(false);
        }

        selectedOrder = so;
        inspectionData.clear();
        lblSelectedOrder.setText("Đơn: " + so.getSiteOrderId()
            + " | Site: " + so.getSiteCode()
            + " | " + so.getDeliveryMeans());

        List<FinalOrder> items = siteOrderService.getItemsOfSiteOrder(so.getSiteOrderId());
        for (FinalOrder fo : items) {
            Map<String, Object> d = new HashMap<>();
            d.put("actualQty",        fo.getQuantityOrdered());
            d.put("actualUnit",       fo.getUnit());
            d.put("itemCodeReceived", fo.getItemCode());
            d.put("description",      "");
            inspectionData.put(fo.getItemCode(), d);
        }

        tblInspection.setItems(FXCollections.observableArrayList(items));
        btnConfirm.setDisable(false);
    }

    @FXML
    private void handleConfirm() {
        if (selectedOrder == null) return;

        // Edge Case D4: kiểm tra qty = 0 phải có ghi chú
        for (Map.Entry<String, Map<String, Object>> entry : inspectionData.entrySet()) {
            Object qtyObj = entry.getValue().get("actualQty");
            int qty = 0;
            if (qtyObj instanceof Integer) qty = (Integer) qtyObj;
            else if (qtyObj instanceof String s) {
                try { qty = Integer.parseInt(s.trim()); } catch (Exception ignored) {}
            }
            String desc = (String) entry.getValue().getOrDefault("description", "");
            if (qty == 0 && (desc == null || desc.isBlank())) {
                AlertUtils.showError("Thiếu thông tin",
                    "Mặt hàng '" + entry.getKey()
                    + "': số lượng thực nhận = 0 nhưng chưa có ghi chú.\n"
                    + "Vui lòng điền lý do vào cột Ghi chú.");
                return;
            }
        }

        try {
            Map<String, Map<String, Object>> converted = new HashMap<>();
            for (var entry : inspectionData.entrySet()) {
                Map<String, Object> d = new HashMap<>(entry.getValue());
                Object qty = d.get("actualQty");
                if (qty instanceof String s) {
                    try { d.put("actualQty", Integer.parseInt(s.trim())); }
                    catch (NumberFormatException e) { d.put("actualQty", 0); }
                }
                converted.put(entry.getKey(), d);
            }

            warehouseService.submitInspection(selectedOrder.getSiteOrderId(), converted);
            AlertUtils.showInfo("Thành công", "Kiểm hàng hoàn tất!");

            setupList();
            tblInspection.getItems().clear();
            lblSelectedOrder.setText("Chọn đơn hàng từ danh sách bên trái");
            btnConfirm.setDisable(true);
            selectedOrder = null;
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }

    // ── Inner class: Editable cell ─────────────────────────────────────────────

    private class EditableCell extends TableCell<FinalOrder, String> {
        private final TextField textField = new TextField();
        private final String    fieldKey;

        EditableCell(String fieldKey) {
            this.fieldKey = fieldKey;
            textField.setStyle(
                "-fx-background-color: #1C2030; -fx-text-fill: #E8ECF5; " +
                "-fx-border-color: #252A3A; -fx-border-radius: 4px;");
            textField.textProperty().addListener((obs, old, val) -> {
                FinalOrder fo = getTableRow().getItem();
                if (fo != null) {
                    inspectionData
                        .computeIfAbsent(fo.getItemCode(), k -> new HashMap<>())
                        .put(fieldKey, val);
                }
            });
            setGraphic(textField);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow().getItem() == null) {
                setGraphic(null);
            } else {
                textField.setText(item != null ? item : "");
                setGraphic(textField);
            }
        }
    }

    @FXML private void goDashboard()  { navigateTo("/fxml/wm/WM_Dashboard.fxml"); }
    @FXML private void goOrderList()  { navigateTo("/fxml/wm/WM_OrderList.fxml"); }
    @FXML private void handleLogout() { SessionManager.logout(); navigateTo("/fxml/Login.fxml"); }

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
