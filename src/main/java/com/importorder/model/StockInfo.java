package com.importorder.model;

import org.bson.types.ObjectId;
import java.time.LocalDateTime;

public class StockInfo {
    private ObjectId id;
    private String batchId;
    private String siteCode;
    private String itemCode;
    private int inStockQty;
    private String unit;
    private String updatedBy;        // username của SITE user cập nhật
    private LocalDateTime queriedAt;

    public StockInfo() {}

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public int getInStockQty() { return inStockQty; }
    public void setInStockQty(int inStockQty) { this.inStockQty = inStockQty; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getQueriedAt() { return queriedAt; }
    public void setQueriedAt(LocalDateTime queriedAt) { this.queriedAt = queriedAt; }
}