package com.importorder.uc1_confirm_import;

import com.importorder.model.FinalOrder;
import com.importorder.model.SiteOrder;
import com.importorder.model.WarehouseDiscrepancy;
import com.importorder.repository.DiscrepancyRepository;
import com.importorder.service.SiteOrderService;
import com.importorder.util.AlertUtils;
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

public class WM_OrderDetailController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private Label lblSiteOrderId;
    @FXML private Label lblSite;
    @FXML private Label lblMeans;
    @FXML private Label lblStatus;
    @FXML private TableView<FinalOrder> tblItems;
    @FXML private TableColumn<FinalOrder, String> colCode;
    @FXML private TableColumn<FinalOrder, String> colName;
    @FXML private TableColumn<FinalOrder, String> colQty;
    @FXML private TableColumn<FinalOrder, String> colUnit;
    @FXML private TableColumn<FinalOrder, String> colItemStatus;
    @FXML private VBox vboxDiscrepancy;
    @FXML private TableView<WarehouseDiscrepancy> tblDiscrepancy;
    @FXML private TableColumn<WarehouseDiscrepancy, String> colDCode;
    @FXML private TableColumn<WarehouseDiscrepancy, String> colDExpected;
    @FXML private TableColumn<WarehouseDiscrepancy, String> colDActual;
    @FXML private TableColumn<WarehouseDiscrepancy, String> colDError;
    @FXML private TableColumn<WarehouseDiscrepancy, String> colDDesc;
    @FXML private TableColumn<WarehouseDiscrepancy, String> colDRecordedBy;

    private final SiteOrderService siteOrderService = new SiteOrderService();
    private final DiscrepancyRepository discrepancyRepo = new DiscrepancyRepository();
    private String siteOrderId;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        setupTables();
    }

    private void setupTables() {
        colCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItemCode()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getItemName() != null ? c.getValue().getItemName() : "--"));
        colQty.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getQuantityOrdered())));
        colUnit.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUnit()));
        colItemStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        colDCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItemCode()));
        colDExpected.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getExpectedQty())));
        colDActual.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getActualQty())));
        colDError.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getErrorCode()));
        colDDesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        colDRecordedBy.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getRecordedBy()));
    }

    public void setSiteOrderId(String siteOrderId) {
        this.siteOrderId = siteOrderId;
        loadData();
    }

    private void loadData() {
        SiteOrder so = siteOrderService.getAll().stream()
            .filter(s -> s.getSiteOrderId().equals(siteOrderId))
            .findFirst().orElse(null);
        if (so == null) return;

        lblSiteOrderId.setText(so.getSiteOrderId());
        lblSite.setText(so.getSiteCode());
        lblMeans.setText(so.getDeliveryMeans());

        String status = so.getStatus();
        lblStatus.setText(status);
        lblStatus.setStyle(switch (status) {
            case "RECEIVED"    -> "-fx-text-fill: #22C55E; -fx-font-weight: 600;";
            case "DISCREPANCY" -> "-fx-text-fill: #F59E0B; -fx-font-weight: 600;";
            case "CANCELLED"   -> "-fx-text-fill: #EF4444; -fx-font-weight: 600;";
            default            -> "-fx-text-fill: #38BDF8; -fx-font-weight: 600;";
        });

        List<FinalOrder> items = siteOrderService.getItemsOfSiteOrder(siteOrderId);
        tblItems.setItems(FXCollections.observableArrayList(items));

        List<WarehouseDiscrepancy> discrepancies = discrepancyRepo.findBySiteOrder(siteOrderId);
        if (!discrepancies.isEmpty()) {
            vboxDiscrepancy.setVisible(true);
            vboxDiscrepancy.setManaged(true);
            tblDiscrepancy.setItems(FXCollections.observableArrayList(discrepancies));
        }
    }

    @FXML private void goOrderList()  { navigateTo("/fxml/wm/WM_OrderList.fxml"); }
    @FXML private void goDashboard()  { navigateTo("/fxml/wm/WM_Dashboard.fxml"); }
    @FXML private void goInspection() { navigateTo("/fxml/wm/WM_Inspection.fxml"); }
    @FXML private void handleLogout() { SessionManager.logout(); navigateTo("/fxml/Login.fxml"); }

    private void navigateTo(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/css/global.css").toExternalForm());
            Stage stage = (Stage) lblUserName.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }
}
