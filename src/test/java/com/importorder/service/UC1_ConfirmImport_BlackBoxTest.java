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

public class UC1_ConfirmImport_BlackBoxTest {

    private FakeSiteOrderRepository fakeSiteOrderRepo;
    private SiteOrderService siteOrderService;

    @BeforeEach
    void setUp() {
        fakeSiteOrderRepo = new FakeSiteOrderRepository();
        siteOrderService = new SiteOrderService(fakeSiteOrderRepo, null, null, null, null);

        // Gia lap don hang SENT (hop le de xac nhan)
        SiteOrder so1 = new SiteOrder();
        so1.setSiteOrderId("SO-VALID");
        so1.setStatus("SENT");
        so1.setConfirmedBySite(false);
        fakeSiteOrderRepo.save(so1);

        // Gia lap don hang CANCELLED
        SiteOrder so2 = new SiteOrder();
        so2.setSiteOrderId("SO-CANCELLED");
        so2.setStatus("CANCELLED");
        so2.setConfirmedBySite(false);
        fakeSiteOrderRepo.save(so2);

        // Gia lap don hang da CONFIRMED
        SiteOrder so3 = new SiteOrder();
        so3.setSiteOrderId("SO-CONFIRMED");
        so3.setStatus("SENT");
        so3.setConfirmedBySite(true);
        fakeSiteOrderRepo.save(so3);

        // Gia lap don hang RECEIVED (khong the xac nhan nua)
        SiteOrder so4 = new SiteOrder();
        so4.setSiteOrderId("SO-RECEIVED");
        so4.setStatus("RECEIVED");
        so4.setConfirmedBySite(false);
        fakeSiteOrderRepo.save(so4);
    }

    // =========================================================================
    // BLACK-BOX: Equivalence Partitioning cho Site Order ID
    // =========================================================================

    static Stream<String> invalidSiteOrderIdsProvider() {
        return Stream.of(
            null,                // Null input
            "",                  // Empty string
            "   ",               // Whitespace
            "SO-NOT-EXIST",      // ID khong ton tai trong DB
            "SO-CANCELLED",      // ID ton tai nhung o trang thai khong cho phep (CANCELLED)
            "SO-CONFIRMED",      // ID ton tai nhung da duoc xac nhan roi (isConfirmedBySite = true)
            "SO-RECEIVED"        // ID ton tai nhung o trang thai RECEIVED (khong the confirm lai)
        );
    }

    @ParameterizedTest(name = "[{index}] ID Don hang = ''{0}''")
    @MethodSource("invalidSiteOrderIdsProvider")
    @DisplayName("Kiem thu Hop den: Cac kich ban Khong Hop Le")
    void testConfirmBySite_InvalidInputs(String siteOrderId) {
        assertThrows(AppException.class, () -> {
            siteOrderService.confirmBySite(siteOrderId);
        });
    }

    static Stream<String> validSiteOrderIdsProvider() {
        return Stream.of(
            "SO-VALID" // ID ton tai va o trang thai SENT
        );
    }

    @ParameterizedTest(name = "[{index}] ID Don hang = ''{0}''")
    @MethodSource("validSiteOrderIdsProvider")
    @DisplayName("Kiem thu Hop den: Cac kich ban Hop Le")
    void testConfirmBySite_ValidInputs(String siteOrderId) {
        assertDoesNotThrow(() -> {
            siteOrderService.confirmBySite(siteOrderId);
        });

        SiteOrder updatedOrder = fakeSiteOrderRepo.findBySiteOrderId(siteOrderId);
        assertTrue(updatedOrder.isConfirmedBySite());
        assertEquals("CONFIRMED", updatedOrder.getStatus());
    }
}
