package com.importorder.service;

import com.importorder.model.*;
import com.importorder.repository.*;
import com.importorder.util.AppException;
import com.importorder.util.SessionManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WarehouseService {

    private final SiteOrderRepository siteOrderRepo;
    private final FinalOrderRepository finalOrderRepo;
    private final DiscrepancyRepository discrepancyRepo;

    public WarehouseService() {
        this.siteOrderRepo = new SiteOrderRepository();
        this.finalOrderRepo = new FinalOrderRepository();
        this.discrepancyRepo = new DiscrepancyRepository();
    }

    public WarehouseService(SiteOrderRepository siteOrderRepo,
                            FinalOrderRepository finalOrderRepo,
                            DiscrepancyRepository discrepancyRepo) {
        this.siteOrderRepo = siteOrderRepo;
        this.finalOrderRepo = finalOrderRepo;
        this.discrepancyRepo = discrepancyRepo;
    }

    public List<SiteOrder> getPendingInspection() {
        return siteOrderRepo.findPendingInspection();
    }

    public List<FinalOrder> getItemsForInspection(String siteOrderId) {
        return finalOrderRepo.findBySiteOrder(siteOrderId);
    }

    // inspectionData: itemCode -> { actualQty, actualUnit, itemCodeReceived, description }
    public void submitInspection(String siteOrderId,
                                  Map<String, Map<String, Object>> inspectionData) {
        SiteOrder so = siteOrderRepo.findBySiteOrderId(siteOrderId);
        if (so == null) throw new AppException("Không tìm thấy đơn: " + siteOrderId);

        List<FinalOrder> orders = finalOrderRepo.findBySiteOrder(siteOrderId);
        List<WarehouseDiscrepancy> discrepancies = new ArrayList<>();

        int totalCount = 0;

        for (FinalOrder fo : orders) {
            Map<String, Object> data = inspectionData.get(fo.getItemCode());
            if (data == null) continue; // dòng chưa nhập → bỏ qua (partial)

            totalCount++;
            int actualQty = (Integer) data.getOrDefault("actualQty", 0);
            String actualUnit = (String) data.getOrDefault("actualUnit", fo.getUnit());
            String itemCodeReceived = (String) data.getOrDefault("itemCodeReceived", fo.getItemCode());
            String description = (String) data.getOrDefault("description", "");

            boolean hasError = false;
            String errorCode = null;

            // Validate: actualQty = 0 phải có description
            if (actualQty == 0 && (description == null || description.isBlank()))
                throw new AppException("Mặt hàng " + fo.getItemCode()
                    + ": số lượng thực nhận = 0, vui lòng điền ghi chú.");

            // Phát hiện sai lệch
            if (!itemCodeReceived.equals(fo.getItemCode())) {
                errorCode = "ERR-ITEM";
                hasError = true;
            } else if (!actualUnit.equals(fo.getUnit())) {
                errorCode = "ERR-UNIT";
                hasError = true;
            } else if (actualQty == 0) {
                errorCode = "ERR-MISSING";
                hasError = true;
            } else if (actualQty != fo.getQuantityOrdered()) {
                errorCode = "ERR-QTY";
                hasError = true;
            }

            if (hasError) {
                WarehouseDiscrepancy d = new WarehouseDiscrepancy();
                d.setFinalOrderId(fo.getId());
                d.setSiteOrderId(siteOrderId);
                d.setItemCode(fo.getItemCode());
                d.setExpectedQty(fo.getQuantityOrdered());
                d.setActualQty(actualQty);
                d.setExpectedUnit(fo.getUnit());
                d.setActualUnit(actualUnit);
                d.setItemCodeReceived(itemCodeReceived);
                d.setErrorCode(errorCode);
                d.setDescription(description);
                d.setRecordedAt(LocalDateTime.now());
                d.setRecordedBy(SessionManager.getUsername());
                d.setSyncedToWMS(false);
                discrepancies.add(d);

                finalOrderRepo.updateStatus(siteOrderId, fo.getItemCode(), "DISCREPANCY");
            } else {
                finalOrderRepo.updateStatus(siteOrderId, fo.getItemCode(), "RECEIVED");
            
            }
        }

        // Lưu discrepancies
        discrepancyRepo.saveAll(discrepancies);

        // Cập nhật SiteOrder status
        String newStatus;
        if (totalCount == 0) {
            newStatus = so.getStatus(); // không thay đổi nếu không nhập gì
        } else if (totalCount < orders.size()) {
            newStatus = "PARTIALLY_RECEIVED";
        } else if (!discrepancies.isEmpty()) {
            newStatus = "DISCREPANCY";
        } else {
            newStatus = "RECEIVED";
        }
        siteOrderRepo.updateStatus(siteOrderId, newStatus);

        // Sync sang WMS (tự động)
        syncToWMS(discrepancies);
    }

    private void syncToWMS(List<WarehouseDiscrepancy> discrepancies) {
        // Trong thực tế: gọi API WMS hoặc ghi vào queue
        // Hiện tại: đánh dấu synced = true
        for (WarehouseDiscrepancy d : discrepancies) {
            try {
                // TODO: gọi WMS API thực tế ở đây
                discrepancyRepo.markSynced(d.getId());
            } catch (Exception e) {
                // Không throw — chỉ log, UI sẽ hiện cảnh báo "chưa sync"
                System.err.println("WMS sync failed for discrepancy: " + d.getId());
            }
        }
    }

    public List<WarehouseDiscrepancy> getDiscrepanciesBySiteOrder(String siteOrderId) {
        return discrepancyRepo.findBySiteOrder(siteOrderId);
    }
}