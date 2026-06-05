package com.importorder.fakes;

import com.importorder.model.StockInfo;
import com.importorder.repository.StockRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FakeStockRepository extends StockRepository {

    private Map<String, StockInfo> database = new HashMap<>();

    @Override
    public void saveOrUpdate(StockInfo stock) {
        String key = stock.getBatchId() + "_" + stock.getSiteCode() + "_" + stock.getItemCode();
        database.put(key, stock);
    }

    @Override
    public List<StockInfo> findByBatch(String batchId) {
        List<StockInfo> list = new ArrayList<>();
        for (StockInfo s : database.values()) {
            if (batchId.equals(s.getBatchId())) {
                list.add(s);
            }
        }
        return list;
    }

    @Override
    public List<StockInfo> findByBatchAndItem(String batchId, String itemCode) {
        List<StockInfo> list = new ArrayList<>();
        for (StockInfo s : database.values()) {
            if (batchId.equals(s.getBatchId()) && itemCode.equals(s.getItemCode())) {
                list.add(s);
            }
        }
        return list;
    }

    @Override
    public StockInfo findByBatchSiteItem(String batchId, String siteCode, String itemCode) {
        String key = batchId + "_" + siteCode + "_" + itemCode;
        return database.get(key);
    }
}
