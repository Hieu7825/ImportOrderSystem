package com.importorder.repository;

import com.importorder.config.MongoConfig;
import com.importorder.model.OrderItem;
import com.importorder.model.SubBatch;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SubBatchRepository {

    private final MongoCollection<Document> collection;

    public SubBatchRepository() {
        this.collection = MongoConfig.getDatabase().getCollection("sub_batches");
    }

    public void save(SubBatch sb) {
        Document doc = toDocument(sb);
        collection.insertOne(doc);
        sb.setId(doc.getObjectId("_id"));
    }

    public SubBatch findBySubBatchId(String subBatchId) {
        Document doc = collection.find(Filters.eq("subBatchId", subBatchId)).first();
        return doc != null ? toSubBatch(doc) : null;
    }

    public List<SubBatch> findByParentBatch(String parentBatchId) {
        List<SubBatch> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("parentBatchId", parentBatchId)))
            list.add(toSubBatch(doc));
        // Sắp xếp theo createdAt tăng dần
        list.sort((a, b) -> {
            if (a.getCreatedAt() == null) return -1;
            if (b.getCreatedAt() == null) return 1;
            return a.getCreatedAt().compareTo(b.getCreatedAt());
        });
        return list;
    }

    public List<SubBatch> findByStatus(String status) {
        List<SubBatch> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("status", status)))
            list.add(toSubBatch(doc));
        return list;
    }

    public void updateStatus(String subBatchId, String status) {
        collection.updateOne(
            Filters.eq("subBatchId", subBatchId),
            Updates.combine(
                Updates.set("status", status),
                Updates.set("updatedAt", LocalDateTime.now().toString())
            )
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Document toDocument(SubBatch sb) {
        return new Document()
            .append("subBatchId",       sb.getSubBatchId())
            .append("parentBatchId",    sb.getParentBatchId())
            .append("type",             sb.getType())
            .append("status",           sb.getStatus())
            .append("replacingOrderId", sb.getReplacingOrderId())
            .append("createdBy",        sb.getCreatedBy())
            .append("createdAt",        sb.getCreatedAt() != null
                ? sb.getCreatedAt().toString() : null)
            .append("updatedAt",        sb.getUpdatedAt() != null
                ? sb.getUpdatedAt().toString() : null)
            .append("items",            toItemDocuments(sb.getItems()));
    }

    private List<Document> toItemDocuments(List<OrderItem> items) {
        List<Document> docs = new ArrayList<>();
        if (items == null) return docs;
        for (OrderItem item : items) {
            docs.add(new Document()
                .append("itemCode",            item.getItemCode())
                .append("itemName",            item.getItemName())
                .append("quantityOrdered",     item.getQuantityOrdered())
                .append("unit",                item.getUnit())
                .append("desiredDeliveryDate", item.getDesiredDeliveryDate() != null
                    ? item.getDesiredDeliveryDate().toString() : null));
        }
        return docs;
    }

    private SubBatch toSubBatch(Document doc) {
        SubBatch sb = new SubBatch();
        sb.setId(doc.getObjectId("_id"));
        sb.setSubBatchId(doc.getString("subBatchId"));
        sb.setParentBatchId(doc.getString("parentBatchId"));
        sb.setType(doc.getString("type"));
        sb.setStatus(doc.getString("status"));
        sb.setReplacingOrderId(doc.getString("replacingOrderId"));
        sb.setCreatedBy(doc.getString("createdBy"));

        String createdAt = doc.getString("createdAt");
        if (createdAt != null) sb.setCreatedAt(LocalDateTime.parse(createdAt));
        String updatedAt = doc.getString("updatedAt");
        if (updatedAt != null) sb.setUpdatedAt(LocalDateTime.parse(updatedAt));

        List<Document> itemDocs = doc.getList("items", Document.class);
        List<OrderItem> items = new ArrayList<>();
        if (itemDocs != null) {
            for (Document d : itemDocs) {
                OrderItem item = new OrderItem();
                item.setItemCode(d.getString("itemCode"));
                item.setItemName(d.getString("itemName"));
                item.setQuantityOrdered(d.getInteger("quantityOrdered", 0));
                item.setUnit(d.getString("unit"));
                String date = d.getString("desiredDeliveryDate");
                if (date != null) item.setDesiredDeliveryDate(LocalDate.parse(date));
                items.add(item);
            }
        }
        sb.setItems(items);
        return sb;
    }
}