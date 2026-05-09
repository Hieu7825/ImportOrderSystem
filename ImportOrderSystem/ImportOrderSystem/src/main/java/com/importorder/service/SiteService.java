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

    private final SiteRepository siteRepo = new SiteRepository();
    private final SiteOrderRepository siteOrderRepo = new SiteOrderRepository();
    private final UserRepository userRepo = new UserRepository();

    public SiteInfo addSite(String siteCode, String siteName, String country,
                             String contactEmail, int shipDays, int airDays, String otherInfo) {
        if (siteCode == null || siteCode.isBlank())
            throw new AppException("Mã site không được để trống.");
        if (siteName == null || siteName.isBlank())
            throw new AppException("Tên site không được để trống.");
        if (shipDays <= 0 || airDays <= 0)
            throw new AppException("Số ngày vận chuyển phải lớn hơn 0.");
        if (siteRepo.existsByCode(siteCode.toUpperCase()))
            throw new AppException("Mã site '" + siteCode + "' đã tồn tại trong hệ thống.");

        SiteInfo site = new SiteInfo();
        site.setSiteCode(siteCode.toUpperCase().trim());
        site.setSiteName(siteName.trim());
        site.setCountry(country != null ? country.trim() : "");
        site.setContactEmail(contactEmail != null ? contactEmail.trim() : "");
        site.setShipDays(shipDays);
        site.setAirDays(airDays);
        site.setOtherInfo(otherInfo != null ? otherInfo.trim() : "");
        site.setCatalogItems(new ArrayList<>());
        site.setStatus("ACTIVE");
        site.setUpdatedAt(LocalDateTime.now());

        siteRepo.save(site);
        return site;
    }

    public void updateInfo(String siteCode, String siteName, String country,
                            String contactEmail, int shipDays, int airDays, String otherInfo) {
        SiteInfo site = siteRepo.findByCode(siteCode);
        if (site == null) throw new AppException("Không tìm thấy site: " + siteCode);
        if (shipDays <= 0 || airDays <= 0)
            throw new AppException("Số ngày vận chuyển phải lớn hơn 0.");

        site.setSiteName(siteName.trim());
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

        // Dedup tự động
        List<String> deduped = catalogItems.stream().distinct().toList();
        siteRepo.updateCatalog(siteCode, deduped);
    }

    public void deactivate(String siteCode) {
        SiteInfo site = siteRepo.findByCode(siteCode);
        if (site == null) throw new AppException("Không tìm thấy site: " + siteCode);
        if ("INACTIVE".equals(site.getStatus()))
            throw new AppException("Site " + siteCode + " đã ngừng liên doanh.");

        // Kiểm tra còn đơn SENT chưa
        long sentCount = siteOrderRepo.findBySite(siteCode).stream()
            .filter(so -> "SENT".equals(so.getStatus()) || "PARTIALLY_RECEIVED".equals(so.getStatus()))
            .count();

        if (sentCount > 0) {
            throw new AppException("WARN:Site này còn " + sentCount
                + " đơn hàng chưa nhận. Ngừng liên doanh sẽ không ảnh hưởng các đơn đã gửi. Xác nhận?");
        }

        siteRepo.deactivate(siteCode, SessionManager.getUsername());

        // Khóa tài khoản SITE user liên kết
        userRepo.findByRole("SITE").stream()
            .filter(u -> siteCode.equals(u.getSiteCode()))
            .forEach(u -> userRepo.setActive(u.getUsername(), false));
    }

    public void deactivateForced(String siteCode) {
        siteRepo.deactivate(siteCode, SessionManager.getUsername());
        userRepo.findByRole("SITE").stream()
            .filter(u -> siteCode.equals(u.getSiteCode()))
            .forEach(u -> userRepo.setActive(u.getUsername(), false));
    }

    public List<SiteInfo> getAllSites() {
        return siteRepo.findAll();
    }

    public List<SiteInfo> getActiveSites() {
        return siteRepo.findAllActive();
    }

    public SiteInfo getByCode(String siteCode) {
        return siteRepo.findByCode(siteCode);
    }

    public List<SiteInfo> findActiveByItemCodes(List<String> itemCodes) {
        return siteRepo.findActiveByItemCodes(itemCodes);
    }
}