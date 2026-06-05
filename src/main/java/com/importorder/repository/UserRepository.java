package com.importorder.repository;

import com.importorder.config.MongoConfig;
import com.importorder.model.User;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    private final MongoCollection<Document> collection;

    public UserRepository() {
        this.collection = MongoConfig.getDatabase().getCollection("users");
    }

    public void save(User user) {
        Document doc = new Document()
            .append("username", user.getUsername())
            .append("passwordHash", user.getPasswordHash())
            .append("role", user.getRole())
            .append("fullName", user.getFullName())
            .append("siteCode", user.getSiteCode())
            .append("isActive", user.isActive())
            .append("createdAt", user.getCreatedAt().toString())
            .append("updatedAt", user.getUpdatedAt().toString());
        collection.insertOne(doc);
        user.setId(doc.getObjectId("_id"));
    }

    public User findByUsername(String username) {
        Document doc = collection.find(Filters.eq("username", username)).first();
        return doc != null ? toUser(doc) : null;
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        for (Document doc : collection.find()) list.add(toUser(doc));
        return list;
    }

    public List<User> findByRole(String role) {
        List<User> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("role", role)))
            list.add(toUser(doc));
        return list;
    }

    public void updatePassword(String username, String newHash) {
        collection.updateOne(
            Filters.eq("username", username),
            Updates.combine(
                Updates.set("passwordHash", newHash),
                Updates.set("updatedAt", LocalDateTime.now().toString())
            )
        );
    }

    public void updateInfo(String username, String fullName, String role, String siteCode) {
        collection.updateOne(
            Filters.eq("username", username),
            Updates.combine(
                Updates.set("fullName", fullName),
                Updates.set("role", role),
                Updates.set("siteCode", siteCode),
                Updates.set("updatedAt", LocalDateTime.now().toString())
            )
        );
    }

    public void setActive(String username, boolean active) {
        collection.updateOne(
            Filters.eq("username", username),
            Updates.combine(
                Updates.set("isActive", active),
                Updates.set("updatedAt", LocalDateTime.now().toString())
            )
        );
    }

    private User toUser(Document doc) {
        User u = new User();
        u.setId(doc.getObjectId("_id"));
        u.setUsername(doc.getString("username"));
        u.setPasswordHash(doc.getString("passwordHash"));
        u.setRole(doc.getString("role"));
        u.setFullName(doc.getString("fullName"));
        u.setSiteCode(doc.getString("siteCode"));
        u.setActive(Boolean.TRUE.equals(doc.getBoolean("isActive")));
        String createdAt = doc.getString("createdAt");
        if (createdAt != null) u.setCreatedAt(LocalDateTime.parse(createdAt));
        String updatedAt = doc.getString("updatedAt");
        if (updatedAt != null) u.setUpdatedAt(LocalDateTime.parse(updatedAt));
        return u;
    }
}