package com.importorder.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.importorder.model.OrderItem;
import com.importorder.model.OrderRequest;
import com.importorder.model.Merchandise;
import com.importorder.fakes.FakeOrderRequestRepository;
import com.importorder.fakes.FakeMerchandiseRepository;
import com.importorder.util.AppException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UC4_CreateOrderRequestTest {

    private FakeOrderRequestRepository fakeOrderRepo;
    private FakeMerchandiseRepository fakeMerchRepo;
    private OrderRequestService orderRequestService;

    private List<OrderItem> items;

    @BeforeEach
    void setUp() {
        fakeOrderRepo = new FakeOrderRequestRepository();
        fakeMerchRepo = new FakeMerchandiseRepository();
        
        orderRequestService = new OrderRequestService(fakeOrderRepo, fakeMerchRepo);

        items = new ArrayList<>();
    }

    // =========================================================================
    // UC4-WB-01: Danh sách mặt hàng null
    // =========================================================================
    @Test
    void testCreateRequest_TC01_NullItems() {
        AppException ex = assertThrows(AppException.class,
                () -> orderRequestService.createRequest(null));
        assertEquals("Danh sách mặt hàng không được để trống.", ex.getMessage());
    }

    // =========================================================================
    // UC4-WB-02: Danh sách mặt hàng rỗng
    // =========================================================================
    @Test
    void testCreateRequest_TC02_EmptyItems() {
        AppException ex = assertThrows(AppException.class,
                () -> orderRequestService.createRequest(items));
        assertEquals("Danh sách mặt hàng không được để trống.", ex.getMessage());
    }

    // =========================================================================
    // UC4-WB-03: Mã hàng bị trống
    // =========================================================================
    @Test
    void testCreateRequest_TC03_BlankItemCode() {
        OrderItem item = new OrderItem();
        item.setItemCode("   ");
        item.setQuantityOrdered(10);
        item.setUnit("box");
        item.setDesiredDeliveryDate(LocalDate.now().plusDays(5));
        items.add(item);

        AppException ex = assertThrows(AppException.class,
                () -> orderRequestService.createRequest(items));
        assertEquals("Dòng 1: Mã hàng không được để trống.", ex.getMessage());
    }

    // =========================================================================
    // UC4-WB-04: Số lượng đặt <= 0
    // =========================================================================
    @Test
    void testCreateRequest_TC04_InvalidQty() {
        OrderItem item = new OrderItem();
        item.setItemCode("M-001");
        item.setQuantityOrdered(0);
        item.setUnit("box");
        item.setDesiredDeliveryDate(LocalDate.now().plusDays(5));
        items.add(item);

        AppException ex = assertThrows(AppException.class,
                () -> orderRequestService.createRequest(items));
        assertEquals("Dòng 1: Số lượng phải lớn hơn 0.", ex.getMessage());
    }

    // =========================================================================
    // UC4-WB-05: Đơn vị tính bị trống
    // =========================================================================
    @Test
    void testCreateRequest_TC05_BlankUnit() {
        OrderItem item = new OrderItem();
        item.setItemCode("M-001");
        item.setQuantityOrdered(10);
        item.setUnit("");
        item.setDesiredDeliveryDate(LocalDate.now().plusDays(5));
        items.add(item);

        AppException ex = assertThrows(AppException.class,
                () -> orderRequestService.createRequest(items));
        assertEquals("Dòng 1: Đơn vị không được để trống.", ex.getMessage());
    }

    // =========================================================================
    // UC4-WB-06: Ngày nhận yêu cầu không sau hôm nay
    // =========================================================================
    @Test
    void testCreateRequest_TC06_InvalidDeliveryDate() {
        OrderItem item = new OrderItem();
        item.setItemCode("M-001");
        item.setQuantityOrdered(10);
        item.setUnit("box");
        item.setDesiredDeliveryDate(LocalDate.now()); // hôm nay (không hợp lệ)
        items.add(item);

        AppException ex = assertThrows(AppException.class,
                () -> orderRequestService.createRequest(items));
        assertEquals("Dòng 1: Ngày nhận phải sau hôm nay ít nhất 1 ngày.", ex.getMessage());
    }

    // =========================================================================
    // UC4-WB-07: Trùng lặp mã mặt hàng
    // =========================================================================
    @Test
    void testCreateRequest_TC07_DuplicateItemCode() {
        OrderItem item1 = new OrderItem();
        item1.setItemCode("M-001");
        item1.setQuantityOrdered(10);
        item1.setUnit("box");
        item1.setDesiredDeliveryDate(LocalDate.now().plusDays(5));

        OrderItem item2 = new OrderItem();
        item2.setItemCode("M-001"); // Trùng mã
        item2.setQuantityOrdered(5);
        item2.setUnit("box");
        item2.setDesiredDeliveryDate(LocalDate.now().plusDays(5));

        items.add(item1);
        items.add(item2);

        Merchandise merch = new Merchandise();
        merch.setItemCode("M-001");
        fakeMerchRepo.save(merch);

        AppException ex = assertThrows(AppException.class,
                () -> orderRequestService.createRequest(items));
        assertEquals("Mã hàng M-001 bị trùng trong danh sách.", ex.getMessage());
    }

    // =========================================================================
    // UC4-WB-08: Mã hàng không tồn tại trong hệ thống
    // =========================================================================
    @Test
    void testCreateRequest_TC08_ItemCodeNotFound() {
        OrderItem item = new OrderItem();
        item.setItemCode("M-999");
        item.setQuantityOrdered(10);
        item.setUnit("box");
        item.setDesiredDeliveryDate(LocalDate.now().plusDays(5));
        items.add(item);
        
        // Không save M-999 vào fakeMerchRepo

        AppException ex = assertThrows(AppException.class,
                () -> orderRequestService.createRequest(items));
        assertEquals("Mã hàng M-999 không tồn tại trong hệ thống.", ex.getMessage());
    }

    // =========================================================================
    // UC4-WB-09: Dữ liệu hợp lệ -> Tạo đợt yêu cầu thành công
    // =========================================================================
    @Test
    void testCreateRequest_TC09_Success() {
        OrderItem item = new OrderItem();
        item.setItemCode("M-001");
        item.setQuantityOrdered(10);
        item.setUnit("box");
        item.setDesiredDeliveryDate(LocalDate.now().plusDays(5));
        items.add(item);

        Merchandise merch = new Merchandise();
        merch.setItemCode("M-001");
        fakeMerchRepo.save(merch);

        OrderRequest result = orderRequestService.createRequest(items);

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertEquals(items, result.getItems());
        
        OrderRequest savedReq = fakeOrderRepo.findByBatchId(result.getBatchId());
        assertNotNull(savedReq);
    }
}
