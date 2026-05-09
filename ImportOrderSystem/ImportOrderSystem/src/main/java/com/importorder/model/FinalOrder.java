package com.importorder.model;

import org.bson.types.ObjectId;
import java.time.LocalDateTime;

public class FinalOrder {
    private ObjectId id;
    private String siteOrderId;
    private String batchId;
    private String siteCode;
    private String itemCode;
    private String itemName;
    private int quantityOrdered;
    private String unit;
    private String deliveryMeans;
    private String status;
    private LocalDateTime createdAt;

    public FinalOrder() {}

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }
    public String getSiteOrderId() { return siteOrderId; }
    public void setSiteOrderId(String siteOrderId) { this.siteOrderId = siteOrderId; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public int getQuantityOrdered() { return quantityOrdered; }
    public void setQuantityOrdered(int quantityOrdered) { this.quantityOrdered = quantityOrdered; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getDeliveryMeans() { return deliveryMeans; }
    public void setDeliveryMeans(String deliveryMeans) { this.deliveryMeans = deliveryMeans; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}