package com.importorder.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.importorder.model.SiteOrder;
import com.importorder.fakes.FakeSiteOrderRepository;
import com.importorder.util.AppException;

public class UC1_ConfirmImportTest {

    private FakeSiteOrderRepository fakeSiteOrderRepo;
    private SiteOrderService siteOrderService;

    private final String SITE_ORDER_ID = "SO-123";
    private SiteOrder mockOrder;

    @BeforeEach
    void setUp() {
        fakeSiteOrderRepo = new FakeSiteOrderRepository();

        // Tiêm các Fake repo vào Service, các tham số không dùng trong test này có thể truyền null
        siteOrderService = new SiteOrderService(fakeSiteOrderRepo, null, null, null, null);

        mockOrder = new SiteOrder();
        mockOrder.setSiteOrderId(SITE_ORDER_ID);
    }

    // =========================================================================
    // UC1-WB-01: Không tìm thấy đơn hàng (so == null)
    // =========================================================================
    @Test
    void testConfirmBySite_TC01_OrderNotFound() {
        // Không lưu order nào vào Fake repo
        AppException ex = assertThrows(AppException.class,
                () -> siteOrderService.confirmBySite(SITE_ORDER_ID));

        assertTrue(ex.getMessage().contains("Không tìm thấy đơn"));
    }

    // =========================================================================
    // UC1-WB-02: Đơn hàng đã bị hủy (status = CANCELLED)
    // =========================================================================
    @Test
    void testConfirmBySite_TC02_AlreadyCancelled() {
        mockOrder.setStatus("CANCELLED");
        fakeSiteOrderRepo.save(mockOrder);

        AppException ex = assertThrows(AppException.class,
                () -> siteOrderService.confirmBySite(SITE_ORDER_ID));

        assertEquals("Đơn này đã bị hủy bởi OOD.", ex.getMessage());
    }

    // =========================================================================
    // UC1-WB-03: Đơn hàng đã được xác nhận trước đó (isConfirmedBySite = true)
    // =========================================================================
    @Test
    void testConfirmBySite_TC03_AlreadyConfirmed() {
        mockOrder.setStatus("SENT");
        mockOrder.setConfirmedBySite(true);
        fakeSiteOrderRepo.save(mockOrder);

        AppException ex = assertThrows(AppException.class,
                () -> siteOrderService.confirmBySite(SITE_ORDER_ID));

        assertEquals("Đơn này đã được xác nhận trước đó.", ex.getMessage());
    }

    // =========================================================================
    // UC1-WB-04: Đơn hàng có trạng thái không hợp lệ (ví dụ: RECEIVED)
    // =========================================================================
    @Test
    void testConfirmBySite_TC04_InvalidState() {
        mockOrder.setStatus("RECEIVED");
        mockOrder.setConfirmedBySite(false);
        fakeSiteOrderRepo.save(mockOrder);

        AppException ex = assertThrows(AppException.class,
                () -> siteOrderService.confirmBySite(SITE_ORDER_ID));

        assertEquals("Chỉ có thể xác nhận đơn ở trạng thái SENT.", ex.getMessage());
    }

    // =========================================================================
    // UC1-WB-05: Xác nhận thành công đơn hàng (status = SENT, isConfirmedBySite = false)
    // =========================================================================
    @Test
    void testConfirmBySite_TC05_Success() {
        mockOrder.setStatus("SENT");
        mockOrder.setConfirmedBySite(false);
        fakeSiteOrderRepo.save(mockOrder);

        assertDoesNotThrow(() -> siteOrderService.confirmBySite(SITE_ORDER_ID));

        // Kiểm tra trực tiếp trong Fake Repo xem dữ liệu đã được update đúng chưa
        SiteOrder updatedOrder = fakeSiteOrderRepo.findBySiteOrderId(SITE_ORDER_ID);
        assertTrue(updatedOrder.isConfirmedBySite());
        assertEquals("CONFIRMED", updatedOrder.getStatus());
        assertNotNull(updatedOrder.getConfirmedAt());
    }
}
