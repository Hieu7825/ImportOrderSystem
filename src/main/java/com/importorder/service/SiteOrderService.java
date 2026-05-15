package com.importorder.service;

import com.importorder.model.FinalOrder;
import com.importorder.model.SiteOrder;
import com.importorder.model.SubBatch;
import com.importorder.repository.FinalOrderRepository;
import com.importorder.repository.SiteOrderRepository;
import com.importorder.repository.SubBatchRepository;
import com.importorder.util.AppException;
import com.importorder.util.SessionManager;

import java.util.List;

public class SiteOrderService {

    private final SiteOrderRepository  siteOrderRepo  = new SiteOrderRepository();
    private final FinalOrderRepository finalOrderRepo = new FinalOrderRepository();
    private final SubBatchRepository   subBatchRepo   = new SubBatchRepository();
    private final OrderOptimizationService optimService = new OrderOptimizationService();

    public List<SiteOrder> getByBatch(String batchId) {
        return siteOrderRepo.findByBatch(batchId);
    }

    public List<SiteOrder> getBySubBatch(String subBatchId) {
        return siteOrderRepo.findBySubBatch(subBatchId);
    }

    public List<SiteOrder> getBySite(String siteCode) {
        return siteOrderRepo.findBySite(siteCode);
    }

    public List<SiteOrder> getAll() {
        return siteOrderRepo.findAll();
    }

    public List<SiteOrder> getCancelRequests() {
        // Edge Case C4: đã sort theo estimatedArrival gần nhất trong repo
        return siteOrderRepo.findCancelRequests();
    }

    public List<SiteOrder> getCancelledForReplacement() {
        return siteOrderRepo.findCancelledForReplacement();
    }

    public List<FinalOrder> getItemsOfSiteOrder(String siteOrderId) {
        return finalOrderRepo.findBySiteOrder(siteOrderId);
    }

    // ── OOD: Sửa đơn hàng ────────────────────────────────────────────────────

    /**
     * Edge Case E2: OOD chỉ được sửa khi đơn còn SENT và SITE chưa xác nhận.
     */
    public void editSiteOrder(String siteOrderId, String deliveryMeans) {
        SiteOrder so = siteOrderRepo.findBySiteOrderId(siteOrderId);
        if (so == null)
            throw new AppException("Không tìm thấy đơn: " + siteOrderId);
        if (!"SENT".equals(so.getStatus()))
            throw new AppException("Chỉ có thể sửa đơn đang ở trạng thái SENT.");
        if (so.isConfirmedBySite())
            // Edge Case E2
            throw new AppException("Site đã xác nhận đơn này, không thể sửa. " +
                "Mọi thay đổi cần tạo đơn điều chỉnh mới.");

        // Tính lại estimatedArrival theo means mới
        com.importorder.model.SiteInfo site =
            new com.importorder.repository.SiteRepository()
                .findByCode(so.getSiteCode());
        if (site == null)
            throw new AppException("Không tìm thấy thông tin site: " + so.getSiteCode());

        int days = "ship".equals(deliveryMeans)
            ? site.getShipDays() : site.getAirDays();
        java.time.LocalDate newArrival = java.time.LocalDate.now().plusDays(days);

        siteOrderRepo.updateDeliveryMeans(siteOrderId, deliveryMeans, newArrival);
    }

    // ── OOD: Hủy đơn hàng ───────────────────────────────────────────────────

    public void cancelSiteOrder(String siteOrderId, String reason) {
        if (reason == null || reason.isBlank())
            throw new AppException("Lý do hủy không được để trống.");

        SiteOrder so = siteOrderRepo.findBySiteOrderId(siteOrderId);
        if (so == null)
            throw new AppException("Không tìm thấy đơn: " + siteOrderId);
        if ("RECEIVED".equals(so.getStatus()) || "PARTIALLY_RECEIVED".equals(so.getStatus()))
            throw new AppException("Hàng đã về, không thể hủy đơn này.");
        if ("CANCELLED".equals(so.getStatus()))
            throw new AppException("Đơn này đã bị hủy trước đó.");

        siteOrderRepo.updateStatus(siteOrderId, "CANCELLED");
        finalOrderRepo.cancelBySiteOrder(siteOrderId);

        // Cập nhật sub-batch nếu có
        if (so.getSubBatchId() != null) {
            subBatchRepo.updateStatus(so.getSubBatchId(), "CANCELLED");
        }
    }

    // ── OOD: Duyệt yêu cầu hủy từ SITE ─────────────────────────────────────

    public void approveCancelRequest(String siteOrderId) {
        SiteOrder so = siteOrderRepo.findBySiteOrderId(siteOrderId);

        if (so == null)
            throw new AppException("Không tìm thấy đơn: " + siteOrderId);

        if ("CANCELLED".equals(so.getStatus()))
            throw new AppException("Đơn này đã bị hủy trước đó.");

        if ("RECEIVED".equals(so.getStatus()) || "PARTIALLY_RECEIVED".equals(so.getStatus()))
            throw new AppException("Hàng đã được nhận, không thể duyệt hủy.");

        if (so.getCancelRequestedAt() == null)
            throw new AppException("Yêu cầu hủy này đã được xử lý hoặc không tồn tại.");

        siteOrderRepo.approveCancel(siteOrderId, SessionManager.getUsername());
        finalOrderRepo.cancelBySiteOrder(siteOrderId);

        if (so.getSubBatchId() != null) {
            subBatchRepo.updateStatus(so.getSubBatchId(), "CANCELLED");
        }
    }

    /**
     * Edge Case C2: OOD từ chối hủy → đơn về trạng thái cũ, ghi log lý do.
     */
    public void rejectCancelRequest(String siteOrderId, String rejectReason) {
        SiteOrder so = siteOrderRepo.findBySiteOrderId(siteOrderId);

        if (so == null)
            throw new AppException("Không tìm thấy đơn: " + siteOrderId);

        if ("CANCELLED".equals(so.getStatus()))
            throw new AppException("Đơn này đã bị hủy, không thể từ chối yêu cầu hủy nữa.");

        if (so.getCancelRequestedAt() == null)
            throw new AppException("Yêu cầu hủy này đã được xử lý hoặc không tồn tại.");

        if (rejectReason == null || rejectReason.isBlank())
            throw new AppException("Vui lòng nhập lý do từ chối.");

        siteOrderRepo.rejectCancelRequest(siteOrderId, rejectReason);
    }

    // ── SITE: Xác nhận nhận đơn ──────────────────────────────────────────────

    public void confirmBySite(String siteOrderId) {
        SiteOrder so = siteOrderRepo.findBySiteOrderId(siteOrderId);
        if (so == null)
            throw new AppException("Không tìm thấy đơn: " + siteOrderId);
        if ("CANCELLED".equals(so.getStatus()))
            throw new AppException("Đơn này đã bị hủy bởi OOD.");
        if (so.isConfirmedBySite())
            throw new AppException("Đơn này đã được xác nhận trước đó.");
        // Edge Case C1: chỉ xác nhận khi còn SENT
        if (!"SENT".equals(so.getStatus()) && !"CONFIRMED".equals(so.getStatus()))
            throw new AppException("Chỉ có thể xác nhận đơn ở trạng thái SENT.");

        siteOrderRepo.confirmBySite(siteOrderId);
    }

    // ── SITE: Gửi yêu cầu hủy ────────────────────────────────────────────────

    /**
     * Edge Case C1: SITE chỉ gửi yêu cầu hủy khi đơn còn SENT (chưa xác nhận).
     * Sau khi đã xác nhận (CONFIRMED) → không cho hủy theo luồng thông thường.
     */
    public void requestCancel(String siteOrderId, String reason) {
        if (reason == null || reason.isBlank())
            throw new AppException("Lý do yêu cầu hủy không được để trống.");

        SiteOrder so = siteOrderRepo.findBySiteOrderId(siteOrderId);
        if (so == null)
            throw new AppException("Không tìm thấy đơn: " + siteOrderId);
        if ("CANCELLED".equals(so.getStatus()))
            throw new AppException("Đơn này đã bị hủy.");

        // Edge Case C1: block nếu đã xác nhận
        if (so.isConfirmedBySite())
            throw new AppException("Đơn này đã được xác nhận. " +
                "Vui lòng liên hệ OOD trực tiếp để xử lý.");

        if (!"SENT".equals(so.getStatus()))
            throw new AppException("Chỉ có thể yêu cầu hủy đơn ở trạng thái SENT.");

        siteOrderRepo.requestCancel(siteOrderId, reason);
    }

    // ── OOD: Tìm phương án thay thế cho đơn bị hủy ──────────────────────────

    /**
     * Tạo sub-batch REPLACEMENT và trả về để OOD xử lý tiếp.
     */
    public SubBatch startReplacement(String cancelledSiteOrderId) {
        return optimService.createReplacementSubBatch(cancelledSiteOrderId);
    }
}