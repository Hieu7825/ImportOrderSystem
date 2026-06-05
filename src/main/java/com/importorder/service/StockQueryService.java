package com.importorder.service;

import com.importorder.model.StockInfo;
import com.importorder.repository.StockRepository;

public class StockQueryService {

    private final StockRepository stockRepo;

    // Constructor for Dependency Injection (for testing)
    public StockQueryService(StockRepository stockRepo) {
        this.stockRepo = stockRepo;
    }

    // Default Constructor (for production)
    public StockQueryService() {
        this.stockRepo = new StockRepository();
    }

    public StockInfo getStock(String batchId, String siteCode, String itemCode) {
        return stockRepo.findByBatchSiteItem(batchId, siteCode, itemCode);
    }
}