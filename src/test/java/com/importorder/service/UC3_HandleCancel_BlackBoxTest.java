package com.importorder.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.importorder.fakes.FakeSiteOrderRepository;
import com.importorder.model.SiteOrder;
import com.importorder.util.AppException;

public class UC3_HandleCancel_BlackBoxTest {

    private SiteOrderService service;
    private FakeSiteOrderRepository fakeSiteOrderRepo;

    @BeforeEach
    void setUp() {
        fakeSiteOrderRepo = new FakeSiteOrderRepository();
        // C?n mock cac repo khac, nhung o day chu yeu test cac loi validate nen ta cho null tam 
        service = new SiteOrderService(fakeSiteOrderRepo, null, null, null, null);

        SiteOrder req = new SiteOrder();
        req.setSiteOrderId("SO-REQ-CANCEL");
        req.setStatus("CANCEL_REQUESTED");
        fakeSiteOrderRepo.save(req);

        SiteOrder normal = new SiteOrder();
        normal.setSiteOrderId("SO-NORMAL");
        normal.setStatus("SENT");
        fakeSiteOrderRepo.save(normal);
    }

    static class TestCase {
        String name;
        String siteOrderId;
        boolean isApproved;

        public TestCase(String name, String siteOrderId, boolean isApproved) {
            this.name = name;
            this.siteOrderId = siteOrderId;
            this.isApproved = isApproved;
        }
        @Override
        public String toString() { return name; }
    }

    static Stream<TestCase> invalidInputsProvider() {
        return Stream.of(
            new TestCase("ID null", null, true),
            new TestCase("ID rong", "", false),
            new TestCase("ID khong ton tai", "SO-NOT-EXIST", true),
            new TestCase("Don hang khong co yeu cau huy", "SO-NORMAL", true)
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidInputsProvider")
    @DisplayName("Kiem thu Hop den: Xu ly huy don KHONG HOP LE")
    void testApproveCancel_InvalidInputs(TestCase tc) {
        assertThrows(AppException.class, () -> {
            service.approveCancelRequest(tc.siteOrderId);
        });
    }
}
