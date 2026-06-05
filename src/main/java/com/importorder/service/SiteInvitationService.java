package com.importorder.service;

import com.importorder.model.SiteInfo;
import com.importorder.model.SiteInvitation;
import com.importorder.repository.SiteInvitationRepository;
import com.importorder.repository.SiteRepository;
import com.importorder.util.AppException;
import com.importorder.util.DateUtils;
import com.importorder.util.SessionManager;

import java.time.LocalDateTime;
import java.util.List;

public class SiteInvitationService {

    private final SiteInvitationRepository inviteRepo = new SiteInvitationRepository();
    private final SiteRepository           siteRepo   = new SiteRepository();

    // =========================================================
    // OOD: Gửi lời mời liên doanh
    // =========================================================
    public SiteInvitation sendInvite(String siteCode, String siteName, String country) {
        if (siteCode == null || siteCode.isBlank())
            throw new AppException("Mã site không được để trống.");
        if (siteName == null || siteName.isBlank())
            throw new AppException("Tên site không được để trống.");

        siteCode = siteCode.toUpperCase().trim();

        // Kiểm tra site đã tồn tại chưa
        if (siteRepo.existsByCode(siteCode))
            throw new AppException("Mã site '" + siteCode + "' đã tồn tại trong hệ thống.");

        // Kiểm tra đã gửi lời mời chưa
        if (inviteRepo.existsBySiteCode(siteCode))
            throw new AppException("Đã có lời mời liên doanh cho site '" + siteCode + "'.");

        // Tạo SiteInvitation
        SiteInvitation inv = new SiteInvitation();
        inv.setInviteId(DateUtils.generateInviteId());
        inv.setSiteCode(siteCode);
        inv.setSiteName(siteName.trim());
        inv.setCountry(country != null ? country.trim() : "");
        inv.setInvitedBy(SessionManager.getUsername());
        inv.setStatus("PENDING");
        inv.setCreatedAt(LocalDateTime.now());
        inviteRepo.save(inv);

        // Tạo SiteInfo với partnerStatus = INVITED (chưa ACTIVE)
        SiteInfo site = new SiteInfo();
        site.setSiteCode(siteCode);
        site.setSiteName(siteName.trim());
        site.setCountry(country != null ? country.trim() : "");
        site.setContactEmail("");
        site.setShipDays(0);
        site.setAirDays(0);
        site.setStatus("INACTIVE");          // chưa active cho đến khi SITE chấp nhận
        site.setPartnerStatus("INVITED");
        site.setInviteId(inv.getInviteId());
        site.setUpdatedAt(LocalDateTime.now());
        siteRepo.save(site);

        return inv;
    }

    // =========================================================
    // SITE: Lấy lời mời của site mình
    // =========================================================
    public SiteInvitation getMyInvitation() {
        String siteCode = SessionManager.getSiteCode();
        if (siteCode == null) return null;
        SiteInvitation inv = inviteRepo.findBySiteCode(siteCode);
        // Chỉ trả về nếu còn PENDING
        if (inv != null && "PENDING".equals(inv.getStatus())) return inv;
        return null;
    }

    // =========================================================
    // SITE: Chấp nhận lời mời
    //   → Điền đầy đủ shipDays, airDays, email trước khi accept
    // =========================================================
    public void acceptInvite(String inviteId,
                              int shipDays, int airDays,
                              String contactEmail, String otherInfo) {
        SiteInvitation inv = inviteRepo.findByInviteId(inviteId);
        if (inv == null)
            throw new AppException("Không tìm thấy lời mời: " + inviteId);
        if (!"PENDING".equals(inv.getStatus()))
            throw new AppException("Lời mời này đã được xử lý.");
        if (shipDays <= 0 || airDays <= 0)
            throw new AppException("Số ngày vận chuyển phải lớn hơn 0.");

        // Cập nhật invitation → ACCEPTED
        inviteRepo.updateStatus(inviteId, "ACCEPTED", null);

        // Cập nhật SiteInfo: điền thông tin đầy đủ + partnerStatus → ACTIVE
        SiteInfo site = siteRepo.findByCode(inv.getSiteCode());
        if (site == null)
            throw new AppException("Không tìm thấy site: " + inv.getSiteCode());

        site.setShipDays(shipDays);
        site.setAirDays(airDays);
        site.setContactEmail(contactEmail != null ? contactEmail.trim() : "");
        site.setOtherInfo(otherInfo != null ? otherInfo.trim() : "");
        siteRepo.updateInfo(site);
        siteRepo.acceptInvitation(inv.getSiteCode(), inviteId);
    }

    // =========================================================
    // SITE: Từ chối lời mời
    // =========================================================
    public void rejectInvite(String inviteId, String reason) {
        SiteInvitation inv = inviteRepo.findByInviteId(inviteId);
        if (inv == null)
            throw new AppException("Không tìm thấy lời mời: " + inviteId);
        if (!"PENDING".equals(inv.getStatus()))
            throw new AppException("Lời mời này đã được xử lý.");
        if (reason == null || reason.isBlank())
            throw new AppException("Vui lòng nhập lý do từ chối.");

        inviteRepo.updateStatus(inviteId, "REJECTED", reason);
        // Giữ SiteInfo nhưng partnerStatus vẫn INVITED (OOD có thể gửi lại)
    }

    // =========================================================
    // Getters cho UI
    // =========================================================
    public List<SiteInvitation> getAllPending() {
        return inviteRepo.findByStatus("PENDING");
    }

    public List<SiteInvitation> getAll() {
        return inviteRepo.findAll();
    }

    public SiteInvitation getByInviteId(String inviteId) {
        return inviteRepo.findByInviteId(inviteId);
    }
}