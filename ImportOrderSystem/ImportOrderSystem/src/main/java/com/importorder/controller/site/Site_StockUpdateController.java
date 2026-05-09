package com.importorder.controller.site;

import com.importorder.model.StockInfo;
import com.importorder.repository.StockRepository;
import com.importorder.service.MerchandiseService;
import com.importorder.service.OrderRequestService;
import com.importorder.service.SiteService;
import com.importorder.util.AlertUtils;
import com.importorder.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;

import java.net.URL;
import java.util.*;

public class Site_StockUpdateController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private Label lblSiteCode;
    @FXML private ComboBox<String> cmbBatch;
    @FXML private TableView<StockInfo> tblStock;
    @FXML private TableColumn<StockInfo, String> colCode;
    @FXML private TableColumn<StockInfo, String> colName;
    @FXML private TableColumn<StockInfo, String> colQty;
    @FXML private TableColumn<StockInfo, String> colUnit;
    @FXML private Button btnSave;

    private final OrderRequestService orderService = new OrderRequestService();
    private final SiteService siteService = new SiteService();
    private final MerchandiseService merchService = new MerchandiseService();
    private final StockRepository stockRepo = new StockRepository();
    private String siteCode;
    private final ObservableList<StockInfo> stockList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        siteCode = SessionManager.getSiteCode();
        lblSiteCode.setText("SITE: " + (siteCode != null ? siteCode : "--"));
        setupTable();
        loadBatches();
        tblStock.setItems(stockList);
    }

    private void loadBatches() {
    if (siteCode == null) return;
    var site = siteService.getByCode(siteCode);
    if (site == null) return;

    // Lấy TẤT CẢ batch PROCESSING (không filter theo user)
    com.importorder.repository.OrderRequestRepository repo =
        new com.importorder.repository.OrderRequestRepository();
    List<String> batchIds = repo.findByStatus("PROCESSING").stream()
        .filter(r -> r.getItems() != null && r.getItems().stream()
            .anyMatch(i -> site.getCatalogItems() != null
                && site.getCatalogItems().contains(i.getItemCode())))
        .map(com.importorder.model.OrderRequest::getBatchId)
        .toList();

    cmbBatch.setItems(FXCollections.observableArrayList(batchIds));
    if (!batchIds.isEmpty()) {
        cmbBatch.setValue(batchIds.get(0));
        handleBatchSelected();
    }
}
    private void setupTable() {
        colCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItemCode()));
        colName.setCellValueFactory(c -> {
            var m = merchService.getByCode(c.getValue().getItemCode());
            return new SimpleStringProperty(m != null ? m.getItemName() : c.getValue().getItemCode());
        });
        colUnit.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUnit()));

        // Editable qty column
        colQty.setCellValueFactory(c ->
            new SimpleStringProperty(String.valueOf(c.getValue().getInStockQty())));
        colQty.setCellFactory(TextFieldTableCell.forTableColumn());
        colQty.setEditable(true);
        colQty.setOnEditCommit(event -> {
            try {
                int qty = Integer.parseInt(event.getNewValue().trim());
                if (qty < 0) throw new NumberFormatException();
                event.getRowValue().setInStockQty(qty);
            } catch (NumberFormatException e) {
                AlertUtils.showError("Lỗi", "Số lượng phải là số nguyên không âm.");
                tblStock.refresh();
            }
        });
        tblStock.setEditable(true);
    }

    @FXML
    private void handleBatchSelected() {
        String batchId = cmbBatch.getValue();
        if (batchId == null || siteCode == null) return;

        var site = siteService.getByCode(siteCode);
        var req = orderService.getByBatchId(batchId);
        if (site == null || req == null || req.getItems() == null) return;

        stockList.clear();

        for (var item : req.getItems()) {
            if (site.getCatalogItems() == null
                || !site.getCatalogItems().contains(item.getItemCode())) continue;

            // Load existing stock hoặc tạo mới
            StockInfo existing = stockRepo.findByBatchSiteItem(
                batchId, siteCode, item.getItemCode());
            if (existing != null) {
                stockList.add(existing);
            } else {
                StockInfo s = new StockInfo();
                s.setBatchId(batchId);
                s.setSiteCode(siteCode);
                s.setItemCode(item.getItemCode());
                s.setInStockQty(0);
                s.setUnit(item.getUnit());
                s.setUpdatedBy(SessionManager.getUsername());
                stockList.add(s);
            }
        }

        btnSave.setDisable(stockList.isEmpty());
    }

    @FXML
    private void handleSave() {
        try {
            for (StockInfo s : stockList) {
                s.setUpdatedBy(SessionManager.getUsername());
                stockRepo.saveOrUpdate(s);
            }
            AlertUtils.showInfo("Thành công",
                "Đã cập nhật tồn kho cho " + stockList.size() + " mặt hàng!");
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }

    @FXML private void goDashboard() { navigateTo("/fxml/site/Site_Dashboard.fxml"); }
    @FXML private void goProfile() { navigateTo("/fxml/site/Site_Profile.fxml"); }
    @FXML private void goCatalog() { navigateTo("/fxml/site/Site_Catalog.fxml"); }
    @FXML private void goOrders() { navigateTo("/fxml/site/Site_OrderList.fxml"); }
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