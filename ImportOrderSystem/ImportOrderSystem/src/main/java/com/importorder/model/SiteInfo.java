package com.importorder.model;

import org.bson.types.ObjectId;
import java.time.LocalDateTime;
import java.util.List;

public class SiteInfo {

    private ObjectId id;
    private String siteCode;
    private String siteName;
    private String country;
    private String contactEmail;
    private int shipDays;
    private int airDays;
    private List<String> catalogItems;
    private String status;           // ACTIVE | INACTIVE

    // ── Thêm mới ──────────────────────────────────────────────────────────────
    /**
     * Trạng thái liên doanh:
     *   INVITED  — OOD đã gửi lời mời, SITE chưa phản hồi
     *   ACTIVE   — SITE đã chấp nhận, đang liên doanh
     *   INACTIVE — đã ngừng liên doanh
     */
    private String partnerStatus;    // INVITED | ACTIVE | INACTIVE
    private String inviteId;         // id của SiteInvitation tương ứng

    private LocalDateTime inactivatedAt;
    private String inactivatedBy;
    private String otherInfo;
    private LocalDateTime updatedAt;

    public SiteInfo() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }

    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public int getShipDays() { return shipDays; }
    public void setShipDays(int shipDays) { this.shipDays = shipDays; }

    public int getAirDays() { return airDays; }
    public void setAirDays(int airDays) { this.airDays = airDays; }

    public List<String> getCatalogItems() { return catalogItems; }
    public void setCatalogItems(List<String> catalogItems) { this.catalogItems = catalogItems; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPartnerStatus() { return partnerStatus; }
    public void setPartnerStatus(String partnerStatus) { this.partnerStatus = partnerStatus; }

    public String getInviteId() { return inviteId; }
    public void setInviteId(String inviteId) { this.inviteId = inviteId; }

    public LocalDateTime getInactivatedAt() { return inactivatedAt; }
    public void setInactivatedAt(LocalDateTime inactivatedAt) { this.inactivatedAt = inactivatedAt; }

    public String getInactivatedBy() { return inactivatedBy; }
    public void setInactivatedBy(String inactivatedBy) { this.inactivatedBy = inactivatedBy; }

    public String getOtherInfo() { return otherInfo; }
    public void setOtherInfo(String otherInfo) { this.otherInfo = otherInfo; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ── Helper ────────────────────────────────────────────────────────────────

    /** Site chỉ được dùng để sinh đơn khi partnerStatus = ACTIVE */
    public boolean isReadyForOrder() {
        return "ACTIVE".equals(partnerStatus) && "ACTIVE".equals(status);
    }
}