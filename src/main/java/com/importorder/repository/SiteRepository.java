package com.importorder.repository;

import com.importorder.config.MongoConfig;
import com.importorder.model.SiteInfo;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SiteRepository {

    private final MongoCollection<Document> collection;

    public SiteRepository() {
        this.collection = MongoConfig.getDatabase().getCollection("sites");
    }

    public void save(SiteInfo site) {
        Document doc = toDocument(site);
        collection.insertOne(doc);
        site.setId(doc.getObjectId("_id"));
    }

    public SiteInfo findByCode(String siteCode) {
        Document doc = collection.find(Filters.eq("siteCode", siteCode)).first();
        return doc != null ? toSiteInfo(doc) : null;
    }

    public List<SiteInfo> findAll() {
        List<SiteInfo> list = new ArrayList<>();
        for (Document doc : collection.find()) list.add(toSiteInfo(doc));
        return list;
    }

    public List<SiteInfo> findAllActive() {
        List<SiteInfo> list = new ArrayList<>();
        for (Document doc : collection.find(
                Filters.and(
                    Filters.eq("status", "ACTIVE"),
                    Filters.eq("partnerStatus", "ACTIVE")
                )))
            list.add(toSiteInfo(doc));
        return list;
    }

    /** Tìm site INVITED (chờ SITE chấp nhận) — dùng khi Admin tạo tài khoản SITE */
    public List<SiteInfo> findAllInvited() {
        List<SiteInfo> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("partnerStatus", "INVITED")))
            list.add(toSiteInfo(doc));
        return list;
    }

    /** Tìm site ACTIVE có catalog chứa ít nhất 1 itemCode — chỉ lấy site đã ACTIVE */
    public List<SiteInfo> findActiveByItemCodes(List<String> itemCodes) {
        List<SiteInfo> list = new ArrayList<>();
        for (Document doc : collection.find(
                Filters.and(
                    Filters.eq("status", "ACTIVE"),
                    Filters.eq("partnerStatus", "ACTIVE"),
                    Filters.in("catalogItems", itemCodes)
                ))) {
            list.add(toSiteInfo(doc));
        }
        return list;
    }

    public void updateInfo(SiteInfo site) {
        collection.updateOne(
            Filters.eq("siteCode", site.getSiteCode()),
            Updates.combine(
                Updates.set("siteName",     site.getSiteName()),
                Updates.set("country",      site.getCountry()),
                Updates.set("contactEmail", site.getContactEmail()),
                Updates.set("shipDays",     site.getShipDays()),
                Updates.set("airDays",      site.getAirDays()),
                Updates.set("otherInfo",    site.getOtherInfo()),
                Updates.set("updatedAt",    LocalDateTime.now().toString())
            )
        );
    }

    public void updateCatalog(String siteCode, List<String> catalogItems) {
        collection.updateOne(
            Filters.eq("siteCode", siteCode),
            Updates.combine(
                Updates.set("catalogItems", catalogItems),
                Updates.set("updatedAt",    LocalDateTime.now().toString())
            )
        );
    }

    /** OOD gửi lời mời → tạo SiteInfo với partnerStatus = INVITED */
    public void updatePartnerStatus(String siteCode, String partnerStatus) {
        collection.updateOne(
            Filters.eq("siteCode", siteCode),
            Updates.combine(
                Updates.set("partnerStatus", partnerStatus),
                Updates.set("updatedAt",     LocalDateTime.now().toString())
            )
        );
    }

    /** SITE chấp nhận lời mời → partnerStatus INVITED → ACTIVE */
    public void acceptInvitation(String siteCode, String inviteId) {
        collection.updateOne(
            Filters.eq("siteCode", siteCode),
            Updates.combine(
                Updates.set("partnerStatus", "ACTIVE"),
                Updates.set("status",        "ACTIVE"),
                Updates.set("inviteId",      inviteId),
                Updates.set("updatedAt",     LocalDateTime.now().toString())
            )
        );
    }

    public void deactivate(String siteCode, String inactivatedBy) {
        collection.updateOne(
            Filters.eq("siteCode", siteCode),
            Updates.combine(
                Updates.set("status",        "INACTIVE"),
                Updates.set("partnerStatus", "INACTIVE"),
                Updates.set("inactivatedAt", LocalDateTime.now().toString()),
                Updates.set("inactivatedBy", inactivatedBy),
                Updates.set("updatedAt",     LocalDateTime.now().toString())
            )
        );
    }

    public boolean existsByCode(String siteCode) {
        return collection.find(Filters.eq("siteCode", siteCode)).first() != null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Document toDocument(SiteInfo s) {
        return new Document()
            .append("siteCode",     s.getSiteCode())
            .append("siteName",     s.getSiteName())
            .append("country",      s.getCountry())
            .append("contactEmail", s.getContactEmail() != null ? s.getContactEmail() : "")
            .append("shipDays",     s.getShipDays())
            .append("airDays",      s.getAirDays())
            .append("catalogItems", s.getCatalogItems() != null
                ? s.getCatalogItems() : new ArrayList<>())
            .append("status",        s.getStatus() != null ? s.getStatus() : "ACTIVE")
            .append("partnerStatus", s.getPartnerStatus() != null
                ? s.getPartnerStatus() : "INVITED")
            .append("inviteId",      s.getInviteId())
            .append("otherInfo",     s.getOtherInfo())
            .append("updatedAt",     LocalDateTime.now().toString());
    }

    private SiteInfo toSiteInfo(Document doc) {
        SiteInfo s = new SiteInfo();
        s.setId(doc.getObjectId("_id"));
        s.setSiteCode(doc.getString("siteCode"));
        s.setSiteName(doc.getString("siteName"));
        s.setCountry(doc.getString("country"));
        s.setContactEmail(doc.getString("contactEmail"));
        s.setShipDays(doc.getInteger("shipDays", 0));
        s.setAirDays(doc.getInteger("airDays", 0));
        s.setCatalogItems(doc.getList("catalogItems", String.class));
        s.setStatus(doc.getString("status"));
        // partnerStatus: fallback sang ACTIVE cho data cũ không có field này
        String ps = doc.getString("partnerStatus");
        s.setPartnerStatus(ps != null ? ps : "ACTIVE");
        s.setInviteId(doc.getString("inviteId"));
        s.setOtherInfo(doc.getString("otherInfo"));
        String inactivatedAt = doc.getString("inactivatedAt");
        if (inactivatedAt != null) s.setInactivatedAt(LocalDateTime.parse(inactivatedAt));
        s.setInactivatedBy(doc.getString("inactivatedBy"));
        String updatedAt = doc.getString("updatedAt");
        if (updatedAt != null) s.setUpdatedAt(LocalDateTime.parse(updatedAt));
        return s;
    }
}