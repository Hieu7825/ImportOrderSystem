package com.importorder.repository;

import com.importorder.config.MongoConfig;
import com.importorder.model.WarehouseDiscrepancy;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DiscrepancyRepository {

    private final MongoCollection<Document> collection;

    public DiscrepancyRepository() {
        this.collection = MongoConfig.getDatabase().getCollection("warehouse_discrepancies");
    }

    public void save(WarehouseDiscrepancy d) {
        Document doc = toDocument(d);
        collection.insertOne(doc);
        d.setId(doc.getObjectId("_id"));
    }

    public void saveAll(List<WarehouseDiscrepancy> list) {
        for (WarehouseDiscrepancy d : list) save(d);
    }

    public List<WarehouseDiscrepancy> findBySiteOrder(String siteOrderId) {
        List<WarehouseDiscrepancy> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("siteOrderId", siteOrderId)))
            list.add(toDiscrepancy(doc));
        return list;
    }

    public List<WarehouseDiscrepancy> findUnsynced() {
        List<WarehouseDiscrepancy> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("syncedToWMS", false)))
            list.add(toDiscrepancy(doc));
        return list;
    }

    public void markSynced(ObjectId id) {
        collection.updateOne(
            Filters.eq("_id", id),
            Updates.combine(
                Updates.set("syncedToWMS", true),
                Updates.set("syncedAt", LocalDateTime.now().toString())
            )
        );
    }

    private Document toDocument(WarehouseDiscrepancy d) {
        return new Document()
            .append("finalOrderId", d.getFinalOrderId())
            .append("siteOrderId", d.getSiteOrderId())
            .append("itemCode", d.getItemCode())
            .append("expectedQty", d.getExpectedQty())
            .append("actualQty", d.getActualQty())
            .append("expectedUnit", d.getExpectedUnit())
            .append("actualUnit", d.getActualUnit())
            .append("itemCodeReceived", d.getItemCodeReceived())
            .append("errorCode", d.getErrorCode())
            .append("description", d.getDescription())
            .append("recordedAt", LocalDateTime.now().toString())
            .append("recordedBy", d.getRecordedBy())
            .append("syncedToWMS", false);
    }

    private WarehouseDiscrepancy toDiscrepancy(Document doc) {
        WarehouseDiscrepancy d = new WarehouseDiscrepancy();
        d.setId(doc.getObjectId("_id"));
        d.setFinalOrderId(doc.getObjectId("finalOrderId"));
        d.setSiteOrderId(doc.getString("siteOrderId"));
        d.setItemCode(doc.getString("itemCode"));
        d.setExpectedQty(doc.getInteger("expectedQty", 0));
        d.setActualQty(doc.getInteger("actualQty", 0));
        d.setExpectedUnit(doc.getString("expectedUnit"));
        d.setActualUnit(doc.getString("actualUnit"));
        d.setItemCodeReceived(doc.getString("itemCodeReceived"));
        d.setErrorCode(doc.getString("errorCode"));
        d.setDescription(doc.getString("description"));
        d.setRecordedBy(doc.getString("recordedBy"));
        String recordedAt = doc.getString("recordedAt");
        if (recordedAt != null) d.setRecordedAt(LocalDateTime.parse(recordedAt));
        String syncedAt = doc.getString("syncedAt");
        if (syncedAt != null) d.setSyncedAt(LocalDateTime.parse(syncedAt));
        return d;
    }
}