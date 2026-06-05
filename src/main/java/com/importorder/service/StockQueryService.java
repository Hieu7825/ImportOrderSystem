package com.importorder.service;

import com.importorder.model.StockInfo;
import com.importorder.repository.StockRepository;

public class StockQueryService {

    private final StockRepository stockRepo = new StockRepository();

    public StockInfo getStock(String batchId, String siteCode, String itemCode) {
        return stockRepo.findByBatchSiteItem(batchId, siteCode, itemCode);
    }
}