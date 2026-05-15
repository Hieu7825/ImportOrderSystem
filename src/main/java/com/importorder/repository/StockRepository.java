package com.importorder.repository;

import com.importorder.config.MongoConfig;
import com.importorder.model.StockInfo;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StockRepository {

    private final MongoCollection<Document> collection;

    public StockRepository() {
        this.collection = MongoConfig.getDatabase().getCollection("stock_info");
    }

    // Upsert: cập nhật nếu đã có, tạo mới nếu chưa có
    public void saveOrUpdate(StockInfo stock) {
        Document doc = new Document()
            .append("batchId", stock.getBatchId())
            .append("siteCode", stock.getSiteCode())
            .append("itemCode", stock.getItemCode())
            .append("inStockQty", stock.getInStockQty())
            .append("unit", stock.getUnit())
            .append("updatedBy", stock.getUpdatedBy())
            .append("queriedAt", LocalDateTime.now().toString());

        collection.replaceOne(
            Filters.and(
                Filters.eq("batchId", stock.getBatchId()),
                Filters.eq("siteCode", stock.getSiteCode()),
                Filters.eq("itemCode", stock.getItemCode())
            ),
            doc,
            new ReplaceOptions().upsert(true)
        );
    }

    public List<StockInfo> findByBatch(String batchId) {
        List<StockInfo> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("batchId", batchId)))
            list.add(toStockInfo(doc));
        return list;
    }

    public List<StockInfo> findByBatchAndItem(String batchId, String itemCode) {
        List<StockInfo> list = new ArrayList<>();
        for (Document doc : collection.find(
                Filters.and(
                    Filters.eq("batchId", batchId),
                    Filters.eq("itemCode", itemCode)
                )))
            list.add(toStockInfo(doc));
        return list;
    }

    public StockInfo findByBatchSiteItem(String batchId, String siteCode, String itemCode) {
        Document doc = collection.find(
            Filters.and(
                Filters.eq("batchId", batchId),
                Filters.eq("siteCode", siteCode),
                Filters.eq("itemCode", itemCode)
            )).first();
        return doc != null ? toStockInfo(doc) : null;
    }

    private StockInfo toStockInfo(Document doc) {
        StockInfo s = new StockInfo();
        s.setId(doc.getObjectId("_id"));
        s.setBatchId(doc.getString("batchId"));
        s.setSiteCode(doc.getString("siteCode"));
        s.setItemCode(doc.getString("itemCode"));
        s.setInStockQty(doc.getInteger("inStockQty", 0));
        s.setUnit(doc.getString("unit"));
        String queriedAt = doc.getString("queriedAt");
        if (queriedAt != null) s.setQueriedAt(LocalDateTime.parse(queriedAt));
        return s;
    }
}