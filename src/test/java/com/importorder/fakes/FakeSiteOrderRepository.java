package com.importorder.fakes;

import com.importorder.model.SiteOrder;
import com.importorder.repository.SiteOrderRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FakeSiteOrderRepository extends SiteOrderRepository {

    private Map<String, SiteOrder> database = new HashMap<>();

    @Override
    public void save(SiteOrder so) {
        database.put(so.getSiteOrderId(), so);
    }

    @Override
    public SiteOrder findBySiteOrderId(String siteOrderId) {
        return database.get(siteOrderId);
    }

    @Override
    public List<SiteOrder> findByBatch(String batchId) {
        List<SiteOrder> list = new ArrayList<>();
        for (SiteOrder so : database.values()) {
            if (batchId.equals(so.getBatchId())) {
                list.add(so);
            }
        }
        return list;
    }

    @Override
    public List<SiteOrder> findBySubBatch(String subBatchId) {
        List<SiteOrder> list = new ArrayList<>();
        for (SiteOrder so : database.values()) {
            if (subBatchId.equals(so.getSubBatchId())) {
                list.add(so);
            }
        }
        return list;
    }

    @Override
    public List<SiteOrder> findBySite(String siteCode) {
        List<SiteOrder> list = new ArrayList<>();
        for (SiteOrder so : database.values()) {
            if (siteCode.equals(so.getSiteCode())) {
                list.add(so);
            }
        }
        return list;
    }

    @Override
    public List<SiteOrder> findByStatus(String status) {
        List<SiteOrder> list = new ArrayList<>();
        for (SiteOrder so : database.values()) {
            if (status.equals(so.getStatus())) {
                list.add(so);
            }
        }
        return list;
    }

    @Override
    public List<SiteOrder> findPendingInspection() {
        List<SiteOrder> list = new ArrayList<>();
        for (SiteOrder so : database.values()) {
            if ("SENT".equals(so.getStatus()) || "PARTIALLY_RECEIVED".equals(so.getStatus())) {
                list.add(so);
            }
        }
        return list;
    }

    @Override
    public List<SiteOrder> findUnconfirmedBySite(String siteCode) {
        List<SiteOrder> list = new ArrayList<>();
        for (SiteOrder so : database.values()) {
            if (siteCode.equals(so.getSiteCode()) && "SENT".equals(so.getStatus()) && !so.isConfirmedBySite()) {
                list.add(so);
            }
        }
        return list;
    }

    @Override
    public List<SiteOrder> findCancelRequests() {
        List<SiteOrder> list = new ArrayList<>();
        for (SiteOrder so : database.values()) {
            if (so.getCancelRequestedAt() != null && !"CANCELLED".equals(so.getStatus())) {
                list.add(so);
            }
        }
        // sort by estimatedArrival
        list.sort((a, b) -> {
            if (a.getEstimatedArrival() == null) return 1;
            if (b.getEstimatedArrival() == null) return -1;
            return a.getEstimatedArrival().compareTo(b.getEstimatedArrival());
        });
        return list;
    }

    @Override
    public List<SiteOrder> findCancelledForReplacement() {
        List<SiteOrder> list = new ArrayList<>();
        for (SiteOrder so : database.values()) {
            if ("CANCELLED".equals(so.getStatus())) {
                list.add(so);
            }
        }
        return list;
    }

    @Override
    public List<SiteOrder> findAll() {
        return new ArrayList<>(database.values());
    }

    @Override
    public void updateStatus(String siteOrderId, String status) {
        SiteOrder so = database.get(siteOrderId);
        if (so != null) {
            so.setStatus(status);
        }
    }

    @Override
    public void confirmBySite(String siteOrderId) {
        SiteOrder so = database.get(siteOrderId);
        if (so != null) {
            so.setConfirmedBySite(true);
            so.setConfirmedAt(LocalDateTime.now());
            so.setStatus("CONFIRMED");
        }
    }

    @Override
    public void requestCancel(String siteOrderId, String reason) {
        SiteOrder so = database.get(siteOrderId);
        if (so != null) {
            so.setCancelRequestedAt(LocalDateTime.now());
            so.setCancelRequestReason(reason);
        }
    }

    @Override
    public void approveCancel(String siteOrderId, String approvedBy) {
        SiteOrder so = database.get(siteOrderId);
        if (so != null && so.getCancelRequestedAt() != null && !"CANCELLED".equals(so.getStatus())) {
            so.setStatus("CANCELLED");
            so.setCancelApprovedBy(approvedBy);
            so.setCancelRequestedAt(null);
            so.setCancelRequestReason(null);
        }
    }

    @Override
    public void rejectCancelRequest(String siteOrderId, String rejectReason) {
        SiteOrder so = database.get(siteOrderId);
        if (so != null) {
            so.setCancelRequestedAt(null);
            so.setCancelRequestReason(null);
        }
    }

    @Override
    public void updateDeliveryMeans(String siteOrderId, String means, LocalDate newArrival) {
        SiteOrder so = database.get(siteOrderId);
        if (so != null) {
            so.setDeliveryMeans(means);
            so.setEstimatedArrival(newArrival);
        }
    }

    @Override
    public void deleteByBatch(String batchId) {
        database.values().removeIf(so -> batchId.equals(so.getBatchId()));
    }
}
