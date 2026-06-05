package com.importorder.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.importorder.fakes.FakeMerchandiseRepository;
import com.importorder.fakes.FakeOrderRequestRepository;
import com.importorder.model.Merchandise;
import com.importorder.model.OrderItem;
import com.importorder.model.OrderRequest;
import com.importorder.util.AppException;

public class UC2_EditOrder_BlackBoxTest {

    private OrderRequestService service;
    private FakeMerchandiseRepository fakeMerchRepo;
    private FakeOrderRequestRepository fakeOrderRepo;

    @BeforeEach
    void setUp() {
        fakeMerchRepo = new FakeMerchandiseRepository();
        fakeOrderRepo = new FakeOrderRequestRepository();

        Merchandise m1 = new Merchandise();
        m1.setItemCode("ITEM-001");
        fakeMerchRepo.save(m1);

        OrderRequest pendingReq = new OrderRequest();
        pendingReq.setBatchId("BATCH-PENDING");
        pendingReq.setStatus("PENDING");
        fakeOrderRepo.save(pendingReq);

        OrderRequest processingReq = new OrderRequest();
        processingReq.setBatchId("BATCH-PROCESSING");
        processingReq.setStatus("PROCESSING");
        fakeOrderRepo.save(processingReq);

        service = new OrderRequestService(fakeOrderRepo, fakeMerchRepo);
    }

    static class TestCase {
        String name;
        String batchId;
        List<OrderItem> newItems;

        public TestCase(String name, String batchId, List<OrderItem> newItems) {
            this.name = name;
            this.batchId = batchId;
            this.newItems = newItems;
        }
        @Override
        public String toString() { return name; }
    }

    static Stream<TestCase> invalidInputsProvider() {
        OrderItem validItem = new OrderItem();
        validItem.setItemCode("ITEM-001");
        validItem.setQuantityOrdered(10);
        validItem.setUnit("Cai");
        validItem.setDesiredDeliveryDate(LocalDate.now().plusDays(2));

        return Stream.of(
            new TestCase("Batch null", null, List.of(validItem)),
            new TestCase("Batch rong", "", List.of(validItem)),
            new TestCase("Batch khong ton tai", "BATCH-NOT-EXIST", List.of(validItem)),
            new TestCase("Batch dang PROCESSING", "BATCH-PROCESSING", List.of(validItem)),
            new TestCase("Items rong", "BATCH-PENDING", new ArrayList<>())
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidInputsProvider")
    @DisplayName("Kiem thu Hop den: Sua don hang KHONG HOP LE")
    void testUpdate_InvalidInputs(TestCase tc) {
        assertThrows(AppException.class, () -> {
            service.updateRequest(tc.batchId, tc.newItems);
        });
    }

    static Stream<TestCase> validInputsProvider() {
        OrderItem validItem = new OrderItem();
        validItem.setItemCode("ITEM-001");
        validItem.setQuantityOrdered(10);
        validItem.setUnit("Cai");
        validItem.setDesiredDeliveryDate(LocalDate.now().plusDays(2));

        return Stream.of(
            new TestCase("Sua don hang hop le", "BATCH-PENDING", List.of(validItem))
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validInputsProvider")
    @DisplayName("Kiem thu Hop den: Sua don hang HOP LE")
    void testUpdate_ValidInputs(TestCase tc) {
        assertDoesNotThrow(() -> {
            service.updateRequest(tc.batchId, tc.newItems);
        });
    }
}
