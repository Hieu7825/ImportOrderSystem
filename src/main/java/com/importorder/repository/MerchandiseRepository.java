package com.importorder.repository;

import com.importorder.config.MongoConfig;
import com.importorder.model.Merchandise;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MerchandiseRepository {

    private final MongoCollection<Document> collection;

    public MerchandiseRepository() {
        this.collection = MongoConfig.getDatabase().getCollection("merchandise");
    }

    public void save(Merchandise m) {
        Document doc = toDocument(m);
        collection.insertOne(doc);
        m.setId(doc.getObjectId("_id"));
    }

    public List<Merchandise> findAllActive() {
        List<Merchandise> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("isActive", true)))
            list.add(toMerchandise(doc));
        return list;
    }

    public List<Merchandise> findAll() {
        List<Merchandise> list = new ArrayList<>();
        for (Document doc : collection.find())
            list.add(toMerchandise(doc));
        return list;
    }

    public Merchandise findByCode(String itemCode) {
        Document doc = collection.find(Filters.eq("itemCode", itemCode)).first();
        return doc != null ? toMerchandise(doc) : null;
    }

    public boolean existsByCode(String itemCode) {
        return collection.find(Filters.eq("itemCode", itemCode)).first() != null;
    }

    public void update(Merchandise m) {
        collection.updateOne(
            Filters.eq("itemCode", m.getItemCode()),
            Updates.combine(
                Updates.set("itemName", m.getItemName()),
                Updates.set("defaultUnit", m.getDefaultUnit()),
                Updates.set("category", m.getCategory()),
                Updates.set("description", m.getDescription()),
                Updates.set("updatedAt", LocalDateTime.now().toString())
            )
        );
    }

    public void setActive(String itemCode, boolean active) {
        collection.updateOne(
            Filters.eq("itemCode", itemCode),
            Updates.combine(
                Updates.set("isActive", active),
                Updates.set("updatedAt", LocalDateTime.now().toString())
            )
        );
    }

    private Document toDocument(Merchandise m) {
        return new Document()
            .append("itemCode", m.getItemCode())
            .append("itemName", m.getItemName())
            .append("defaultUnit", m.getDefaultUnit())
            .append("category", m.getCategory())
            .append("description", m.getDescription())
            .append("isActive", true)
            .append("createdBy", m.getCreatedBy())
            .append("createdAt", LocalDateTime.now().toString())
            .append("updatedAt", LocalDateTime.now().toString());
    }

    private Merchandise toMerchandise(Document doc) {
        Merchandise m = new Merchandise();
        m.setId(doc.getObjectId("_id"));
        m.setItemCode(doc.getString("itemCode"));
        m.setItemName(doc.getString("itemName"));
        m.setDefaultUnit(doc.getString("defaultUnit"));
        m.setCategory(doc.getString("category"));
        m.setDescription(doc.getString("description"));
        m.setActive(Boolean.TRUE.equals(doc.getBoolean("isActive")));
        m.setCreatedBy(doc.getString("createdBy"));
        String createdAt = doc.getString("createdAt");
        if (createdAt != null) m.setCreatedAt(LocalDateTime.parse(createdAt));
        String updatedAt = doc.getString("updatedAt");
        if (updatedAt != null) m.setUpdatedAt(LocalDateTime.parse(updatedAt));
        return m;
    }
}