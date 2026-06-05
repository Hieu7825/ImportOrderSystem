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
import com.importorder.util.AppException;

public class UC4_CreateRequest_BlackBoxTest {

    private OrderRequestService service;
    private FakeMerchandiseRepository fakeMerchRepo;
    private FakeOrderRequestRepository fakeOrderRepo;

    @BeforeEach
    void setUp() {
        fakeMerchRepo = new FakeMerchandiseRepository();
        fakeOrderRepo = new FakeOrderRequestRepository();

        // Gia lap hang hoa hop le
        Merchandise m1 = new Merchandise();
        m1.setItemCode("ITEM-001");
        m1.setItemName("Item 1");
        fakeMerchRepo.save(m1);

        Merchandise m2 = new Merchandise();
        m2.setItemCode("ITEM-002");
        m2.setItemName("Item 2");
        fakeMerchRepo.save(m2);

        service = new OrderRequestService(fakeOrderRepo, fakeMerchRepo);
    }

    // =========================================================================
    // BLACK-BOX: Equivalence Partitioning & Boundary Value Analysis cho Create Request
    // =========================================================================

    static Stream<TestCase> invalidItemsProvider() {
        return Stream.of(
                // 1. Danh sach items rong va null
                new TestCase("Danh sach rong", new ArrayList<>(), "khong duoc de trong"),

                // 2. Ma hang khong hop le (Empty, Null, Khong ton tai)
                new TestCase("Ma hang rong", List.of(createItem("", 10, "Cai", 5)), "Ma hang khong duoc de trong"),
                new TestCase("Ma hang null", List.of(createItem(null, 10, "Cai", 5)), "Ma hang khong duoc de trong"),
                new TestCase("Ma hang khong co trong he thong", List.of(createItem("INVALID-CODE", 10, "Cai", 5)), "khong ton tai"),

                // 3. So luong tai bien (Boundary Value) va phan hoach khong hop le (<= 0)
                new TestCase("So luong am", List.of(createItem("ITEM-001", -5, "Cai", 5)), "So luong phai lon hon 0"),
                new TestCase("So luong bang 0 (Bien)", List.of(createItem("ITEM-001", 0, "Cai", 5)), "So luong phai lon hon 0"),

                // 4. Don vi khong hop le (Empty, Null)
                new TestCase("Don vi rong", List.of(createItem("ITEM-001", 10, "", 5)), "Don vi khong duoc de trong"),
                new TestCase("Don vi null", List.of(createItem("ITEM-001", 10, null, 5)), "Don vi khong duoc de trong"),

                // 5. Ngay nhan (DesiredDeliveryDate) tai bien
                new TestCase("Ngay nhan la hom qua", List.of(createItem("ITEM-001", 10, "Cai", -1)), "Ngay nhan phai sau hom nay"),
                new TestCase("Ngay nhan la hom nay (Bien)", List.of(createItem("ITEM-001", 10, "Cai", 0)), "Ngay nhan phai sau hom nay"),

                // 6. Trung ma hang trong cung danh sach
                new TestCase("Trung ma hang", List.of(
                        createItem("ITEM-001", 10, "Cai", 5),
                        createItem("ITEM-001", 20, "Cai", 6)
                ), "bi trung trong danh sach")
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidItemsProvider")
    @DisplayName("Kim th- hTp `en: cAc k<ch bn to Request KHdA?NG H~P L")
    void testCreateRequest_InvalidInputs(TestCase tc) {
        assertThrows(AppException.class, () -> {
            service.createRequest(tc.items);
        });
    }

    static Stream<TestCase> validItemsProvider() {
        return Stream.of(
                // PhAn hoch hp l: danh sAch chu n, nhi?u items khAc nhau, s` lng Y biAn (1)
                new TestCase("1 item hp l, quantity = 1 (BiAn)", List.of(createItem("ITEM-001", 1, "Cai", 1)), null),
                new TestCase("1 item hp l, quantity l>n", List.of(createItem("ITEM-001", 9999, "Cai", 30)), null),
                new TestCase("Nhi?u items hp l", List.of(
                        createItem("ITEM-001", 100, "Cai", 5),
                        createItem("ITEM-002", 50, "BT", 10)
                ), null)
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validItemsProvider")
    @DisplayName("Kim th- hTp `en: cAc k<ch bn to Request H~P L")
    void testCreateRequest_ValidInputs(TestCase tc) {
        assertDoesNotThrow(() -> {
            service.createRequest(tc.items);
        });
    }

    // Helper methods
    private static OrderItem createItem(String code, int quantity, String unit, int daysAhead) {
        OrderItem item = new OrderItem();
        item.setItemCode(code);
        item.setQuantityOrdered(quantity);
        item.setUnit(unit);
        if (daysAhead != -999) {
            item.setDesiredDeliveryDate(LocalDate.now().plusDays(daysAhead));
        } else {
            item.setDesiredDeliveryDate(null);
        }
        return item;
    }

    // Helper record
    static class TestCase {
        String name;
        List<OrderItem> items;
        String expectedError;

        public TestCase(String name, List<OrderItem> items, String expectedError) {
            this.name = name;
            this.items = items;
            this.expectedError = expectedError;
        }
        @Override
        public String toString() { return name; }
    }
}
