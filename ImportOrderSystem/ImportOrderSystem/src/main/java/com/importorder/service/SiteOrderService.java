package com.importorder.service;

import com.importorder.model.FinalOrder;
import com.importorder.model.SiteOrder;
import com.importorder.repository.FinalOrderRepository;
import com.importorder.repository.SiteOrderRepository;
import com.importorder.util.AppException;
import com.importorder.util.SessionManager;

import java.util.List;

public class SiteOrderService {

    private final SiteOrderRepository siteOrderRepo = new SiteOrderRepository();
    private final FinalOrderRepository finalOrderRepo = new FinalOrderRepository();

    public List<SiteOrder> getByBatch(String batchId) {
        return siteOrderRepo.findByBatch(batchId);
    }

    public List<SiteOrder> getBySite(String siteCode) {
        return siteOrderRepo.findBySite(siteCode);
    }

    public List<SiteOrder> getAll() {
        return siteOrderRepo.findAll();
    }

    public List<SiteOrder> getCancelRequests() {
        return siteOrderRepo.findCancelRequests();
    }

    public List<FinalOrder> getItemsOfSiteOrder(String siteOrderId) {
        return finalOrderRepo.findBySiteOrder(siteOrderId);
    }

    // OOD sửa đơn hàng
    public void editSiteOrder(String siteOrderId, String deliveryMeans) {
        SiteOrder so = siteOrderRepo.findBySiteOrderId(siteOrderId);
        if (so == null) throw new AppException("Không tìm thấy đơn: " + siteOrderId);
        if (!"SENT".equals(so.getStatus()))
            throw new AppException("Chỉ có thể sửa đơn đang ở trạng thái SENT.");
        if (so.isConfirmedBySite())
            throw new AppException("Site đã xác nhận đơn này, không thể sửa.");

        siteOrderRepo.updateStatus(siteOrderId, so.getStatus()); // giữ status
        // Cập nhật deliveryMeans nếu cần — mở rộng sau
    }

    // OOD hủy đơn hàng
    public void cancelSiteOrder(String siteOrderId, String reason) {
        if (reason == null || reason.isBlank())
            throw new AppException("Lý do hủy không được để trống.");

        SiteOrder so = siteOrderRepo.findBySiteOrderId(siteOrderId);
        if (so == null) throw new AppException("Không tìm thấy đơn: " + siteOrderId);
        if ("RECEIVED".equals(so.getStatus()) || "PARTIALLY_RECEIVED".equals(so.getStatus()))
            throw new AppException("Hàng đã về, không thể hủy đơn này.");
        if ("CANCELLED".equals(so.getStatus()))
            throw new AppException("Đơn này đã bị hủy trước đó.");

        siteOrderRepo.updateStatus(siteOrderId, "CANCELLED");
        finalOrderRepo.cancelBySiteOrder(siteOrderId);
    }

    // OOD duyệt yêu cầu hủy từ Site
    public void approveCancelRequest(String siteOrderId) {
        SiteOrder so = siteOrderRepo.findBySiteOrderId(siteOrderId);
        if (so == null) throw new AppException("Không tìm thấy đơn: " + siteOrderId);
        if (so.getCancelRequestedAt() == null)
            throw new AppException("Đơn này không có yêu cầu hủy.");
        if ("RECEIVED".equals(so.getStatus()))
            throw new AppException("Hàng đã được nhận, không thể duyệt hủy.");

        siteOrderRepo.approveCancel(siteOrderId, SessionManager.getUsername());
        finalOrderRepo.cancelBySiteOrder(siteOrderId);
    }

    // OOD từ chối yêu cầu hủy từ Site — chỉ xóa cancelRequestedAt
    public void rejectCancelRequest(String siteOrderId) {
        SiteOrder so = siteOrderRepo.findBySiteOrderId(siteOrderId);
        if (so == null) throw new AppException("Không tìm thấy đơn: " + siteOrderId);
        // Reset cancel request fields
        siteOrderRepo.rejectCancelRequest(siteOrderId);
    }

    // Site xác nhận nhận đơn
    public void confirmBySite(String siteOrderId) {
        SiteOrder so = siteOrderRepo.findBySiteOrderId(siteOrderId);
        if (so == null) throw new AppException("Không tìm thấy đơn: " + siteOrderId);
        if ("CANCELLED".equals(so.getStatus()))
            throw new AppException("Đơn này đã bị hủy bởi OOD.");
        if (so.isConfirmedBySite())
            throw new AppException("Đơn này đã được xác nhận trước đó.");

        siteOrderRepo.confirmBySite(siteOrderId);
    }

    // Site gửi yêu cầu hủy
    public void requestCancel(String siteOrderId, String reason) {
        if (reason == null || reason.isBlank())
            throw new AppException("Lý do yêu cầu hủy không được để trống.");

        SiteOrder so = siteOrderRepo.findBySiteOrderId(siteOrderId);
        if (so == null) throw new AppException("Không tìm thấy đơn: " + siteOrderId);
        if ("CANCELLED".equals(so.getStatus()))
            throw new AppException("Đơn này đã bị hủy.");

        siteOrderRepo.requestCancel(siteOrderId, reason);
    }
}