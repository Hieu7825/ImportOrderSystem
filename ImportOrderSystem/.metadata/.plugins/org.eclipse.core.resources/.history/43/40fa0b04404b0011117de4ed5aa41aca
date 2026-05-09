package com.importorder.controller.ood;

import com.importorder.model.SiteInfo;
import com.importorder.service.SiteService;
import com.importorder.util.AlertUtils;
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

public class OOD_SiteListController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private TableView<SiteInfo> tblSites;
    @FXML private TableColumn<SiteInfo, String> colCode;
    @FXML private TableColumn<SiteInfo, String> colName;
    @FXML private TableColumn<SiteInfo, String> colCountry;
    @FXML private TableColumn<SiteInfo, String> colShip;
    @FXML private TableColumn<SiteInfo, String> colAir;
    @FXML private TableColumn<SiteInfo, String> colCatalog;
    @FXML private TableColumn<SiteInfo, String> colStatus;
    @FXML private TableColumn<SiteInfo, Void> colActions;

    private final SiteService siteService = new SiteService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        setupTable();
        loadData();
    }

    private void setupTable() {
        colCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSiteCode()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSiteName()));
        colCountry.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCountry()));
        colShip.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getShipDays() + " ngày"));
        colAir.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAirDays() + " ngày"));
        colCatalog.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getCatalogItems() != null
                ? c.getValue().getCatalogItems().size() + " mặt hàng" : "0"));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
            "ACTIVE".equals(c.getValue().getStatus()) ? "✅ Hoạt động" : "⛔ Ngừng"));
        colActions.setCellFactory(col -> new TableCell<>() {
            final Button btnView = new Button("Chi tiết");
            final Button btnDeactivate = new Button("Ngừng LĐ");
            final HBox box = new HBox(6, btnView, btnDeactivate);
            {
                btnView.setStyle("-fx-background-color: rgba(79,110,247,0.15); -fx-text-fill: #4F6EF7; -fx-background-radius: 6px; -fx-cursor: hand; -fx-font-size: 11px;");
                btnView.setOnAction(e -> {
                    SiteInfo s = getTableView().getItems().get(getIndex());
                    try {
                        FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/fxml/ood/OOD_SiteDetail.fxml"));
                        Scene scene = new Scene(loader.load(), 1280, 720);
                        scene.getStylesheets().add(
                            getClass().getResource("/css/global.css").toExternalForm());
                        OOD_SiteDetailController ctrl = loader.getController();
                        ctrl.setSiteCode(s.getSiteCode());
                        Stage stage = (Stage) tblSites.getScene().getWindow();
                        stage.setScene(scene);
                    } catch (Exception ex) {
                        AlertUtils.showError("Lỗi", ex.getMessage());
                    }
                });
                btnDeactivate.setStyle("-fx-background-color: rgba(239,68,68,0.15); -fx-text-fill: #EF4444; -fx-background-radius: 6px; -fx-cursor: hand; -fx-font-size: 11px;");
                btnDeactivate.setOnAction(e -> {
                    SiteInfo s = getTableView().getItems().get(getIndex());
                    handleDeactivate(s);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                SiteInfo s = getTableView().getItems().get(getIndex());
                btnDeactivate.setVisible("ACTIVE".equals(s.getStatus()));
                btnDeactivate.setManaged("ACTIVE".equals(s.getStatus()));
                setGraphic(box);
            }
        });
    }

    private void loadData() {
        tblSites.setItems(FXCollections.observableArrayList(siteService.getAllSites()));
    }

    @FXML
    private void handleAddSite() {
        showAddSiteDialog();
    }

    private void showAddSiteDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Thêm Site mới");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField txtCode = new TextField(); txtCode.setPromptText("Mã site *");
        TextField txtName = new TextField(); txtName.setPromptText("Tên site *");
        TextField txtCountry = new TextField(); txtCountry.setPromptText("Quốc gia");
        TextField txtEmail = new TextField(); txtEmail.setPromptText("Email liên hệ");
        TextField txtShip = new TextField(); txtShip.setPromptText("Số ngày tàu *");
        TextField txtAir = new TextField(); txtAir.setPromptText("Số ngày hàng không *");

        var content = new javafx.scene.layout.VBox(8,
            new Label("Mã site:"), txtCode,
            new Label("Tên site:"), txtName,
            new Label("Quốc gia:"), txtCountry,
            new Label("Email:"), txtEmail,
            new Label("Số ngày tàu:"), txtShip,
            new Label("Số ngày HK:"), txtAir);
        content.setStyle("-fx-padding: 10;");
        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    int ship = Integer.parseInt(txtShip.getText().trim());
                    int air = Integer.parseInt(txtAir.getText().trim());
                    siteService.addSite(txtCode.getText(), txtName.getText(),
                        txtCountry.getText(), txtEmail.getText(), ship, air, "");
                    loadData();
                } catch (NumberFormatException e) {
                    AlertUtils.showError("Lỗi", "Số ngày phải là số nguyên.");
                } catch (Exception e) {
                    AlertUtils.showError("Lỗi", e.getMessage());
                }
            }
        });
    }

    private void handleDeactivate(SiteInfo s) {
        String reason = AlertUtils.showConfirmWithReason("Ngừng liên doanh",
            "Xác nhận ngừng liên doanh với site " + s.getSiteCode() + "?");
        if (reason != null) {
            try {
                try {
                    siteService.deactivate(s.getSiteCode());
                } catch (com.importorder.util.AppException ex) {
                    if (ex.getMessage().startsWith("WARN:")) {
                        if (AlertUtils.showConfirm("Cảnh báo", ex.getMessage().substring(5))) {
                            siteService.deactivateForced(s.getSiteCode());
                        } else return;
                    } else throw ex;
                }
                loadData();
            } catch (Exception e) {
                AlertUtils.showError("Lỗi", e.getMessage());
            }
        }
    }

    @FXML private void goDashboard() { navigateTo("/fxml/ood/OOD_Dashboard.fxml"); }
    @FXML private void goOrderRequests() { navigateTo("/fxml/ood/OOD_OrderRequestList.fxml"); }
    @FXML private void goSiteOrders() { navigateTo("/fxml/ood/OOD_SiteOrderList.fxml"); }
    @FXML private void goCancelRequests() { navigateTo("/fxml/ood/OOD_CancelRequestList.fxml"); }
    @FXML private void handleLogout() { SessionManager.logout(); navigateTo("/fxml/Login.fxml"); }

    private void navigateTo(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/css/global.css").toExternalForm());
            Stage stage = (Stage) tblSites.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }
}