package com.importorder.repository;

import com.importorder.config.MongoConfig;
import com.importorder.model.SiteInvitation;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SiteInvitationRepository {

    private final MongoCollection<Document> collection;

    public SiteInvitationRepository() {
        this.collection = MongoConfig.getDatabase().getCollection("site_invitations");
    }

    public void save(SiteInvitation inv) {
        Document doc = toDocument(inv);
        collection.insertOne(doc);
        inv.setId(doc.getObjectId("_id"));
    }

    public SiteInvitation findByInviteId(String inviteId) {
        Document doc = collection.find(Filters.eq("inviteId", inviteId)).first();
        return doc != null ? toInvitation(doc) : null;
    }

    public SiteInvitation findBySiteCode(String siteCode) {
        Document doc = collection.find(Filters.eq("siteCode", siteCode)).first();
        return doc != null ? toInvitation(doc) : null;
    }

    public List<SiteInvitation> findByStatus(String status) {
        List<SiteInvitation> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("status", status)))
            list.add(toInvitation(doc));
        return list;
    }

    public List<SiteInvitation> findAll() {
        List<SiteInvitation> list = new ArrayList<>();
        for (Document doc : collection.find()) list.add(toInvitation(doc));
        return list;
    }

    public void updateStatus(String inviteId, String status, String rejectReason) {
        collection.updateOne(
            Filters.eq("inviteId", inviteId),
            Updates.combine(
                Updates.set("status",        status),
                Updates.set("rejectReason",  rejectReason),
                Updates.set("respondedAt",   LocalDateTime.now().toString())
            )
        );
    }

    public boolean existsBySiteCode(String siteCode) {
        return collection.find(Filters.eq("siteCode", siteCode)).first() != null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Document toDocument(SiteInvitation inv) {
        return new Document()
            .append("inviteId",    inv.getInviteId())
            .append("siteCode",    inv.getSiteCode())
            .append("siteName",    inv.getSiteName())
            .append("country",     inv.getCountry())
            .append("invitedBy",   inv.getInvitedBy())
            .append("status",      inv.getStatus())
            .append("rejectReason",inv.getRejectReason())
            .append("createdAt",   inv.getCreatedAt() != null
                ? inv.getCreatedAt().toString() : null)
            .append("respondedAt", inv.getRespondedAt() != null
                ? inv.getRespondedAt().toString() : null);
    }

    private SiteInvitation toInvitation(Document doc) {
        SiteInvitation inv = new SiteInvitation();
        inv.setId(doc.getObjectId("_id"));
        inv.setInviteId(doc.getString("inviteId"));
        inv.setSiteCode(doc.getString("siteCode"));
        inv.setSiteName(doc.getString("siteName"));
        inv.setCountry(doc.getString("country"));
        inv.setInvitedBy(doc.getString("invitedBy"));
        inv.setStatus(doc.getString("status"));
        inv.setRejectReason(doc.getString("rejectReason"));

        String createdAt = doc.getString("createdAt");
        if (createdAt != null) inv.setCreatedAt(LocalDateTime.parse(createdAt));
        String respondedAt = doc.getString("respondedAt");
        if (respondedAt != null) inv.setRespondedAt(LocalDateTime.parse(respondedAt));

        return inv;
    }
}