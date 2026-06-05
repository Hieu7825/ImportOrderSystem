package com.importorder.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.importorder.model.*;
import com.importorder.fakes.*;
import com.importorder.util.AppException;
import com.importorder.util.SessionManager;

import java.util.*;

public class UC6_ProcessImportRequestTest {

    private FakeSiteRepository fakeSiteRepo;
    private FakeStockRepository fakeStockRepo;
    private FakeOrderRequestRepository fakeOrderRepo;
    private FakeSiteOrderRepository fakeSiteOrderRepo;
    private FakeFinalOrderRepository fakeFinalOrderRepo;
    private FakeSubBatchRepository fakeSubBatchRepo;

    private OrderOptimizationService optimService;

    private final String BATCH_ID = "B-123";
    private OrderRequest mockRequest;

    @BeforeEach
    void setUp() {
        fakeSiteRepo = new FakeSiteRepository();
        fakeStockRepo = new FakeStockRepository();
        fakeOrderRepo = new FakeOrderRequestRepository();
        fakeSiteOrderRepo = new FakeSiteOrderRepository();
        fakeFinalOrderRepo = new FakeFinalOrderRepository();
        fakeSubBatchRepo = new FakeSubBatchRepository();

        optimService = new OrderOptimizationService(
                fakeSiteRepo, fakeStockRepo, fakeOrderRepo, 
                fakeSiteOrderRepo, fakeFinalOrderRepo, fakeSubBatchRepo);

        mockRequest = new OrderRequest();
        mockRequest.setBatchId(BATCH_ID);
        mockRequest.setStatus("PENDING");
        mockRequest.setItems(new ArrayList<>());
    }

    // =========================================================================
    // UC6-WB-01: Batch không tồn tại trong DB (req == null)
    // =========================================================================
    @Test
    void testStartProcessing_TC01_BatchNotFound() {
        AppException ex = assertThrows(AppException.class,
                () -> optimService.startProcessing(BATCH_ID));

        assertTrue(ex.getMessage().contains("Không tìm thấy batch"));
    }

    // =========================================================================
    // UC6-WB-02: Batch không ở trạng thái PENDING
    // =========================================================================
    @Test
    void testStartProcessing_TC02_BatchNotPending() {
        mockRequest.setStatus("PROCESSING");
        fakeOrderRepo.save(mockRequest);

        AppException ex = assertThrows(AppException.class,
                () -> optimService.startProcessing(BATCH_ID));

        assertTrue(ex.getMessage().contains("không ở trạng thái PENDING"));
    }

    // =========================================================================
    // UC6-WB-03: Đã tồn tại sub-batch PROCESSING
    // =========================================================================
    @Test
    void testStartProcessing_TC03_SubBatchAlreadyProcessing() {
        fakeOrderRepo.save(mockRequest);

        SubBatch existingSb = new SubBatch();
        existingSb.setSubBatchId("SB-EXIST");
        existingSb.setParentBatchId(BATCH_ID);
        existingSb.setStatus("PROCESSING");
        fakeSubBatchRepo.save(existingSb);

        SubBatch result = optimService.startProcessing(BATCH_ID);

        assertNotNull(result);
        assertEquals("SB-EXIST", result.getSubBatchId());
        
        // PENDING -> should not be updated to PROCESSING because we returned early
        assertEquals("PENDING", fakeOrderRepo.findByBatchId(BATCH_ID).getStatus());
    }

    // =========================================================================
    // UC6-WB-04: Khởi tạo mới sub-batch PROCESSING (thành công)
    // =========================================================================
    @Test
    void testStartProcessing_TC04_CreateNewSubBatch() {
        fakeOrderRepo.save(mockRequest);

        // Có sub-batch cũ nhưng status = CANCELLED
        SubBatch oldSb = new SubBatch();
        oldSb.setSubBatchId("SB-OLD");
        oldSb.setParentBatchId(BATCH_ID);
        oldSb.setStatus("CANCELLED");
        fakeSubBatchRepo.save(oldSb);

        User testUser = new User();
        testUser.setUsername("test-user");
        SessionManager.login(testUser);

        SubBatch result = optimService.startProcessing(BATCH_ID);

        assertNotNull(result);
        assertTrue(result.getSubBatchId().startsWith("SUB-" + BATCH_ID));
        assertEquals("ORIGINAL", result.getType());
        assertEquals("PROCESSING", result.getStatus());
        assertEquals("test-user", result.getCreatedBy());

        assertEquals("PROCESSING", fakeOrderRepo.findByBatchId(BATCH_ID).getStatus());
        assertNotNull(fakeSubBatchRepo.findByParentBatch(BATCH_ID).stream()
                .filter(sb -> "PROCESSING".equals(sb.getStatus())).findFirst().orElse(null));

        SessionManager.logout();
    }
}
