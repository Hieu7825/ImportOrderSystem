package com.importorder.model;

import org.bson.types.ObjectId;
import java.time.LocalDateTime;

/**
 * Lời mời liên doanh do OOD gửi cho SITE.
 * status: PENDING → ACCEPTED | REJECTED
 */
public class SiteInvitation {

    private ObjectId id;
    private String inviteId;          // e.g. "INV-20250507-001"
    private String siteCode;          // mã site OOD đề xuất
    private String siteName;          // tên site
    private String country;           // quốc gia
    private String invitedBy;         // username OOD
    private String status;            // PENDING | ACCEPTED | REJECTED
    private String rejectReason;      // lý do từ chối (nếu REJECTED)
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    public SiteInvitation() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getInviteId() { return inviteId; }
    public void setInviteId(String inviteId) { this.inviteId = inviteId; }

    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }

    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getInvitedBy() { return invitedBy; }
    public void setInvitedBy(String invitedBy) { this.invitedBy = invitedBy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
}