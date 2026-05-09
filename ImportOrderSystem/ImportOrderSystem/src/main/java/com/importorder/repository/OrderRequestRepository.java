package com.importorder.repository;

import com.importorder.config.MongoConfig;
import com.importorder.model.OrderItem;
import com.importorder.model.OrderRequest;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderRequestRepository {

    private final MongoCollection<Document> collection;

    public OrderRequestRepository() {
        this.collection = MongoConfig.getDatabase().getCollection("order_requests");
    }

    public void save(OrderRequest req) {
        Document doc = toDocument(req);
        collection.insertOne(doc);
        req.setId(doc.getObjectId("_id"));
    }

    public OrderRequest findByBatchId(String batchId) {
        Document doc = collection.find(Filters.eq("batchId", batchId)).first();
        return doc != null ? toOrderRequest(doc) : null;
    }

    public List<OrderRequest> findAll() {
        List<OrderRequest> list = new ArrayList<>();
        for (Document doc : collection.find()) list.add(toOrderRequest(doc));
        return list;
    }

    public List<OrderRequest> findByStatus(String status) {
        List<OrderRequest> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("status", status)))
            list.add(toOrderRequest(doc));
        return list;
    }

    public List<OrderRequest> findByCreatedBy(String username) {
        List<OrderRequest> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("createdBy", username)))
            list.add(toOrderRequest(doc));
        return list;
    }

    public void updateStatus(String batchId, String status) {
        collection.updateOne(
            Filters.eq("batchId", batchId),
            Updates.combine(
                Updates.set("status", status),
                Updates.set("updatedAt", LocalDateTime.now().toString())
            )
        );
    }

    public void cancel(String batchId, String cancelledBy, String reason) {
        collection.updateOne(
            Filters.eq("batchId", batchId),
            Updates.combine(
                Updates.set("status", "CANCELLED"),
                Updates.set("cancelledBy", cancelledBy),
                Updates.set("cancelledAt", LocalDateTime.now().toString()),
                Updates.set("cancelReason", reason),
                Updates.set("updatedAt", LocalDateTime.now().toString())
            )
        );
    }

    public void update(String batchId, List<OrderItem> newItems) {
        collection.updateOne(
            Filters.eq("batchId", batchId),
            Updates.combine(
                Updates.set("items", toItemDocuments(newItems)),
                Updates.set("updatedAt", LocalDateTime.now().toString())
            )
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Document toDocument(OrderRequest req) {
        return new Document()
            .append("batchId",     req.getBatchId())
            .append("createdBy",   req.getCreatedBy())
            .append("createdAt",   req.getCreatedAt().toString())
            .append("updatedAt",   req.getUpdatedAt().toString())
            .append("status",      req.getStatus())
            .append("cancelledBy", req.getCancelledBy())
            .append("cancelledAt", req.getCancelledAt() != null ? req.getCancelledAt().toString() : null)
            .append("cancelReason",req.getCancelReason())
            .append("items",       toItemDocuments(req.getItems()));
    }

    private List<Document> toItemDocuments(List<OrderItem> items) {
        List<Document> docs = new ArrayList<>();
        if (items == null) return docs;
        for (OrderItem item : items) {
            docs.add(new Document()
                .append("itemCode",            item.getItemCode())
                .append("itemName",            item.getItemName())   // ← thêm
                .append("quantityOrdered",     item.getQuantityOrdered())
                .append("unit",                item.getUnit())
                .append("desiredDeliveryDate", item.getDesiredDeliveryDate() != null
                    ? item.getDesiredDeliveryDate().toString() : null));
        }
        return docs;
    }

    private OrderRequest toOrderRequest(Document doc) {
        OrderRequest req = new OrderRequest();
        req.setId(doc.getObjectId("_id"));
        req.setBatchId(doc.getString("batchId"));
        req.setCreatedBy(doc.getString("createdBy"));
        req.setStatus(doc.getString("status"));
        req.setCancelledBy(doc.getString("cancelledBy"));
        req.setCancelReason(doc.getString("cancelReason"));

        String createdAt  = doc.getString("createdAt");
        if (createdAt  != null) req.setCreatedAt(LocalDateTime.parse(createdAt));
        String updatedAt  = doc.getString("updatedAt");
        if (updatedAt  != null) req.setUpdatedAt(LocalDateTime.parse(updatedAt));
        String cancelledAt = doc.getString("cancelledAt");
        if (cancelledAt != null) req.setCancelledAt(LocalDateTime.parse(cancelledAt));

        List<Document> itemDocs = doc.getList("items", Document.class);
        List<OrderItem> items = new ArrayList<>();
        if (itemDocs != null) {
            for (Document d : itemDocs) {
                OrderItem item = new OrderItem();
                item.setItemCode(d.getString("itemCode"));
                item.setItemName(d.getString("itemName"));   // ← thêm
                item.setQuantityOrdered(d.getInteger("quantityOrdered", 0));
                item.setUnit(d.getString("unit"));
                String date = d.getString("desiredDeliveryDate");
                if (date != null) item.setDesiredDeliveryDate(LocalDate.parse(date));
                items.add(item);
            }
        }
        req.setItems(items);
        return req;
    }
}