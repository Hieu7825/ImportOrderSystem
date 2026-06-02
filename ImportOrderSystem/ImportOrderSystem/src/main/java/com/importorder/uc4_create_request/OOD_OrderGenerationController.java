package com.importorder.uc4_create_request;

import com.importorder.service.OrderOptimizationService;
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
import java.util.*;
import java.util.stream.Collectors;

public class OOD_OrderGenerationController implements Initializable {

    @FXML private Label  lblUserName;
    @FXML private Label  lblBatchId;
    @FXML private Label  lblSubBatchInfo;
    @FXML private Label  lblPlanIndex;
    @FXML private Label  lblPlanLabel;
    @FXML private VBox   vboxWarning;
    @FXML private Label  lblInsufficientItems;
    @FXML private TableView<Map<String, Object>> tblSiteOrders;
    @FXML private TableColumn<Map<String, Object>, String> colSite;
    @FXML private TableColumn<Map<String, Object>, String> colMeans;
    @FXML private TableColumn<Map<String, Object>, String> colArrival;
    @FXML private TableColumn<Map<String, Object>, String> colItems;
    @FXML private Button btnConfirm;

    private final OrderOptimizationService optimService = new OrderOptimizationService();
    private String       batchId;
    private String       subBatchId;
    private List<String> prioritized = new ArrayList<>();
    private List<String> avoided     = new ArrayList<>();
    private List<Map<String, Object>> allPlans = new ArrayList<>();
    private int currentPlanIndex = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (lblUserName != null)
            lblUserName.setText(SessionManager.getCurrentUser().getFullName());
        setupTable();
    }

    /** Gọi từ SupplyDashboard (ORIGINAL) */
    public void setBatchData(String batchId, List<String> prioritized, List<String> avoided) {
        setBatchData(batchId, null, prioritized, avoided);
    }

    /** Gọi từ SupplyDashboard (REPLACEMENT) */
    public void setBatchData(String batchId, String subBatchId,
                              List<String> prioritized, List<String> avoided) {
        this.batchId     = batchId;
        this.subBatchId  = subBatchId;
        this.prioritized = prioritized;
        this.avoided     = avoided;

        if (lblBatchId != null) lblBatchId.setText("Batch: " + batchId);
        if (lblSubBatchInfo != null) {
            if (subBatchId != null) {
                lblSubBatchInfo.setText("🔄 Phương án thay thế — " + subBatchId);
                lblSubBatchInfo.setVisible(true);
                lblSubBatchInfo.setManaged(true);
            } else {
                lblSubBatchInfo.setVisible(false);
                lblSubBatchInfo.setManaged(false);
            }
        }
        calculatePlans();
    }

    private void calculatePlans() {
        try {
            if (subBatchId != null) {
                allPlans = optimService.generateAllPlansForSubBatch(
                    batchId, subBatchId, prioritized, avoided);
            } else {
                allPlans = optimService.generateAllPlans(batchId, prioritized, avoided);
            }

            if (allPlans.isEmpty()) {
                AlertUtils.showError("Lỗi",
                    "Không thể tính phương án nào. Kiểm tra lại tồn kho và deadline.");
                return;
            }
            currentPlanIndex = 0;
            displayPlan(currentPlanIndex);
            btnConfirm.setDisable(false);
        } catch (Exception e) {
            AlertUtils.showError("Lỗi tính phương án", e.getMessage());
        }
    }

    private void setupTable() {
        colSite.setCellValueFactory(c ->
            new SimpleStringProperty((String) c.getValue().get("siteCode")));
        colMeans.setCellValueFactory(c ->
            new SimpleStringProperty((String) c.getValue().get("means")));
        colArrival.setCellValueFactory(c -> {
            Object arrival = c.getValue().get("estimatedArrival");
            return new SimpleStringProperty(arrival != null ? arrival.toString() : "--");
        });
        colItems.setCellValueFactory(c -> {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items =
                (List<Map<String, Object>>) c.getValue().get("items");
            if (items == null) return new SimpleStringProperty("--");
            String text = items.stream()
                .map(i -> i.get("itemCode") + ": " + i.get("qty") + " " + i.get("unit"))
                .collect(Collectors.joining(" | "));
            return new SimpleStringProperty(text);
        });
    }

    @SuppressWarnings("unchecked")
    private void displayPlan(int index) {
        Map<String, Object> plan  = allPlans.get(index);
        int                 total = allPlans.size();

        lblPlanIndex.setText("Phương án " + (index + 1) + " / " + total
            + "   |   Số site: " + plan.get("totalSites"));

        String label = (String) plan.get("label");
        if (lblPlanLabel != null)
            lblPlanLabel.setText(label != null ? "💡 " + label : "");

        List<Map<String, Object>> siteOrders =
            (List<Map<String, Object>>) plan.get("siteOrders");
        tblSiteOrders.setItems(FXCollections.observableArrayList(
            siteOrders != null ? siteOrders : new ArrayList<>()));

        // Edge Case B3: cảnh báo insufficient items
        List<Map<String, Object>> insufficient =
            (List<Map<String, Object>>) plan.get("insufficientItems");
        if (insufficient != null && !insufficient.isEmpty()) {
            vboxWarning.setVisible(true);
            vboxWarning.setManaged(true);
            String text = insufficient.stream()
                .map(i -> {
                    String line = "• " + i.get("itemCode") + ": " + i.get("reason");
                    if ("ONLY_AVOIDED_SITES_AVAILABLE".equals(i.get("reason"))) {
                        line += " (Site bị đánh ✕ là nguồn duy nhất: "
                            + i.get("avoidedSites") + ")";
                    }
                    return line;
                })
                .collect(Collectors.joining("\n"));
            lblInsufficientItems.setText(text);
        } else {
            vboxWarning.setVisible(false);
            vboxWarning.setManaged(false);
        }
    }

    @FXML
    private void handlePrevPlan() {
        if (currentPlanIndex > 0) {
            currentPlanIndex--;
            displayPlan(currentPlanIndex);
        }
    }

    @FXML
    private void handleNextPlan() {
        if (currentPlanIndex < allPlans.size() - 1) {
            currentPlanIndex++;
            displayPlan(currentPlanIndex);
        }
    }

    @FXML
    private void handleConfirm() {
        if (allPlans.isEmpty()) return;
        Map<String, Object> selectedPlan = allPlans.get(currentPlanIndex);
        String planName = (String) selectedPlan.getOrDefault(
            "label", "Phương án " + (currentPlanIndex + 1));

        boolean confirm = AlertUtils.showConfirm("Xác nhận sinh đơn",
            "Xác nhận sinh đơn hàng theo:\n\"" + planName + "\"?");
        if (!confirm) return;

        try {
            if (subBatchId != null) {
                optimService.confirmPlanForSubBatch(batchId, subBatchId, selectedPlan);
            } else {
                optimService.confirmPlan(batchId, selectedPlan);
            }
            AlertUtils.showInfo("Thành công",
                "Đã sinh đơn hàng thành công!\n"
                + (subBatchId != null
                    ? "Sub-batch " + subBatchId + " → COMPLETED."
                    : "Batch " + batchId + " → COMPLETED."));
            navigateTo("/fxml/ood/OOD_SiteOrderList.fxml");
        } catch (Exception e) {
            AlertUtils.showError("Lỗi sinh đơn", e.getMessage());
        }
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/ood/OOD_SupplyDashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(
                getClass().getResource("/css/global.css").toExternalForm());
            OOD_SupplyDashboardController ctrl = loader.getController();
            ctrl.setBatchId(batchId);
            if (subBatchId != null) ctrl.setSubBatchId(subBatchId);
            Stage stage = (Stage) lblUserName.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }

    @FXML private void handleLogout() {
        SessionManager.logout();
        navigateTo("/fxml/Login.fxml");
    }

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
