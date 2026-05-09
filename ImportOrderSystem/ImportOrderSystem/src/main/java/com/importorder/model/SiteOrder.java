package com.importorder.model;

import org.bson.types.ObjectId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class SiteOrder {

    private ObjectId id;
    private String siteOrderId;
    private String batchId;
    private String subBatchId;          // ← thêm mới: gắn với sub-batch nào
    private String siteCode;
    private String deliveryMeans;       // ship | air
    private LocalDate estimatedArrival;
    private String status;              // SENT|CONFIRMED|PARTIALLY_RECEIVED|RECEIVED|DISCREPANCY|CANCELLED
    private boolean confirmedBySite;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelRequestedAt;
    private String cancelRequestReason;
    private String cancelApprovedBy;
    private LocalDateTime createdAt;
    private List<FinalOrder> items;

    public SiteOrder() {}

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getSiteOrderId() { return siteOrderId; }
    public void setSiteOrderId(String siteOrderId) { this.siteOrderId = siteOrderId; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getSubBatchId() { return subBatchId; }
    public void setSubBatchId(String subBatchId) { this.subBatchId = subBatchId; }

    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }

    public String getDeliveryMeans() { return deliveryMeans; }
    public void setDeliveryMeans(String deliveryMeans) { this.deliveryMeans = deliveryMeans; }

    public LocalDate getEstimatedArrival() { return estimatedArrival; }
    public void setEstimatedArrival(LocalDate estimatedArrival) {
        this.estimatedArrival = estimatedArrival;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isConfirmedBySite() { return confirmedBySite; }
    public void setConfirmedBySite(boolean confirmedBySite) {
        this.confirmedBySite = confirmedBySite;
    }

    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public LocalDateTime getCancelRequestedAt() { return cancelRequestedAt; }
    public void setCancelRequestedAt(LocalDateTime cancelRequestedAt) {
        this.cancelRequestedAt = cancelRequestedAt;
    }

    public String getCancelRequestReason() { return cancelRequestReason; }
    public void setCancelRequestReason(String cancelRequestReason) {
        this.cancelRequestReason = cancelRequestReason;
    }

    public String getCancelApprovedBy() { return cancelApprovedBy; }
    public void setCancelApprovedBy(String cancelApprovedBy) {
        this.cancelApprovedBy = cancelApprovedBy;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<FinalOrder> getItems() { return items; }
    public void setItems(List<FinalOrder> items) { this.items = items; }
}