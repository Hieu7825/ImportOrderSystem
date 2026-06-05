package com.importorder.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.importorder.model.*;
import com.importorder.fakes.*;
import com.importorder.util.AppException;
import com.importorder.util.SessionManager;

import java.util.*;

public class UC5_InventoryInspectionTest {

    private FakeSiteOrderRepository fakeSiteOrderRepo;
    private FakeFinalOrderRepository fakeFinalOrderRepo;
    private FakeDiscrepancyRepository fakeDiscrepancyRepo;

    private WarehouseService warehouseService;

    private final String SITE_ORDER_ID = "SO-123";
    private SiteOrder mockOrder;
    private Map<String, Map<String, Object>> inspectionData;

    @BeforeEach
    void setUp() {
        fakeSiteOrderRepo = new FakeSiteOrderRepository();
        fakeFinalOrderRepo = new FakeFinalOrderRepository();
        fakeDiscrepancyRepo = new FakeDiscrepancyRepository();

        warehouseService = new WarehouseService(fakeSiteOrderRepo, fakeFinalOrderRepo, fakeDiscrepancyRepo);

        mockOrder = new SiteOrder();
        mockOrder.setSiteOrderId(SITE_ORDER_ID);
        mockOrder.setStatus("SENT");
        
        inspectionData = new HashMap<>();
    }

    // =========================================================================
    // UC5-WB-01: Đơn hàng kiểm tra không tồn tại
    // =========================================================================
    @Test
    void testSubmitInspection_TC01_OrderNotFound() {
        AppException ex = assertThrows(AppException.class,
                () -> warehouseService.submitInspection(SITE_ORDER_ID, inspectionData));

        assertTrue(ex.getMessage().contains("Không tìm thấy đơn"));
    }

    // =========================================================================
    // UC5-WB-02: Dữ liệu kiểm hàng trống hoàn toàn (totalCount = 0)
    // =========================================================================
    @Test
    void testSubmitInspection_TC02_EmptyInspectionData() {
        fakeSiteOrderRepo.save(mockOrder);

        FinalOrder fo = new FinalOrder();
        fo.setSiteOrderId(SITE_ORDER_ID);
        fo.setItemCode("M-001");
        fo.setQuantityOrdered(10);
        fo.setUnit("box");
        fakeFinalOrderRepo.save(fo);

        assertDoesNotThrow(() -> warehouseService.submitInspection(SITE_ORDER_ID, inspectionData));

        SiteOrder updatedOrder = fakeSiteOrderRepo.findBySiteOrderId(SITE_ORDER_ID);
        assertEquals("SENT", updatedOrder.getStatus());
    }

    // =========================================================================
    // UC5-WB-03: Kiểm hàng một phần (totalCount < orders.size())
    // =========================================================================
    @Test
    void testSubmitInspection_TC03_PartialReceipt() {
        fakeSiteOrderRepo.save(mockOrder);

        FinalOrder fo1 = new FinalOrder();
        fo1.setSiteOrderId(SITE_ORDER_ID);
        fo1.setItemCode("M-001");
        fo1.setQuantityOrdered(10);
        fo1.setUnit("box");
        
        FinalOrder fo2 = new FinalOrder();
        fo2.setSiteOrderId(SITE_ORDER_ID);
        fo2.setItemCode("M-002");
        fo2.setQuantityOrdered(5);
        fo2.setUnit("box");

        fakeFinalOrderRepo.save(fo1);
        fakeFinalOrderRepo.save(fo2);

        Map<String, Object> data = new HashMap<>();
        data.put("actualQty", 10);
        data.put("actualUnit", "box");
        data.put("itemCodeReceived", "M-001");
        data.put("description", "");
        inspectionData.put("M-001", data);

        assertDoesNotThrow(() -> warehouseService.submitInspection(SITE_ORDER_ID, inspectionData));

        SiteOrder updatedOrder = fakeSiteOrderRepo.findBySiteOrderId(SITE_ORDER_ID);
        assertEquals("PARTIALLY_RECEIVED", updatedOrder.getStatus());
        
        assertEquals("RECEIVED", fo1.getStatus());
    }

    // =========================================================================
    // UC5-WB-04: Thực nhận = 0 nhưng mô tả trống
    // =========================================================================
    @Test
    void testSubmitInspection_TC04_ZeroQtyBlankDesc() {
        fakeSiteOrderRepo.save(mockOrder);

        FinalOrder fo = new FinalOrder();
        fo.setSiteOrderId(SITE_ORDER_ID);
        fo.setItemCode("M-001");
        fo.setQuantityOrdered(10);
        fo.setUnit("box");
        fakeFinalOrderRepo.save(fo);

        Map<String, Object> data = new HashMap<>();
        data.put("actualQty", 0);
        data.put("actualUnit", "box");
        data.put("itemCodeReceived", "M-001");
        data.put("description", "  ");
        inspectionData.put("M-001", data);

        AppException ex = assertThrows(AppException.class,
                () -> warehouseService.submitInspection(SITE_ORDER_ID, inspectionData));

        assertTrue(ex.getMessage().contains("số lượng thực nhận = 0, vui lòng điền ghi chú."));
    }

    // =========================================================================
    // UC5-WB-05: Sai lệch mã hàng nhận (ERR-ITEM)
    // =========================================================================
    @Test
    void testSubmitInspection_TC05_ItemCodeDiscrepancy() {
        fakeSiteOrderRepo.save(mockOrder);

        FinalOrder fo = new FinalOrder();
        fo.setSiteOrderId(SITE_ORDER_ID);
        fo.setItemCode("M-001");
        fo.setQuantityOrdered(10);
        fo.setUnit("box");
        fakeFinalOrderRepo.save(fo);

        Map<String, Object> data = new HashMap<>();
        data.put("actualQty", 10);
        data.put("actualUnit", "box");
        data.put("itemCodeReceived", "M-DIFF"); // sai mã
        data.put("description", "Giao nhầm mã");
        inspectionData.put("M-001", data);

        User testUser = new User();
        testUser.setUsername("test-user");
        SessionManager.login(testUser);

        assertDoesNotThrow(() -> warehouseService.submitInspection(SITE_ORDER_ID, inspectionData));

        SiteOrder updatedOrder = fakeSiteOrderRepo.findBySiteOrderId(SITE_ORDER_ID);
        assertEquals("DISCREPANCY", updatedOrder.getStatus());
        assertEquals("DISCREPANCY", fo.getStatus());
        
        List<WarehouseDiscrepancy> list = fakeDiscrepancyRepo.findBySiteOrder(SITE_ORDER_ID);
        assertEquals(1, list.size());
        assertEquals("ERR-ITEM", list.get(0).getErrorCode());

        SessionManager.logout();
    }

    // =========================================================================
    // UC5-WB-06: Sai lệch đơn vị (ERR-UNIT)
    // =========================================================================
    @Test
    void testSubmitInspection_TC06_UnitDiscrepancy() {
        fakeSiteOrderRepo.save(mockOrder);

        FinalOrder fo = new FinalOrder();
        fo.setSiteOrderId(SITE_ORDER_ID);
        fo.setItemCode("M-001");
        fo.setQuantityOrdered(10);
        fo.setUnit("box");
        fakeFinalOrderRepo.save(fo);

        Map<String, Object> data = new HashMap<>();
        data.put("actualQty", 10);
        data.put("actualUnit", "pack"); // sai đơn vị
        data.put("itemCodeReceived", "M-001");
        data.put("description", "Giao sai đơn vị");
        inspectionData.put("M-001", data);

        User testUser = new User();
        testUser.setUsername("test-user");
        SessionManager.login(testUser);

        assertDoesNotThrow(() -> warehouseService.submitInspection(SITE_ORDER_ID, inspectionData));

        assertEquals("DISCREPANCY", fo.getStatus());
        assertEquals("DISCREPANCY", fakeSiteOrderRepo.findBySiteOrderId(SITE_ORDER_ID).getStatus());

        SessionManager.logout();
    }

    // =========================================================================
    // UC5-WB-07: Nhận thiếu hàng (Qty = 0 có ghi chú - ERR-MISSING)
    // =========================================================================
    @Test
    void testSubmitInspection_TC07_MissingDiscrepancy() {
        fakeSiteOrderRepo.save(mockOrder);

        FinalOrder fo = new FinalOrder();
        fo.setSiteOrderId(SITE_ORDER_ID);
        fo.setItemCode("M-001");
        fo.setQuantityOrdered(10);
        fo.setUnit("box");
        fakeFinalOrderRepo.save(fo);

        Map<String, Object> data = new HashMap<>();
        data.put("actualQty", 0);
        data.put("actualUnit", "box");
        data.put("itemCodeReceived", "M-001");
        data.put("description", "Thiếu hàng");
        inspectionData.put("M-001", data);

        User testUser = new User();
        testUser.setUsername("test-user");
        SessionManager.login(testUser);

        assertDoesNotThrow(() -> warehouseService.submitInspection(SITE_ORDER_ID, inspectionData));

        assertEquals("DISCREPANCY", fo.getStatus());
        assertEquals("DISCREPANCY", fakeSiteOrderRepo.findBySiteOrderId(SITE_ORDER_ID).getStatus());

        SessionManager.logout();
    }

    // =========================================================================
    // UC5-WB-08: Sai lệch số lượng (ERR-QTY)
    // =========================================================================
    @Test
    void testSubmitInspection_TC08_QtyDiscrepancy() {
        fakeSiteOrderRepo.save(mockOrder);

        FinalOrder fo = new FinalOrder();
        fo.setSiteOrderId(SITE_ORDER_ID);
        fo.setItemCode("M-001");
        fo.setQuantityOrdered(10);
        fo.setUnit("box");
        fakeFinalOrderRepo.save(fo);

        Map<String, Object> data = new HashMap<>();
        data.put("actualQty", 8); // sai lệch số lượng
        data.put("actualUnit", "box");
        data.put("itemCodeReceived", "M-001");
        data.put("description", "Thiếu 2 hộp");
        inspectionData.put("M-001", data);

        User testUser = new User();
        testUser.setUsername("test-user");
        SessionManager.login(testUser);

        assertDoesNotThrow(() -> warehouseService.submitInspection(SITE_ORDER_ID, inspectionData));

        assertEquals("DISCREPANCY", fo.getStatus());
        assertEquals("DISCREPANCY", fakeSiteOrderRepo.findBySiteOrderId(SITE_ORDER_ID).getStatus());

        SessionManager.logout();
    }

    // =========================================================================
    // UC5-WB-09: Khớp hoàn toàn không lỗi (RECEIVED)
    // =========================================================================
    @Test
    void testSubmitInspection_TC09_SuccessReceived() {
        fakeSiteOrderRepo.save(mockOrder);

        FinalOrder fo = new FinalOrder();
        fo.setSiteOrderId(SITE_ORDER_ID);
        fo.setItemCode("M-001");
        fo.setQuantityOrdered(10);
        fo.setUnit("box");
        fakeFinalOrderRepo.save(fo);

        Map<String, Object> data = new HashMap<>();
        data.put("actualQty", 10);
        data.put("actualUnit", "box");
        data.put("itemCodeReceived", "M-001");
        data.put("description", "");
        inspectionData.put("M-001", data);

        assertDoesNotThrow(() -> warehouseService.submitInspection(SITE_ORDER_ID, inspectionData));

        assertEquals("RECEIVED", fo.getStatus());
        assertEquals("RECEIVED", fakeSiteOrderRepo.findBySiteOrderId(SITE_ORDER_ID).getStatus());
    }
}
