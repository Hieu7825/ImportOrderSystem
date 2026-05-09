package com.importorder.controller.sd;

import com.importorder.model.Merchandise;
import com.importorder.service.MerchandiseService;
import com.importorder.util.AlertUtils;
import com.importorder.util.AppException;
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

public class SD_MerchandiseListController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private TableView<Merchandise> tblMerchandise;
    @FXML private TableColumn<Merchandise, String> colCode;
    @FXML private TableColumn<Merchandise, String> colName;
    @FXML private TableColumn<Merchandise, String> colUnit;
    @FXML private TableColumn<Merchandise, String> colCategory;
    @FXML private TableColumn<Merchandise, String> colStatus;
    @FXML private TableColumn<Merchandise, Void> colActions;

    private final MerchandiseService merchService = new MerchandiseService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        setupTable();
        loadData();
    }

    private void setupTable() {
        colCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItemCode()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItemName()));
        colUnit.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDefaultUnit()));
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getCategory() != null ? c.getValue().getCategory() : "--"));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().isActive() ? "✅ Đang dùng" : "⛔ Đã ẩn"));

        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btnEdit = new Button("Sửa");
            final Button btnToggle = new Button();
            final HBox box = new HBox(8, btnEdit, btnToggle);

            {
                btnEdit.setStyle("-fx-background-color: rgba(79,110,247,0.15); " +
                    "-fx-text-fill: #4F6EF7; -fx-background-radius: 6px; -fx-cursor: hand;");
                btnEdit.setOnAction(e -> {
                    Merchandise m = getTableView().getItems().get(getIndex());
                    showEditDialog(m);
                });
                btnToggle.setOnAction(e -> {
                    Merchandise m = getTableView().getItems().get(getIndex());
                    handleToggle(m);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Merchandise m = getTableView().getItems().get(getIndex());
                if (m.isActive()) {
                    btnToggle.setText("Ẩn");
                    btnToggle.setStyle("-fx-background-color: rgba(239,68,68,0.15); " +
                        "-fx-text-fill: #EF4444; -fx-background-radius: 6px; -fx-cursor: hand;");
                } else {
                    btnToggle.setText("Hiện");
                    btnToggle.setStyle("-fx-background-color: rgba(34,197,94,0.15); " +
                        "-fx-text-fill: #22C55E; -fx-background-radius: 6px; -fx-cursor: hand;");
                }
                setGraphic(box);
            }
        });
    }

    private void loadData() {
        tblMerchandise.setItems(
            FXCollections.observableArrayList(merchService.getAll()));
    }

    @FXML
    private void handleCreate() {
        showCreateDialog();
    }

    private void showCreateDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Thêm mặt hàng mới");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField txtCode = new TextField(); txtCode.setPromptText("Mã hàng *");
        TextField txtName = new TextField(); txtName.setPromptText("Tên hàng *");
        TextField txtUnit = new TextField(); txtUnit.setPromptText("Đơn vị *");
        TextField txtCategory = new TextField(); txtCategory.setPromptText("Danh mục");
        TextField txtDesc = new TextField(); txtDesc.setPromptText("Mô tả");

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10,
            new Label("Mã hàng:"), txtCode,
            new Label("Tên hàng:"), txtName,
            new Label("Đơn vị:"), txtUnit,
            new Label("Danh mục:"), txtCategory,
            new Label("Mô tả:"), txtDesc);
        content.setStyle("-fx-padding: 10;");
        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    merchService.create(txtCode.getText(), txtName.getText(),
                        txtUnit.getText(), txtCategory.getText(), txtDesc.getText());
                    loadData();
                } catch (AppException e) {
                    AlertUtils.showError("Lỗi", e.getMessage());
                }
            }
        });
    }

    private void showEditDialog(Merchandise m) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Sửa mặt hàng: " + m.getItemCode());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField txtName = new TextField(m.getItemName());
        TextField txtUnit = new TextField(m.getDefaultUnit());
        TextField txtCategory = new TextField(m.getCategory());
        TextField txtDesc = new TextField(m.getDescription());

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10,
            new Label("Tên hàng:"), txtName,
            new Label("Đơn vị:"), txtUnit,
            new Label("Danh mục:"), txtCategory,
            new Label("Mô tả:"), txtDesc);
        content.setStyle("-fx-padding: 10;");
        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    merchService.update(m.getItemCode(), txtName.getText(),
                        txtUnit.getText(), txtCategory.getText(), txtDesc.getText());
                    loadData();
                } catch (AppException e) {
                    AlertUtils.showError("Lỗi", e.getMessage());
                }
            }
        });
    }

    private void handleToggle(Merchandise m) {
        try {
            if (m.isActive()) {
                try {
                    merchService.deactivate(m.getItemCode());
                } catch (AppException e) {
                    if (e.getMessage().startsWith("WARN:")) {
                        String msg = e.getMessage().substring(5);
                        if (AlertUtils.showConfirm("Cảnh báo", msg)) {
                            merchService.deactivateForced(m.getItemCode());
                        } else return;
                    } else throw e;
                }
            } else {
                merchService.activate(m.getItemCode());
            }
            loadData();
        } catch (AppException e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }

    @FXML private void goDashboard() { navigateTo("/fxml/sd/SD_Dashboard.fxml"); }
    @FXML private void goOrderList() { navigateTo("/fxml/sd/SD_OrderRequestList.fxml"); }
    @FXML private void handleLogout() { SessionManager.logout(); navigateTo("/fxml/Login.fxml"); }

    private void navigateTo(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(
                getClass().getResource("/css/global.css").toExternalForm());
            Stage stage = (Stage) tblMerchandise.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }
}