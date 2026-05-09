package com.importorder.model;

import org.bson.types.ObjectId;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Sub-batch — một lần xử lý trong OrderRequest.
 * type = ORIGINAL  : lần xử lý đầu tiên
 * type = REPLACEMENT : lần xử lý thay thế cho đơn bị hủy
 */
public class SubBatch {

    private ObjectId id;
    private String subBatchId;       // e.g. "SUB-20250507-001"
    private String parentBatchId;    // batchId của OrderRequest cha
    private String type;             // ORIGINAL | REPLACEMENT
    private String status;           // PROCESSING | COMPLETED | CANCELLED
    private String replacingOrderId; // siteOrderId bị hủy (chỉ có khi REPLACEMENT)
    private List<OrderItem> items;   // mặt hàng cần xử lý trong lần này
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SubBatch() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getSubBatchId() { return subBatchId; }
    public void setSubBatchId(String subBatchId) { this.subBatchId = subBatchId; }

    public String getParentBatchId() { return parentBatchId; }
    public void setParentBatchId(String parentBatchId) { this.parentBatchId = parentBatchId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReplacingOrderId() { return replacingOrderId; }
    public void setReplacingOrderId(String replacingOrderId) {
        this.replacingOrderId = replacingOrderId;
    }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}