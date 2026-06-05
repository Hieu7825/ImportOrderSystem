package com.importorder.controller.sd;

import com.importorder.model.Merchandise;
import com.importorder.model.OrderItem;
import com.importorder.service.MerchandiseService;
import com.importorder.service.OrderRequestService;
import com.importorder.util.AlertUtils;
import com.importorder.util.AppException;
import com.importorder.util.SessionManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class SD_OrderRequestFormController implements Initializable {

    @FXML private Label lblTitle;
    @FXML private Label lblUserName;
    @FXML private Label lblError;
    @FXML private Label lblItemCount;
    @FXML private ComboBox<String> cmbItemCode;
    @FXML private TextField txtQty;
    @FXML private TextField txtUnit;
    @FXML private DatePicker dpDate;
    @FXML private Button btnSave;
    @FXML private TableView<OrderItem> tblItems;
    @FXML private TableColumn<OrderItem, Number> colNo;
    @FXML private TableColumn<OrderItem, String> colCode;
    @FXML private TableColumn<OrderItem, String> colName;
    @FXML private TableColumn<OrderItem, Number> colQty;
    @FXML private TableColumn<OrderItem, String> colUnit;
    @FXML private TableColumn<OrderItem, String> colDate;
    @FXML private TableColumn<OrderItem, Void> colDel;

    private final OrderRequestService orderService = new OrderRequestService();
    private final MerchandiseService merchService = new MerchandiseService();
    private final ObservableList<OrderItem> itemList = FXCollections.observableArrayList();
    private List<Merchandise> allMerchandise;
    private String editingBatchId = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (lblUserName != null)
            lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        loadMerchandise();
        setupTable();
        tblItems.setItems(itemList);
        updateCount();
    }

    private void loadMerchandise() {
        allMerchandise = merchService.getAllActive();
        cmbItemCode.setItems(FXCollections.observableArrayList(
            allMerchandise.stream().map(Merchandise::getItemCode).toList()));
    }

    @FXML
    private void handleItemSelected() {
        String code = cmbItemCode.getValue();
        if (code == null) return;
        allMerchandise.stream()
            .filter(m -> m.getItemCode().equals(code))
            .findFirst()
            .ifPresent(m -> txtUnit.setText(m.getDefaultUnit()));
    }

    private void setupTable() {
        colNo.setCellValueFactory(c ->
            new SimpleIntegerProperty(tblItems.getItems().indexOf(c.getValue()) + 1));
        colCode.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getItemCode()));
        colName.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getItemName()));
        colQty.setCellValueFactory(c ->
            new SimpleIntegerProperty(c.getValue().getQuantityOrdered()));
        colUnit.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getUnit()));
        colDate.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getDesiredDeliveryDate() != null
                ? c.getValue().getDesiredDeliveryDate().toString() : "--"));

        colDel.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("✕");
            {
                btn.setStyle("-fx-background-color: rgba(239,68,68,0.15); " +
                    "-fx-text-fill: #EF4444; -fx-background-radius: 6px; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    OrderItem item = getTableView().getItems().get(getIndex());
                    itemList.remove(item);
                    updateCount();
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    @FXML
    private void handleAddItem() {
        hideError();
        String code = cmbItemCode.getValue();
        if (code == null || code.isBlank()) {
            showError("Vui lòng chọn mã hàng.");
            return;
        }
        int qty;
        try {
            qty = Integer.parseInt(txtQty.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Số lượng phải là số nguyên dương.");
            return;
        }
        String unit = txtUnit.getText().trim();
        if (unit.isBlank()) {
            showError("Vui lòng nhập đơn vị.");
            return;
        }
        LocalDate date = dpDate.getValue();
        if (date == null || !date.isAfter(LocalDate.now())) {
            showError("Ngày nhận phải sau hôm nay ít nhất 1 ngày.");
            return;
        }
        // Kiểm tra trùng
        boolean dup = itemList.stream().anyMatch(i -> i.getItemCode().equals(code));
        if (dup) {
            showError("Mã hàng " + code + " đã có trong danh sách.");
            return;
        }

        String itemName = allMerchandise.stream()
            .filter(m -> m.getItemCode().equals(code))
            .map(Merchandise::getItemName)
            .findFirst().orElse(code);

        OrderItem item = new OrderItem(code, itemName, qty, unit, date);
        itemList.add(item);
        updateCount();

        // Reset inputs
        cmbItemCode.setValue(null);
        txtQty.clear();
        txtUnit.clear();
        dpDate.setValue(null);
    }

    @FXML
    private void handleSave() {
        hideError();
        if (itemList.isEmpty()) {
            showError("Danh sách mặt hàng không được để trống.");
            return;
        }
        try {
            if (editingBatchId == null) {
                orderService.createRequest(new ArrayList<>(itemList));
                AlertUtils.showInfo("Thành công", "Tạo yêu cầu nhập hàng thành công!");
            } else {
                orderService.updateRequest(editingBatchId, new ArrayList<>(itemList));
                AlertUtils.showInfo("Thành công", "Cập nhật yêu cầu thành công!");
            }
            navigateTo("/fxml/sd/SD_OrderRequestList.fxml");
        } catch (AppException e) {
            showError(e.getMessage());
        }
    }

    public void setEditMode(String batchId) {
        this.editingBatchId = batchId;
        if (lblTitle != null) lblTitle.setText("Sửa yêu cầu nhập hàng");
        if (btnSave != null) btnSave.setText("Lưu thay đổi");

        var req = orderService.getByBatchId(batchId);
        if (req != null && req.getItems() != null) {
            itemList.addAll(req.getItems());
            updateCount();
        }
    }

    private void updateCount() {
        if (lblItemCount != null)
            lblItemCount.setText("Tổng: " + itemList.size() + " mặt hàng");
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    @FXML private void handleCancel() { navigateTo("/fxml/sd/SD_OrderRequestList.fxml"); }
    @FXML private void handleLogout() { SessionManager.logout(); navigateTo("/fxml/Login.fxml"); }

    private void navigateTo(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(
                getClass().getResource("/css/global.css").toExternalForm());
            Stage stage = (Stage) btnSave.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }
}