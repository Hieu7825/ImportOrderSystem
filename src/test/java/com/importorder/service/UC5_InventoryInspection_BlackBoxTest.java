package com.importorder.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.importorder.fakes.FakeDiscrepancyRepository;
import com.importorder.fakes.FakeFinalOrderRepository;
import com.importorder.fakes.FakeSiteOrderRepository;
import com.importorder.model.FinalOrder;
import com.importorder.model.SiteOrder;
import com.importorder.util.AppException;

public class UC5_InventoryInspection_BlackBoxTest {

    private WarehouseService service;
    private FakeSiteOrderRepository fakeSiteRepo;
    private FakeFinalOrderRepository fakeFinalRepo;
    private FakeDiscrepancyRepository fakeDiscRepo;

    @BeforeEach
    void setUp() {
        fakeSiteRepo = new FakeSiteOrderRepository();
        fakeFinalRepo = new FakeFinalOrderRepository();
        fakeDiscRepo = new FakeDiscrepancyRepository();
        service = new WarehouseService(fakeSiteRepo, fakeFinalRepo, fakeDiscRepo);

        SiteOrder so = new SiteOrder();
        so.setSiteOrderId("SO-DELIVERED");
        so.setStatus("DELIVERED");
        fakeSiteRepo.save(so);

        FinalOrder fo = new FinalOrder();
        fo.setSiteOrderId("SO-DELIVERED");
        fo.setItemCode("ITEM-001");
        fo.setQuantityOrdered(10);
        fo.setUnit("Cai");
        fakeFinalRepo.save(fo);
    }

    static class TestCase {
        String name;
        String siteOrderId;
        Map<String, Map<String, Object>> inspectionData;

        public TestCase(String name, String siteOrderId, Map<String, Map<String, Object>> inspectionData) {
            this.name = name;
            this.siteOrderId = siteOrderId;
            this.inspectionData = inspectionData;
        }
        @Override
        public String toString() { return name; }
    }

    static Stream<TestCase> invalidInputsProvider() {
        Map<String, Map<String, Object>> validData = new HashMap<>();
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("actualQty", 10);
        validData.put("ITEM-001", itemData);

        Map<String, Map<String, Object>> zeroQtyNoDesc = new HashMap<>();
        Map<String, Object> zeroData = new HashMap<>();
        zeroData.put("actualQty", 0);
        zeroData.put("description", ""); // Khong co mo ta khi sl = 0
        zeroQtyNoDesc.put("ITEM-001", zeroData);

        return Stream.of(
            new TestCase("ID null", null, validData),
            new TestCase("ID rong", "", validData),
            new TestCase("ID khong ton tai", "SO-NOT-EXIST", validData),
            new TestCase("Slg = 0 nhung khong co mo ta", "SO-DELIVERED", zeroQtyNoDesc)
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidInputsProvider")
    @DisplayName("Kiem thu Hop den: Kiem hang KHONG HOP LE")
    void testInspect_InvalidInputs(TestCase tc) {
        assertThrows(AppException.class, () -> {
            service.submitInspection(tc.siteOrderId, tc.inspectionData);
        });
    }

    static Stream<TestCase> validInputsProvider() {
        Map<String, Map<String, Object>> diffData = new HashMap<>();
        Map<String, Object> diffItem = new HashMap<>();
        diffItem.put("actualQty", 5); // Khac so voi 10
        diffItem.put("description", "Hieu it hang");
        diffItem.put("itemCodeReceived", "ITEM-001");
        diffItem.put("actualUnit", "Cai");
        diffData.put("ITEM-001", diffItem);

        return Stream.of(
            new TestCase("Kiem hang voi sai lech", "SO-DELIVERED", diffData)
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validInputsProvider")
    @DisplayName("Kiem thu Hop den: Kiem hang HOP LE")
    void testInspect_ValidInputs(TestCase tc) {
        assertDoesNotThrow(() -> {
            service.submitInspection(tc.siteOrderId, tc.inspectionData);
        });
        
        // Neu co sai lech, phai tao Discrepancy
        if (tc.name.contains("sai lech")) {
            assertTrue(fakeDiscRepo.findBySiteOrder(tc.siteOrderId).size() > 0);
        }
    }
}
