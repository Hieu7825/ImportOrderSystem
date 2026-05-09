package com.importorder.repository;

import com.importorder.config.MongoConfig;
import com.importorder.model.SiteOrder;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SiteOrderRepository {

    private final MongoCollection<Document> collection;

    public SiteOrderRepository() {
        this.collection = MongoConfig.getDatabase().getCollection("site_orders");
    }

    public void save(SiteOrder so) {
        Document doc = toDocument(so);
        collection.insertOne(doc);
        so.setId(doc.getObjectId("_id"));
    }

    public SiteOrder findBySiteOrderId(String siteOrderId) {
        Document doc = collection.find(Filters.eq("siteOrderId", siteOrderId)).first();
        return doc != null ? toSiteOrder(doc) : null;
    }

    public List<SiteOrder> findByBatch(String batchId) {
        List<SiteOrder> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("batchId", batchId)))
            list.add(toSiteOrder(doc));
        return list;
    }

    public List<SiteOrder> findBySite(String siteCode) {
        List<SiteOrder> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("siteCode", siteCode)))
            list.add(toSiteOrder(doc));
        return list;
    }

    public List<SiteOrder> findByStatus(String status) {
        List<SiteOrder> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("status", status)))
            list.add(toSiteOrder(doc));
        return list;
    }

    public List<SiteOrder> findPendingInspection() {
        List<SiteOrder> list = new ArrayList<>();
        for (Document doc : collection.find(
                Filters.in("status", "SENT", "PARTIALLY_RECEIVED")))
            list.add(toSiteOrder(doc));
        return list;
    }

    // Lấy đơn Site cần xác nhận (SENT, chưa confirm)
    public List<SiteOrder> findUnconfirmedBySite(String siteCode) {
        List<SiteOrder> list = new ArrayList<>();
        for (Document doc : collection.find(
                Filters.and(
                    Filters.eq("siteCode", siteCode),
                    Filters.eq("status", "SENT"),
                    Filters.eq("confirmedBySite", false)
                )))
            list.add(toSiteOrder(doc));
        return list;
    }

    // Lấy đơn có yêu cầu hủy chờ OOD duyệt
    public List<SiteOrder> findCancelRequests() {
        List<SiteOrder> list = new ArrayList<>();
        for (Document doc : collection.find(
                Filters.exists("cancelRequestedAt")))
            list.add(toSiteOrder(doc));
        return list;
    }

    public List<SiteOrder> findAll() {
        List<SiteOrder> list = new ArrayList<>();
        for (Document doc : collection.find()) list.add(toSiteOrder(doc));
        return list;
    }

    public void updateStatus(String siteOrderId, String status) {
        collection.updateOne(
            Filters.eq("siteOrderId", siteOrderId),
            Updates.set("status", status)
        );
    }

    public void confirmBySite(String siteOrderId) {
        collection.updateOne(
            Filters.eq("siteOrderId", siteOrderId),
            Updates.combine(
                Updates.set("confirmedBySite", true),
                Updates.set("confirmedAt", LocalDateTime.now().toString()),
                Updates.set("status", "CONFIRMED")
            )
        );
    }

    public void requestCancel(String siteOrderId, String reason) {
        collection.updateOne(
            Filters.eq("siteOrderId", siteOrderId),
            Updates.combine(
                Updates.set("cancelRequestedAt", LocalDateTime.now().toString()),
                Updates.set("cancelRequestReason", reason)
            )
        );
    }

    public void approveCancel(String siteOrderId, String approvedBy) {
        collection.updateOne(
            Filters.eq("siteOrderId", siteOrderId),
            Updates.combine(
                Updates.set("status", "CANCELLED"),
                Updates.set("cancelApprovedBy", approvedBy)
            )
        );
    }

    public void deleteByBatch(String batchId) {
        collection.deleteMany(Filters.eq("batchId", batchId));
    }

    private Document toDocument(SiteOrder so) {
        return new Document()
            .append("siteOrderId", so.getSiteOrderId())
            .append("batchId", so.getBatchId())
            .append("siteCode", so.getSiteCode())
            .append("deliveryMeans", so.getDeliveryMeans())
            .append("estimatedArrival", so.getEstimatedArrival() != null
                ? so.getEstimatedArrival().toString() : null)
            .append("status", so.getStatus())
            .append("confirmedBySite", false)
            .append("createdAt", LocalDateTime.now().toString());
    }

    private SiteOrder toSiteOrder(Document doc) {
        SiteOrder so = new SiteOrder();
        so.setId(doc.getObjectId("_id"));
        so.setSiteOrderId(doc.getString("siteOrderId"));
        so.setBatchId(doc.getString("batchId"));
        so.setSiteCode(doc.getString("siteCode"));
        so.setDeliveryMeans(doc.getString("deliveryMeans"));
        so.setStatus(doc.getString("status"));
        so.setConfirmedBySite(Boolean.TRUE.equals(doc.getBoolean("confirmedBySite")));
        so.setCancelRequestReason(doc.getString("cancelRequestReason"));
        so.setCancelApprovedBy(doc.getString("cancelApprovedBy"));
        String arrival = doc.getString("estimatedArrival");
        if (arrival != null) so.setEstimatedArrival(LocalDate.parse(arrival));
        String confirmedAt = doc.getString("confirmedAt");
        if (confirmedAt != null) so.setConfirmedAt(LocalDateTime.parse(confirmedAt));
        String cancelReqAt = doc.getString("cancelRequestedAt");
        if (cancelReqAt != null) so.setCancelRequestedAt(LocalDateTime.parse(cancelReqAt));
        String createdAt = doc.getString("createdAt");
        if (createdAt != null) so.setCreatedAt(LocalDateTime.parse(createdAt));
        return so;
    }
    public void rejectCancelRequest(String siteOrderId) {
        collection.updateOne(
            Filters.eq("siteOrderId", siteOrderId),
            Updates.combine(
                Updates.unset("cancelRequestedAt"),
                Updates.unset("cancelRequestReason")
            )
        );
    }
}