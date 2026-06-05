package com.importorder.service;

import com.importorder.model.*;
import com.importorder.repository.*;
import com.importorder.util.AppException;
import com.importorder.util.DateUtils;
import com.importorder.util.SessionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class OrderOptimizationService {

    private final SiteRepository         siteRepo;
    private final StockRepository        stockRepo;
    private final OrderRequestRepository orderRepo;
    private final SiteOrderRepository    siteOrderRepo;
    private final FinalOrderRepository   finalOrderRepo;
    private final SubBatchRepository     subBatchRepo;

    public OrderOptimizationService() {
        this.siteRepo = new SiteRepository();
        this.stockRepo = new StockRepository();
        this.orderRepo = new OrderRequestRepository();
        this.siteOrderRepo = new SiteOrderRepository();
        this.finalOrderRepo = new FinalOrderRepository();
        this.subBatchRepo = new SubBatchRepository();
    }

    public OrderOptimizationService(SiteRepository siteRepo,
                                     StockRepository stockRepo,
                                     OrderRequestRepository orderRepo,
                                     SiteOrderRepository siteOrderRepo,
                                     FinalOrderRepository finalOrderRepo,
                                     SubBatchRepository subBatchRepo) {
        this.siteRepo = siteRepo;
        this.stockRepo = stockRepo;
        this.orderRepo = orderRepo;
        this.siteOrderRepo = siteOrderRepo;
        this.finalOrderRepo = finalOrderRepo;
        this.subBatchRepo = subBatchRepo;
    }

    // =========================================================
    // BƯỚC 1: Bắt đầu xử lý batch — tạo SubBatch ORIGINAL
    // =========================================================
    public SubBatch startProcessing(String batchId) {
        OrderRequest req = orderRepo.findByBatchId(batchId);
        if (req == null) throw new AppException("Không tìm thấy batch: " + batchId);
        if (!"PENDING".equals(req.getStatus()))
            throw new AppException("Batch " + batchId + " không ở trạng thái PENDING.");

        // Kiểm tra đã có sub-batch PROCESSING chưa
        List<SubBatch> existing = subBatchRepo.findByParentBatch(batchId);
        for (SubBatch sb : existing) {
            if ("PROCESSING".equals(sb.getStatus())) return sb; // trả về cái cũ
        }

        // Tạo sub-batch ORIGINAL
        SubBatch sb = new SubBatch();
        sb.setSubBatchId(DateUtils.generateSubBatchId(batchId));
        sb.setParentBatchId(batchId);
        sb.setType("ORIGINAL");
        sb.setStatus("PROCESSING");
        sb.setItems(req.getItems());
        sb.setCreatedBy(SessionManager.getUsername());
        sb.setCreatedAt(LocalDateTime.now());
        sb.setUpdatedAt(LocalDateTime.now());
        subBatchRepo.save(sb);

        orderRepo.updateStatus(batchId, "PROCESSING");
        return sb;
    }

    // =========================================================
    // BƯỚC 1b: Tạo sub-batch REPLACEMENT từ đơn bị hủy
    // =========================================================
    public SubBatch createReplacementSubBatch(String cancelledSiteOrderId) {
        // Lấy thông tin đơn bị hủy
        SiteOrder cancelledOrder = siteOrderRepo.findBySiteOrderId(cancelledSiteOrderId);
        if (cancelledOrder == null)
            throw new AppException("Không tìm thấy đơn: " + cancelledSiteOrderId);
        if (!"CANCELLED".equals(cancelledOrder.getStatus()))
            throw new AppException("Đơn " + cancelledSiteOrderId + " chưa bị hủy.");

        String batchId = cancelledOrder.getBatchId();
        OrderRequest req = orderRepo.findByBatchId(batchId);
        if (req == null) throw new AppException("Không tìm thấy batch gốc: " + batchId);

        // Lấy các mặt hàng trong đơn bị hủy
        List<FinalOrder> cancelledItems = finalOrderRepo.findBySiteOrder(cancelledSiteOrderId);
        if (cancelledItems.isEmpty())
            throw new AppException("Đơn bị hủy không có mặt hàng nào.");

        // Convert FinalOrder → OrderItem để tái xử lý
        List<OrderItem> itemsToReplace = cancelledItems.stream().map(fo -> {
            OrderItem oi = new OrderItem();
            oi.setItemCode(fo.getItemCode());
            oi.setItemName(fo.getItemName());
            oi.setQuantityOrdered(fo.getQuantityOrdered());
            oi.setUnit(fo.getUnit());
            // Giữ nguyên desiredDeliveryDate từ batch gốc nếu có
            req.getItems().stream()
                .filter(i -> i.getItemCode().equals(fo.getItemCode()))
                .findFirst()
                .ifPresent(orig -> oi.setDesiredDeliveryDate(orig.getDesiredDeliveryDate()));
            // Nếu không tìm được → dùng 30 ngày từ hôm nay
            if (oi.getDesiredDeliveryDate() == null)
                oi.setDesiredDeliveryDate(LocalDate.now().plusDays(30));
            return oi;
        }).collect(Collectors.toList());

        // Tạo sub-batch REPLACEMENT
        SubBatch sb = new SubBatch();
        sb.setSubBatchId(DateUtils.generateSubBatchId(batchId));
        sb.setParentBatchId(batchId);
        sb.setType("REPLACEMENT");
        sb.setStatus("PROCESSING");
        sb.setReplacingOrderId(cancelledSiteOrderId);
        sb.setItems(itemsToReplace);
        sb.setCreatedBy(SessionManager.getUsername());
        sb.setCreatedAt(LocalDateTime.now());
        sb.setUpdatedAt(LocalDateTime.now());
        subBatchRepo.save(sb);

        // Batch lớn về PROCESSING để OOD xử lý lại
        orderRepo.updateStatus(batchId, "PROCESSING");
        return sb;
    }

    // =========================================================
    // BƯỚC 2: Ma trận tồn kho (dùng cho SubBatch)
    // =========================================================
    public Map<String, List<StockInfo>> getStockMatrix(String batchId) {
        List<StockInfo> all = stockRepo.findByBatch(batchId);
        Map<String, List<StockInfo>> matrix = new LinkedHashMap<>();
        for (StockInfo s : all)
            matrix.computeIfAbsent(s.getItemCode(), k -> new ArrayList<>()).add(s);
        return matrix;
    }

    // =========================================================
    // BƯỚC 3: Sinh 4 phương án
    // =========================================================
    public List<Map<String, Object>> generateAllPlans(String batchId,
                                                       List<String> prioritized,
                                                       List<String> avoided) {
        return generateAllPlansForSubBatch(batchId, null, prioritized, avoided);
    }

    /**
     * Sinh phương án cho sub-batch cụ thể.
     * Nếu subBatchId = null → dùng items của batch gốc.
     */
    public List<Map<String, Object>> generateAllPlansForSubBatch(
            String batchId, String subBatchId,
            List<String> prioritized, List<String> avoided) {

        OrderRequest req = orderRepo.findByBatchId(batchId);
        if (req == null) throw new AppException("Không tìm thấy batch: " + batchId);

        List<OrderItem> items;
        if (subBatchId != null) {
            SubBatch sb = subBatchRepo.findBySubBatchId(subBatchId);
            if (sb == null) throw new AppException("Không tìm thấy sub-batch: " + subBatchId);
            items = sb.getItems();
        } else {
            items = req.getItems();
        }

        List<Map<String, Object>> plans = new ArrayList<>();
        String[] labels = {
            "Ít site nhất (tiết kiệm chi phí)",
            "Nhanh nhất (ưu tiên Air)",
            "Cân bằng (★ ưu tiên + Ship/Air linh hoạt)",
            "Phân tán rủi ro (nhiều site)"
        };

        for (int v = 0; v < 4; v++) {
        	Map<String, Object> plan = computePlan(req, items, prioritized, avoided, v, subBatchId);
            if (plan != null) {
                plan.put("label",    labels[v]);
                plan.put("batchId",  batchId);
                plan.put("subBatchId", subBatchId);
                plans.add(plan);
            }
        }

        for (int i = 0; i < plans.size(); i++) plans.get(i).put("planIndex", i);
        return plans;
    }

    // =========================================================
    // BƯỚC 4: Xác nhận & sinh đơn hàng
    // =========================================================
    public void confirmPlan(String batchId, Map<String, Object> selectedPlan) {
        confirmPlanForSubBatch(batchId, null, selectedPlan);
    }

    public void confirmPlanForSubBatch(String batchId, String subBatchId,
                                        Map<String, Object> selectedPlan) {
        OrderRequest req = orderRepo.findByBatchId(batchId);
        if (req == null) throw new AppException("Không tìm thấy batch: " + batchId);
        if ("CANCELLED".equals(req.getStatus()))
            throw new AppException("Batch đã bị hủy, không thể sinh đơn.");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> siteOrders =
            (List<Map<String, Object>>) selectedPlan.get("siteOrders");

        for (Map<String, Object> so : siteOrders) {
            String    siteCode = (String)    so.get("siteCode");
            String    means    = (String)    so.get("means");
            LocalDate arrival  = (LocalDate) so.get("estimatedArrival");

            SiteOrder siteOrder = new SiteOrder();
            siteOrder.setSiteOrderId(DateUtils.generateSiteOrderId(siteCode));
            siteOrder.setBatchId(batchId);
            siteOrder.setSubBatchId(subBatchId);     // gắn sub-batch
            siteOrder.setSiteCode(siteCode);
            siteOrder.setDeliveryMeans(means);
            siteOrder.setEstimatedArrival(arrival);
            siteOrder.setStatus("SENT");
            siteOrder.setConfirmedBySite(false);
            siteOrder.setCreatedAt(LocalDateTime.now());
            siteOrderRepo.save(siteOrder);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items =
                (List<Map<String, Object>>) so.get("items");
            for (Map<String, Object> item : items) {
                FinalOrder fo = new FinalOrder();
                fo.setSiteOrderId(siteOrder.getSiteOrderId());
                fo.setBatchId(batchId);
                fo.setSiteCode(siteCode);
                fo.setItemCode((String)  item.get("itemCode"));
                fo.setItemName((String)  item.get("itemName"));
                fo.setQuantityOrdered((Integer) item.get("qty"));
                fo.setUnit((String)     item.get("unit"));
                fo.setDeliveryMeans(means);
                fo.setStatus("SENT");
                fo.setCreatedAt(LocalDateTime.now());
                finalOrderRepo.save(fo);
            }
        }

        // Cập nhật sub-batch → COMPLETED
        if (subBatchId != null) {
            subBatchRepo.updateStatus(subBatchId, "COMPLETED");
        } else {
            // Luồng ORIGINAL: đóng sub-batch ORIGINAL đang PROCESSING để
            // displayStatus của batch chuyển sang COMPLETED, tránh việc OOD
            // xử lý / sinh đơn lại lần nữa cho cùng một yêu cầu.
            for (SubBatch sb : subBatchRepo.findByParentBatch(batchId)) {
                if ("ORIGINAL".equals(sb.getType()) && "PROCESSING".equals(sb.getStatus())) {
                    subBatchRepo.updateStatus(sb.getSubBatchId(), "COMPLETED");
                }
            }
        }
        orderRepo.updateStatus(batchId, "COMPLETED");
    }

    // =========================================================
    // INTERNAL: Tính 1 phương án
    // =========================================================
    private Map<String, Object> computePlan(OrderRequest req,
            								List<OrderItem> items,
            								List<String> prioritized,
            								List<String> avoided,
            								int variant,
            								String subBatchId) {
        Map<String, Map<String, Object>> siteAllocMap  = new LinkedHashMap<>();
        List<Map<String, Object>>        insufficients = new ArrayList<>();
        List<String> excludedSites = new ArrayList<>();

        if (subBatchId != null) {
            SubBatch sb = subBatchRepo.findBySubBatchId(subBatchId);

            if (sb != null && sb.getReplacingOrderId() != null) {
                SiteOrder cancelledOrder = siteOrderRepo.findBySiteOrderId(sb.getReplacingOrderId());

                if (cancelledOrder != null && cancelledOrder.getSiteCode() != null) {
                    excludedSites.add(cancelledOrder.getSiteCode());
                }
            }
        }

        for (OrderItem item : items) {
            if (item.getDesiredDeliveryDate() == null) continue;
            long daysLeft = ChronoUnit.DAYS.between(
            	    LocalDate.now(), item.getDesiredDeliveryDate());

            List<StockInfo> allStockForItem = stockRepo
            	    .findByBatchAndItem(req.getBatchId(), item.getItemCode())
            	    .stream()
            	    .filter(s -> s.getInStockQty() > 0)
            	    .filter(s -> !containsSiteCode(excludedSites, s.getSiteCode()))
            	    .collect(Collectors.toList());

            	if (allStockForItem.isEmpty()) {
            	    insufficients.add(buildInsufficient(item, "NO_STOCK", 0));
            	    continue;
            	}

            	// Stock đủ điều kiện về site active/ready + deadline
            	List<StockInfo> eligibleStock = allStockForItem.stream()
            	    .filter(s -> {
            	        SiteInfo site = siteRepo.findByCode(s.getSiteCode());
            	        if (site == null || !site.isReadyForOrder()) return false;
            	        return site.getShipDays() <= daysLeft || site.getAirDays() <= daysLeft;
            	    })
            	    .collect(Collectors.toList());

            	if (eligibleStock.isEmpty()) {
            	    boolean hasReadySite = allStockForItem.stream().anyMatch(s -> {
            	        SiteInfo site = siteRepo.findByCode(s.getSiteCode());
            	        return site != null && site.isReadyForOrder();
            	    });

            	    if (!hasReadySite) {
            	        insufficients.add(buildInsufficient(item, "SITE_NOT_READY",
            	            allStockForItem.stream().mapToInt(StockInfo::getInStockQty).sum()));
            	    } else {
            	        insufficients.add(buildInsufficient(item, "DELIVERY_DEADLINE_TOO_SOON",
            	            allStockForItem.stream().mapToInt(StockInfo::getInStockQty).sum()));
            	    }
            	    continue;
            	}

            	// Tách stock thường và stock bị đánh dấu tránh
            	List<StockInfo> normalStocks = eligibleStock.stream()
            	    .filter(s -> !containsSiteCode(avoided, s.getSiteCode()))
            	    .collect(Collectors.toList());

            	List<StockInfo> avoidedStocks = eligibleStock.stream()
            	    .filter(s -> containsSiteCode(avoided, s.getSiteCode()))
            	    .collect(Collectors.toList());

            	List<StockInfo> stocks;

            	if (!normalStocks.isEmpty()) {
            	    stocks = normalStocks;
            	} else if (!avoidedStocks.isEmpty()) {
            	    Map<String, Object> err = buildInsufficient(
            	        item,
            	        "ONLY_AVOIDED_SITES_AVAILABLE",
            	        avoidedStocks.stream().mapToInt(StockInfo::getInStockQty).sum()
            	    );

            	    err.put("avoidedSites", avoidedStocks.stream()
            	        .map(StockInfo::getSiteCode)
            	        .collect(Collectors.toList()));

            	    insufficients.add(err);
            	    stocks = avoidedStocks;
            	} else {
            	    insufficients.add(buildInsufficient(item, "NO_ELIGIBLE_SITE",
            	        allStockForItem.stream().mapToInt(StockInfo::getInStockQty).sum()));
            	    continue;
            	}

            stocks.sort(buildComparator(prioritized, daysLeft, variant));

            if (variant == 0) {
                stocks = concentrateToFewestSites(stocks, item.getQuantityOrdered());
            } else if (variant == 3) {
                stocks = spreadAcrossAllSites(stocks, item.getQuantityOrdered());
            }

            int remaining = item.getQuantityOrdered();
            for (StockInfo stock : stocks) {
                if (remaining <= 0) break;

                SiteInfo site   = siteRepo.findByCode(stock.getSiteCode());
                if (site == null) continue;
                String   means  = chooseMeans(site, daysLeft, variant);
                int      take   = Math.min(remaining, stock.getInStockQty());
                LocalDate arrival = LocalDate.now().plusDays(
                    "ship".equals(means) ? site.getShipDays() : site.getAirDays());

                String sc = stock.getSiteCode();
                siteAllocMap.computeIfAbsent(sc, k -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("siteCode",         sc);
                    m.put("means",            means);
                    m.put("estimatedArrival", arrival);
                    m.put("items",            new ArrayList<Map<String, Object>>());
                    return m;
                });

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> itemList =
                    (List<Map<String, Object>>) siteAllocMap.get(sc).get("items");

                Map<String, Object> allocItem = new LinkedHashMap<>();
                allocItem.put("itemCode", item.getItemCode());
                allocItem.put("itemName", item.getItemName());
                allocItem.put("qty",      take);
                allocItem.put("unit",     item.getUnit());
                itemList.add(allocItem);

                remaining -= take;
            }

            if (remaining > 0)
                insufficients.add(buildInsufficient(item, "INSUFFICIENT_STOCK",
                    stocks.stream().mapToInt(StockInfo::getInStockQty).sum()));
        }

        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("planIndex",         0);
        plan.put("variant",           variant);
        plan.put("siteOrders",        new ArrayList<>(siteAllocMap.values()));
        plan.put("insufficientItems", insufficients);
        plan.put("totalSites",        siteAllocMap.size());
        return plan;
    }

    // ── Helpers (giữ nguyên từ bản cũ) ───────────────────────────────────────

    private String chooseMeans(SiteInfo site, long daysLeft, int variant) {
        boolean canShip = site.getShipDays() <= daysLeft;
        boolean canAir  = site.getAirDays()  <= daysLeft;
        return switch (variant) {
            case 0  -> canShip ? "ship" : "air";
            case 1  -> canAir  ? "air"  : "ship";
            case 2  -> (canShip && daysLeft - site.getShipDays() >= 3) ? "ship" : "air";
            default -> canShip ? "ship" : "air";
        };
    }

    private Comparator<StockInfo> buildComparator(List<String> prioritized,
                                                   long daysLeft, int variant) {
        Comparator<StockInfo> priority = (variant == 1 || variant == 3)
            ? Comparator.comparingInt(s -> 0)
            : Comparator.comparingInt(s -> prioritized.contains(s.getSiteCode()) ? 0 : 1);

        Comparator<StockInfo> byMeans;
        Comparator<StockInfo> byStock;

        switch (variant) {
            case 0 -> {
                byMeans = Comparator.comparingInt(s -> canShip(s.getSiteCode(), daysLeft) ? 0 : 1);
                byStock = Comparator.comparingInt(s -> -s.getInStockQty());
            }
            case 1 -> {
                byMeans = Comparator.comparingInt(s -> {
                    SiteInfo si = siteRepo.findByCode(s.getSiteCode());
                    return si != null ? si.getAirDays() : 999;
                });
                byStock = Comparator.comparingInt(s -> -s.getInStockQty());
            }
            case 2 -> {
                byMeans = Comparator.comparingInt(s -> canShip(s.getSiteCode(), daysLeft) ? 0 : 1);
                byStock = Comparator.comparingInt(s -> -s.getInStockQty());
            }
            default -> {
                byMeans = Comparator.comparingInt(s -> canShip(s.getSiteCode(), daysLeft) ? 0 : 1);
                byStock = Comparator.comparingInt(StockInfo::getInStockQty);
            }
        }
        return priority.thenComparing(byMeans).thenComparing(byStock);
    }

    private List<StockInfo> concentrateToFewestSites(List<StockInfo> stocks, int needed) {
        int cumulative = 0;
        List<StockInfo> result = new ArrayList<>();
        for (StockInfo s : stocks) {
            result.add(s);
            cumulative += s.getInStockQty();
            if (cumulative >= needed) break;
        }
        return result;
    }

    private List<StockInfo> spreadAcrossAllSites(List<StockInfo> stocks, int needed) {
        if (stocks.size() <= 1) return stocks;
        return stocks;
    }

    private boolean canShip(String siteCode, long daysLeft) {
        SiteInfo site = siteRepo.findByCode(siteCode);
        return site != null && site.getShipDays() <= daysLeft;
    }

    private Map<String, Object> buildInsufficient(OrderItem item, String reason, int available) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("itemCode",       item.getItemCode());
        err.put("itemName",       item.getItemName());
        err.put("needed",         item.getQuantityOrdered());
        err.put("reason",         reason);
        err.put("totalAvailable", available);
        return err;
    }
    
    private boolean containsSiteCode(List<String> list, String siteCode) {
        if (list == null || siteCode == null) return false;

        String target = siteCode.trim();

        return list.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .anyMatch(code -> code.equalsIgnoreCase(target));
    }
}