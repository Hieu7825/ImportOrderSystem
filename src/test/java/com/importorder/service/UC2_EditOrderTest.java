package com.importorder.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.importorder.model.SiteOrder;
import com.importorder.model.SiteInfo;
import com.importorder.fakes.FakeSiteOrderRepository;
import com.importorder.fakes.FakeSiteRepository;
import com.importorder.util.AppException;

public class UC2_EditOrderTest {

    private FakeSiteOrderRepository fakeSiteOrderRepo;
    private FakeSiteRepository fakeSiteRepo;
    private SiteOrderService siteOrderService;

    private final String SITE_ORDER_ID = "SO-123";
    private SiteOrder mockOrder;

    @BeforeEach
    void setUp() {
        fakeSiteOrderRepo = new FakeSiteOrderRepository();
        fakeSiteRepo = new FakeSiteRepository();

        siteOrderService = new SiteOrderService(fakeSiteOrderRepo, null, null, null, fakeSiteRepo);

        mockOrder = new SiteOrder();
        mockOrder.setSiteOrderId(SITE_ORDER_ID);
    }

    // =========================================================================
    // UC2-WB-01: Không tìm thấy đơn hàng (so == null)
    // =========================================================================
    @Test
    void testEditSiteOrder_TC01_OrderNotFound() {
        AppException ex = assertThrows(AppException.class,
                () -> siteOrderService.editSiteOrder(SITE_ORDER_ID, "ship"));

        assertTrue(ex.getMessage().contains("Không tìm thấy đơn"));
    }

    // =========================================================================
    // UC2-WB-02: Đơn hàng không ở trạng thái SENT
    // =========================================================================
    @Test
    void testEditSiteOrder_TC02_InvalidStatus() {
        mockOrder.setStatus("RECEIVED");
        fakeSiteOrderRepo.save(mockOrder);

        AppException ex = assertThrows(AppException.class,
                () -> siteOrderService.editSiteOrder(SITE_ORDER_ID, "ship"));

        assertEquals("Chỉ có thể sửa đơn đang ở trạng thái SENT.", ex.getMessage());
    }

    // =========================================================================
    // UC2-WB-03: Đơn hàng đã được site xác nhận (isConfirmedBySite = true)
    // =========================================================================
    @Test
    void testEditSiteOrder_TC03_AlreadyConfirmed() {
        mockOrder.setStatus("SENT");
        mockOrder.setConfirmedBySite(true);
        fakeSiteOrderRepo.save(mockOrder);

        AppException ex = assertThrows(AppException.class,
                () -> siteOrderService.editSiteOrder(SITE_ORDER_ID, "ship"));

        assertTrue(ex.getMessage().contains("Site đã xác nhận đơn này"));
    }

    // =========================================================================
    // UC2-WB-04: Không tìm thấy thông tin chi nhánh (site == null)
    // =========================================================================
    @Test
    void testEditSiteOrder_TC04_SiteNotFound() {
        mockOrder.setStatus("SENT");
        mockOrder.setConfirmedBySite(false);
        mockOrder.setSiteCode("SITE-XYZ");
        fakeSiteOrderRepo.save(mockOrder);
        // không save SITE-XYZ vào fakeSiteRepo

        AppException ex = assertThrows(AppException.class,
                () -> siteOrderService.editSiteOrder(SITE_ORDER_ID, "ship"));

        assertEquals("Không tìm thấy thông tin site: SITE-XYZ", ex.getMessage());
    }

    // =========================================================================
    // UC2-WB-05: Chỉnh sửa thành công sang "ship"
    // =========================================================================
    @Test
    void testEditSiteOrder_TC05_SuccessShip() {
        mockOrder.setStatus("SENT");
        mockOrder.setConfirmedBySite(false);
        mockOrder.setSiteCode("SITE-A");
        fakeSiteOrderRepo.save(mockOrder);
        
        SiteInfo siteInfo = new SiteInfo();
        siteInfo.setSiteCode("SITE-A");
        siteInfo.setShipDays(15);
        siteInfo.setAirDays(3);
        fakeSiteRepo.save(siteInfo);

        assertDoesNotThrow(() -> siteOrderService.editSiteOrder(SITE_ORDER_ID, "ship"));

        SiteOrder updatedOrder = fakeSiteOrderRepo.findBySiteOrderId(SITE_ORDER_ID);
        assertEquals("ship", updatedOrder.getDeliveryMeans());
        assertNotNull(updatedOrder.getEstimatedArrival());
    }

    // =========================================================================
    // UC2-WB-06: Chỉnh sửa thành công sang "air"
    // =========================================================================
    @Test
    void testEditSiteOrder_TC06_SuccessAir() {
        mockOrder.setStatus("SENT");
        mockOrder.setConfirmedBySite(false);
        mockOrder.setSiteCode("SITE-A");
        fakeSiteOrderRepo.save(mockOrder);
        
        SiteInfo siteInfo = new SiteInfo();
        siteInfo.setSiteCode("SITE-A");
        siteInfo.setShipDays(15);
        siteInfo.setAirDays(3);
        fakeSiteRepo.save(siteInfo);

        assertDoesNotThrow(() -> siteOrderService.editSiteOrder(SITE_ORDER_ID, "air"));

        SiteOrder updatedOrder = fakeSiteOrderRepo.findBySiteOrderId(SITE_ORDER_ID);
        assertEquals("air", updatedOrder.getDeliveryMeans());
        assertNotNull(updatedOrder.getEstimatedArrival());
    }
}
