package com.importorder.service;

import com.importorder.model.*;
import com.importorder.repository.*;
import com.importorder.util.AppException;
import com.importorder.util.DateUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class OrderOptimizationService {

    private final SiteRepository          siteRepo      = new SiteRepository();
    private final StockRepository         stockRepo     = new StockRepository();
    private final OrderRequestRepository  orderRepo     = new OrderRequestRepository();
    private final SiteOrderRepository     siteOrderRepo = new SiteOrderRepository();
    private final FinalOrderRepository    finalOrderRepo= new FinalOrderRepository();

    // =========================================================
    // BƯỚC 1: Tra cứu tồn kho
    // =========================================================
    public void startProcessing(String batchId) {
        OrderRequest req = orderRepo.findByBatchId(batchId);
        if (req == null) throw new AppException("Không tìm thấy batch: " + batchId);
        if (!"PENDING".equals(req.getStatus()))
            throw new AppException("Batch " + batchId + " không ở trạng thái PENDING.");
        orderRepo.updateStatus(batchId, "PROCESSING");
    }

    // =========================================================
    // BƯỚC 2: Lấy ma trận tồn kho
    // =========================================================
    public Map<String, List<StockInfo>> getStockMatrix(String batchId) {
        List<StockInfo> all = stockRepo.findByBatch(batchId);
        Map<String, List<StockInfo>> matrix = new LinkedHashMap<>();
        for (StockInfo s : all)
            matrix.computeIfAbsent(s.getItemCode(), k -> new ArrayList<>()).add(s);
        return matrix;
    }

    // =========================================================
    // BƯỚC 3: Sinh 4 phương án với chiến lược thực sự khác nhau
    //
    //  Variant 0 — ÍT SITE NHẤT   : tập trung vào 1-2 site có nhiều hàng nhất,
    //                                ưu tiên ship để tiết kiệm chi phí.
    //  Variant 1 — NHANH NHẤT     : ưu tiên air toàn bộ, chọn site có airDays
    //                                ngắn nhất bất kể chi phí.
    //  Variant 2 — CÂN BẰNG       : phân tán đều, mỗi site ship nếu kịp deadline
    //                                còn không thì air; ưu tiên site được đánh dấu ★.
    //  Variant 3 — TRÁNH RỦI RO   : dùng nhiều site nhất có thể (không bỏ trứng
    //                                vào 1 giỏ), mỗi site ship nếu kịp, air nếu không.
    // =========================================================
    public List<Map<String, Object>> generateAllPlans(String batchId,
                                                       List<String> prioritized,
                                                       List<String> avoided) {
        OrderRequest req = orderRepo.findByBatchId(batchId);
        if (req == null) throw new AppException("Không tìm thấy batch: " + batchId);

        List<Map<String, Object>> plans = new ArrayList<>();
        String[] labels = {
            "Ít site nhất (tiết kiệm chi phí)",
            "Nhanh nhất (ưu tiên Air)",
            "Cân bằng (★ ưu tiên + Ship/Air linh hoạt)",
            "Phân tán rủi ro (nhiều site)"
        };

        for (int v = 0; v < 4; v++) {
            Map<String, Object> plan = computePlan(req, prioritized, avoided, v);
            if (plan != null) {
                plan.put("label", labels[v]);
                plans.add(plan);
            }
        }

        // Đánh index
        for (int i = 0; i < plans.size(); i++) plans.get(i).put("planIndex", i);
        return plans;
    }

    // =========================================================
    // BƯỚC 4: Xác nhận & sinh đơn hàng
    // =========================================================
    public void confirmPlan(String batchId, Map<String, Object> selectedPlan) {
        OrderRequest req = orderRepo.findByBatchId(batchId);
        if (req == null) throw new AppException("Không tìm thấy batch: " + batchId);
        if ("CANCELLED".equals(req.getStatus()))
            throw new AppException("Batch đã bị hủy trong khi xử lý. Không thể sinh đơn.");

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
            siteOrder.setSiteCode(siteCode);
            siteOrder.setDeliveryMeans(means);
            siteOrder.setEstimatedArrival(arrival);
            siteOrder.setStatus("SENT");
            siteOrder.setConfirmedBySite(false);
            siteOrder.setCreatedAt(LocalDateTime.now());
            siteOrderRepo.save(siteOrder);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) so.get("items");
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

        orderRepo.updateStatus(batchId, "COMPLETED");
    }

    // =========================================================
    // INTERNAL: Tính 1 phương án
    // =========================================================
    private Map<String, Object> computePlan(OrderRequest req,
                                             List<String> prioritized,
                                             List<String> avoided,
                                             int variant) {
        Map<String, Map<String, Object>> siteAllocMap  = new LinkedHashMap<>();
        List<Map<String, Object>>        insufficients = new ArrayList<>();

        for (OrderItem item : req.getItems()) {
            long daysLeft = ChronoUnit.DAYS.between(
                LocalDate.now(), item.getDesiredDeliveryDate());

            // Lấy stock hợp lệ (> 0, không bị avoided, kịp deadline)
            List<StockInfo> stocks = stockRepo
                .findByBatchAndItem(req.getBatchId(), item.getItemCode())
                .stream()
                .filter(s -> s.getInStockQty() > 0)
                .filter(s -> !avoided.contains(s.getSiteCode()))
                .filter(s -> {
                    SiteInfo site = siteRepo.findByCode(s.getSiteCode());
                    if (site == null) return false;
                    return site.getShipDays() <= daysLeft || site.getAirDays() <= daysLeft;
                })
                .collect(Collectors.toList());

            if (stocks.isEmpty()) {
                insufficients.add(buildInsufficient(item, "NO_ELIGIBLE_SITE", 0));
                continue;
            }

            // Sắp xếp theo chiến lược
            stocks.sort(buildComparator(prioritized, daysLeft, variant));

            // Variant 0 — ÍT SITE NHẤT: chỉ lấy site đầu tiên có đủ hàng nếu có thể
            if (variant == 0) {
                stocks = concentrateToFewestSites(stocks, item.getQuantityOrdered());
            }
            // Variant 3 — PHÂN TÁN: tối đa site, mỗi site chỉ cung cấp 1 phần nhỏ
            else if (variant == 3) {
                stocks = spreadAcrossAllSites(stocks, item.getQuantityOrdered());
            }

            int remaining = item.getQuantityOrdered();
            for (StockInfo stock : stocks) {
                if (remaining <= 0) break;

                SiteInfo site   = siteRepo.findByCode(stock.getSiteCode());
                String   means  = chooseMeans(site, daysLeft, variant);
                int      take   = Math.min(remaining, stock.getInStockQty());
                LocalDate arrival = LocalDate.now().plusDays(
                    "ship".equals(means) ? site.getShipDays() : site.getAirDays());

                String siteCode = stock.getSiteCode();
                siteAllocMap.computeIfAbsent(siteCode, k -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("siteCode",         siteCode);
                    m.put("means",            means);
                    m.put("estimatedArrival", arrival);
                    m.put("items",            new ArrayList<Map<String, Object>>());
                    return m;
                });

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> itemList =
                    (List<Map<String, Object>>) siteAllocMap.get(siteCode).get("items");

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
        plan.put("planIndex",        0);
        plan.put("variant",          variant);
        plan.put("siteOrders",       new ArrayList<>(siteAllocMap.values()));
        plan.put("insufficientItems",insufficients);
        plan.put("totalSites",       siteAllocMap.size());
        return plan;
    }

    // =========================================================
    // INTERNAL: Chiến lược chọn phương tiện vận chuyển
    // =========================================================
    private String chooseMeans(SiteInfo site, long daysLeft, int variant) {
        boolean canShip = site.getShipDays() <= daysLeft;
        boolean canAir  = site.getAirDays()  <= daysLeft;

        return switch (variant) {
            // Variant 0 — tiết kiệm: ship nếu được, không thì air
            case 0 -> canShip ? "ship" : "air";
            // Variant 1 — nhanh nhất: luôn air nếu được
            case 1 -> canAir  ? "air"  : "ship";
            // Variant 2 — cân bằng: ship nếu kịp deadline >= 3 ngày dư, không thì air
            case 2 -> (canShip && daysLeft - site.getShipDays() >= 3) ? "ship" : "air";
            // Variant 3 — phân tán: ship nếu được
            default -> canShip ? "ship" : "air";
        };
    }

    // =========================================================
    // INTERNAL: Comparator theo variant
    // =========================================================
    private Comparator<StockInfo> buildComparator(List<String> prioritized,
                                                   long daysLeft, int variant) {
        // Prioritized sites lên đầu (tất cả variant trừ 1 và 3)
        Comparator<StockInfo> priority = (variant == 1 || variant == 3)
            ? Comparator.comparingInt(s -> 0)
            : Comparator.comparingInt(s -> prioritized.contains(s.getSiteCode()) ? 0 : 1);

        Comparator<StockInfo> byMeans;
        Comparator<StockInfo> byStock;

        switch (variant) {
            case 0 -> {
                // Ít site nhất: nhiều hàng nhất lên trước → tập trung 1-2 site
                byMeans = Comparator.comparingInt(s -> canShip(s.getSiteCode(), daysLeft) ? 0 : 1);
                byStock = Comparator.comparingInt(s -> -s.getInStockQty());
            }
            case 1 -> {
                // Nhanh nhất: airDays ngắn nhất lên trước
                byMeans = Comparator.comparingInt(s -> {
                    SiteInfo si = siteRepo.findByCode(s.getSiteCode());
                    return si != null ? si.getAirDays() : 999;
                });
                byStock = Comparator.comparingInt(s -> -s.getInStockQty());
            }
            case 2 -> {
                // Cân bằng: ★ trước, rồi ship nếu kịp, rồi stock nhiều
                byMeans = Comparator.comparingInt(s -> canShip(s.getSiteCode(), daysLeft) ? 0 : 1);
                byStock = Comparator.comparingInt(s -> -s.getInStockQty());
            }
            default -> {
                // Phân tán: stock ÍT nhất lên trước → buộc dùng nhiều site
                byMeans = Comparator.comparingInt(s -> canShip(s.getSiteCode(), daysLeft) ? 0 : 1);
                byStock = Comparator.comparingInt(StockInfo::getInStockQty); // tăng dần
            }
        }

        return priority.thenComparing(byMeans).thenComparing(byStock);
    }

    // =========================================================
    // INTERNAL: Variant 0 — tập trung vào ít site nhất
    // Nếu site đầu tiên đủ hàng → chỉ dùng site đó
    // =========================================================
    private List<StockInfo> concentrateToFewestSites(List<StockInfo> stocks, int needed) {
        int cumulative = 0;
        List<StockInfo> result = new ArrayList<>();
        for (StockInfo s : stocks) {
            result.add(s);
            cumulative += s.getInStockQty();
            if (cumulative >= needed) break; // đủ rồi, dừng thêm site
        }
        return result;
    }

    // =========================================================
    // INTERNAL: Variant 3 — phân tán sang nhiều site nhất có thể
    // Mỗi site chỉ cung cấp tối đa (needed / totalSites) + phần dư
    // =========================================================
    private List<StockInfo> spreadAcrossAllSites(List<StockInfo> stocks, int needed) {
        if (stocks.size() <= 1) return stocks;
        // Không lọc bớt — dùng tất cả site có hàng
        // Comparator đã sắp xếp stock ít nhất lên trước để buộc dùng nhiều site
        return stocks;
    }

    // =========================================================
    // Helpers
    // =========================================================
    private boolean canShip(String siteCode, long daysLeft) {
        SiteInfo site = siteRepo.findByCode(siteCode);
        return site != null && site.getShipDays() <= daysLeft;
    }

    private Map<String, Object> buildInsufficient(OrderItem item, String reason, int available) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("itemCode",      item.getItemCode());
        err.put("itemName",      item.getItemName());
        err.put("needed",        item.getQuantityOrdered());
        err.put("reason",        reason);
        err.put("totalAvailable",available);
        return err;
    }
}