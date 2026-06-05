package com.importorder.service;

import com.importorder.model.OrderItem;
import com.importorder.model.OrderRequest;
import com.importorder.repository.MerchandiseRepository;
import com.importorder.repository.OrderRequestRepository;
import com.importorder.util.AppException;
import com.importorder.util.DateUtils;
import com.importorder.util.SessionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OrderRequestService {

    private final OrderRequestRepository orderRepo;
    private final MerchandiseRepository  merchRepo;

    public OrderRequestService() {
        this.orderRepo = new OrderRequestRepository();
        this.merchRepo = new MerchandiseRepository();
    }

    public OrderRequestService(OrderRequestRepository orderRepo, MerchandiseRepository merchRepo) {
        this.orderRepo = orderRepo;
        this.merchRepo = merchRepo;
    }

    public OrderRequest createRequest(List<OrderItem> items) {
        validateItems(items);

        OrderRequest req = new OrderRequest();
        req.setBatchId(DateUtils.generateBatchId());
        req.setCreatedBy(SessionManager.getUsername());
        req.setCreatedAt(LocalDateTime.now());
        req.setUpdatedAt(LocalDateTime.now());
        req.setStatus("PENDING");
        req.setItems(items);

        orderRepo.save(req);
        return req;
    }

    public void updateRequest(String batchId, List<OrderItem> newItems) {
        OrderRequest existing = orderRepo.findByBatchId(batchId);
        if (existing == null)
            throw new AppException("Không tìm thấy batch: " + batchId);
        if (!"PENDING".equals(existing.getStatus()))
            throw new AppException.InvalidOrderStateException(
                "Batch " + batchId + " đang ở trạng thái "
                + existing.getStatus() + ", không thể sửa.");

        validateItems(newItems);
        orderRepo.update(existing.getBatchId(), newItems);
    }

    public void cancelRequest(String batchId, String reason) {
        if (reason == null || reason.isBlank())
            throw new AppException("Lý do hủy không được để trống.");

        OrderRequest req = orderRepo.findByBatchId(batchId);
        if (req == null)
            throw new AppException("Không tìm thấy batch: " + batchId);

        String role        = SessionManager.getRole();
        String currentUser = SessionManager.getUsername();

        if ("SD".equals(role)) {
            if (!currentUser.equals(req.getCreatedBy()))
                throw new AppException("Bạn không có quyền hủy yêu cầu này.");

            // Edge Case A1: SD không thể hủy batch đang PROCESSING
            if ("PROCESSING".equals(req.getStatus()))
                throw new AppException(
                    "Batch đang được OOD xử lý (PROCESSING). " +
                    "Vui lòng liên hệ bộ phận đặt hàng quốc tế để hủy.");

            if (!"PENDING".equals(req.getStatus()))
                throw new AppException.OrderNotCancellableException(batchId, req.getStatus());
        }

        if ("OOD".equals(role)) {
            if ("COMPLETED".equals(req.getStatus()))
                throw new AppException.OrderNotCancellableException(batchId, req.getStatus());
        }

        orderRepo.cancel(batchId, currentUser, reason);
    }

    /**
     * Lấy danh sách batch cho user hiện tại.
     * SD → chỉ thấy batch của mình.
     * OOD → thấy tất cả.
     * displayStatus = status của sub-batch mới nhất.
     */
    public List<OrderRequest> getRequestsForCurrentUser() {
        String role = SessionManager.getRole();
        List<OrderRequest> list;
        if ("SD".equals(role))
            list = orderRepo.findByCreatedBy(SessionManager.getUsername());
        else
            list = orderRepo.findAll();
        return list;
    }

    public List<OrderRequest> getByStatus(String status) {
        return orderRepo.findByStatus(status);
    }

    public OrderRequest getByBatchId(String batchId) {
        return orderRepo.findByBatchId(batchId);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validateItems(List<OrderItem> items) {
        // Edge Case A5: batch rỗng
        if (items == null || items.isEmpty())
            throw new AppException("Danh sách mặt hàng không được để trống.");

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            OrderItem item = items.get(i);
            if (item.getItemCode() == null || item.getItemCode().isBlank())
                throw new AppException("Dòng " + (i+1) + ": Mã hàng không được để trống.");
            if (item.getQuantityOrdered() <= 0)
                throw new AppException("Dòng " + (i+1) + ": Số lượng phải lớn hơn 0.");
            if (item.getUnit() == null || item.getUnit().isBlank())
                throw new AppException("Dòng " + (i+1) + ": Đơn vị không được để trống.");
            if (item.getDesiredDeliveryDate() == null ||
                !item.getDesiredDeliveryDate().isAfter(LocalDate.now()))
                throw new AppException(
                    "Dòng " + (i+1) + ": Ngày nhận phải sau hôm nay ít nhất 1 ngày.");
            if (!seen.add(item.getItemCode()))
                throw new AppException(
                    "Mã hàng " + item.getItemCode() + " bị trùng trong danh sách.");
            if (merchRepo.findByCode(item.getItemCode()) == null)
                throw new AppException(
                    "Mã hàng " + item.getItemCode() + " không tồn tại trong hệ thống.");
        }
    }
}