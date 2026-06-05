package com.importorder.fakes;

import com.importorder.model.OrderRequest;
import com.importorder.model.OrderItem;
import com.importorder.repository.OrderRequestRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

public class FakeOrderRequestRepository extends OrderRequestRepository {

    private Map<String, OrderRequest> database = new HashMap<>();

    @Override
    public void save(OrderRequest req) {
        database.put(req.getBatchId(), req);
    }

    @Override
    public OrderRequest findByBatchId(String batchId) {
        return database.get(batchId);
    }

    @Override
    public List<OrderRequest> findAll() {
        return new ArrayList<>(database.values());
    }

    @Override
    public List<OrderRequest> findByStatus(String status) {
        List<OrderRequest> list = new ArrayList<>();
        for (OrderRequest req : database.values()) {
            if (status.equals(req.getStatus())) {
                list.add(req);
            }
        }
        return list;
    }

    @Override
    public List<OrderRequest> findByCreatedBy(String username) {
        List<OrderRequest> list = new ArrayList<>();
        for (OrderRequest req : database.values()) {
            if (username.equals(req.getCreatedBy())) {
                list.add(req);
            }
        }
        return list;
    }

    @Override
    public void updateStatus(String batchId, String status) {
        OrderRequest req = database.get(batchId);
        if (req != null) {
            req.setStatus(status);
            req.setUpdatedAt(LocalDateTime.now());
        }
    }

    @Override
    public void cancel(String batchId, String cancelledBy, String reason) {
        OrderRequest req = database.get(batchId);
        if (req != null) {
            req.setStatus("CANCELLED");
            req.setCancelledBy(cancelledBy);
            req.setCancelReason(reason);
            req.setCancelledAt(LocalDateTime.now());
            req.setUpdatedAt(LocalDateTime.now());
        }
    }

    @Override
    public void update(String batchId, List<OrderItem> newItems) {
        OrderRequest req = database.get(batchId);
        if (req != null) {
            req.setItems(newItems);
            req.setUpdatedAt(LocalDateTime.now());
        }
    }
}
