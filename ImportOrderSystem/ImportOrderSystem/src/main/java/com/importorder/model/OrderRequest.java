package com.importorder.model;

import org.bson.types.ObjectId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OrderRequest {

    private ObjectId id;
    private String batchId;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status;           // PENDING | PROCESSING | COMPLETED | CANCELLED
    private String cancelledBy;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private List<OrderItem> items;

    // ── Thêm mới ──────────────────────────────────────────────────────────────
    /** Danh sách các lần xử lý (sub-batch) trong batch này */
    private List<SubBatch> subBatches = new ArrayList<>();

    public OrderRequest() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public List<SubBatch> getSubBatches() { return subBatches; }
    public void setSubBatches(List<SubBatch> subBatches) {
        this.subBatches = subBatches != null ? subBatches : new ArrayList<>();
    }

    // ── Computed ──────────────────────────────────────────────────────────────

    /**
     * Trạng thái hiển thị ra ngoài = status của sub-batch mới nhất.
     * Nếu chưa có sub-batch thì trả về status gốc của batch.
     */
    public String getDisplayStatus() {
        if (subBatches == null || subBatches.isEmpty()) return status;
        return subBatches.stream()
            .filter(sb -> sb.getCreatedAt() != null)
            .max(Comparator.comparing(SubBatch::getCreatedAt))
            .map(SubBatch::getStatus)
            .orElse(status);
    }

    /**
     * Lấy sub-batch mới nhất.
     */
    public SubBatch getLatestSubBatch() {
        if (subBatches == null || subBatches.isEmpty()) return null;
        return subBatches.stream()
            .filter(sb -> sb.getCreatedAt() != null)
            .max(Comparator.comparing(SubBatch::getCreatedAt))
            .orElse(null);
    }
}