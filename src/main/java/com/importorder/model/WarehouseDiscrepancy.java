package com.importorder.model;

import org.bson.types.ObjectId;
import java.time.LocalDateTime;

public class WarehouseDiscrepancy {
    private ObjectId id;
    private ObjectId finalOrderId;
    private String siteOrderId;
    private String itemCode;
    private int expectedQty;
    private int actualQty;
    private String expectedUnit;
    private String actualUnit;
    private String itemCodeReceived; // nếu nhận nhầm mã hàng
    private String errorCode; // ERR-QTY | ERR-ITEM | ERR-UNIT | ERR-MISSING
    private String description;
    private LocalDateTime recordedAt;
    private String recordedBy;

    public WarehouseDiscrepancy() {}

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }
    public ObjectId getFinalOrderId() { return finalOrderId; }
    public void setFinalOrderId(ObjectId finalOrderId) { this.finalOrderId = finalOrderId; }
    public String getSiteOrderId() { return siteOrderId; }
    public void setSiteOrderId(String siteOrderId) { this.siteOrderId = siteOrderId; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public int getExpectedQty() { return expectedQty; }
    public void setExpectedQty(int expectedQty) { this.expectedQty = expectedQty; }
    public int getActualQty() { return actualQty; }
    public void setActualQty(int actualQty) { this.actualQty = actualQty; }
    public String getExpectedUnit() { return expectedUnit; }
    public void setExpectedUnit(String expectedUnit) { this.expectedUnit = expectedUnit; }
    public String getActualUnit() { return actualUnit; }
    public void setActualUnit(String actualUnit) { this.actualUnit = actualUnit; }
    public String getItemCodeReceived() { return itemCodeReceived; }
    public void setItemCodeReceived(String itemCodeReceived) { this.itemCodeReceived = itemCodeReceived; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    private boolean syncedToWMS;
    private LocalDateTime syncedAt;
    public boolean isSyncedToWMS() { return syncedToWMS; }
    public void setSyncedToWMS(boolean syncedToWMS) { this.syncedToWMS = syncedToWMS; }
    public LocalDateTime getSyncedAt() { return syncedAt; }
    public void setSyncedAt(LocalDateTime syncedAt) { this.syncedAt = syncedAt; }
}