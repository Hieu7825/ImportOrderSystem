package com.importorder.service;

import com.importorder.model.SiteInfo;
import com.importorder.repository.SiteOrderRepository;
import com.importorder.repository.SiteRepository;
import com.importorder.repository.UserRepository;
import com.importorder.util.AppException;
import com.importorder.util.SessionManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SiteService {

    private final SiteRepository      siteRepo      = new SiteRepository();
    private final SiteOrderRepository siteOrderRepo = new SiteOrderRepository();
    private final UserRepository      userRepo      = new UserRepository();

    // ── Chỉ SITE tự cập nhật thông tin của mình ──────────────────────────────

    public void updateInfo(String siteCode, String siteName, String country,
                            String contactEmail, int shipDays, int airDays, String otherInfo) {
        SiteInfo site = siteRepo.findByCode(siteCode);
        if (site == null)
            throw new AppException("Không tìm thấy site: " + siteCode);
        if (shipDays <= 0 || airDays <= 0)
            throw new AppException("Số ngày vận chuyển phải lớn hơn 0.");

        site.setSiteName(siteName != null ? siteName.trim() : "");
        site.setCountry(country != null ? country.trim() : "");
        site.setContactEmail(contactEmail != null ? contactEmail.trim() : "");
        site.setShipDays(shipDays);
        site.setAirDays(airDays);
        site.setOtherInfo(otherInfo != null ? otherInfo.trim() : "");

        siteRepo.updateInfo(site);
    }

    public void updateCatalog(String siteCode, List<String> catalogItems) {
        if (siteRepo.findByCode(siteCode) == null)
            throw new AppException("Không tìm thấy site: " + siteCode);
        List<String> deduped = catalogItems.stream().distinct().toList();
        siteRepo.updateCatalog(siteCode, deduped);
    }

    // ── OOD: Ngừng liên doanh ────────────────────────────────────────────────

    /**
     * Edge Case B5: Site ngừng liên doanh trong khi đang xử lý batch.
     * → Đơn hàng đã sinh từ site đó vẫn giữ nguyên (không xóa).
     * → Site không hiển thị trong kết quả tính phương án mới.
     */
    public void deactivate(String siteCode) {
        SiteInfo site = siteRepo.findByCode(siteCode);
        if (site == null)
            throw new AppException("Không tìm thấy site: " + siteCode);
        if ("INACTIVE".equals(site.getStatus()))
            throw new AppException("Site " + siteCode + " đã ngừng liên doanh.");

        // Kiểm tra còn đơn SENT/PARTIALLY_RECEIVED
        long sentCount = siteOrderRepo.findBySite(siteCode).stream()
            .filter(so -> "SENT".equals(so.getStatus())
                       || "PARTIALLY_RECEIVED".equals(so.getStatus()))
            .count();

        if (sentCount > 0) {
            // Edge Case B5: cảnh báo nhưng vẫn cho ngừng
            // Đơn đã sinh giữ nguyên, site chỉ không được chọn cho đơn mới
            throw new AppException("WARN:Site này còn " + sentCount
                + " đơn hàng chưa nhận. Ngừng liên doanh sẽ không ảnh hưởng " +
                "các đơn đã gửi — chúng vẫn tiếp tục. Xác nhận?");
        }

        siteRepo.deactivate(siteCode, SessionManager.getUsername());

        // Khóa tài khoản SITE user liên kết
        userRepo.findByRole("SITE").stream()
            .filter(u -> siteCode.equals(u.getSiteCode()))
            .forEach(u -> userRepo.setActive(u.getUsername(), false));
    }

    public void deactivateForced(String siteCode) {
        // Gọi khi user đã xác nhận (bỏ qua cảnh báo sentCount)
        siteRepo.deactivate(siteCode, SessionManager.getUsername());
        userRepo.findByRole("SITE").stream()
            .filter(u -> siteCode.equals(u.getSiteCode()))
            .forEach(u -> userRepo.setActive(u.getUsername(), false));
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** OOD xem tất cả site (kể cả INVITED, INACTIVE) — read only */
    public List<SiteInfo> getAllSites() {
        return siteRepo.findAll();
    }

    /** Chỉ site ACTIVE + partnerStatus ACTIVE — dùng để sinh đơn */
    public List<SiteInfo> getActiveSites() {
        return siteRepo.findAllActive();
    }

    /** Site INVITED — Admin dùng khi tạo tài khoản SITE */
    public List<SiteInfo> getInvitedSites() {
        return siteRepo.findAllInvited();
    }

    public SiteInfo getByCode(String siteCode) {
        return siteRepo.findByCode(siteCode);
    }

    public List<SiteInfo> findActiveByItemCodes(List<String> itemCodes) {
        return siteRepo.findActiveByItemCodes(itemCodes);
    }
}