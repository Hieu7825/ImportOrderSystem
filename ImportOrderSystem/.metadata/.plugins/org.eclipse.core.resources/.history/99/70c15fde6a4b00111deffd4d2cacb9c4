package com.importorder.util;

import com.importorder.model.*;
import com.importorder.repository.*;
import com.importorder.service.AuthService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DataSeeder {

    public static void seed() {
        UserRepository userRepo        = new UserRepository();
        MerchandiseRepository merchRepo = new MerchandiseRepository();
        SiteRepository siteRepo        = new SiteRepository();

        System.out.println("[Seeder] Kiểm tra và bổ sung data mẫu còn thiếu...");

        seedUsers(userRepo);
        seedMerchandise(merchRepo);
        seedSites(siteRepo);
        seedSampleStock(new StockRepository(), new OrderRequestRepository());
        fixMissingItemNames(new OrderRequestRepository(), merchRepo);

        System.out.println("[Seeder] Hoàn tất!");
        System.out.println("[Seeder] Tài khoản mặc định - Username: admin / Password: Admin@123");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FIX — điền itemName còn thiếu cho các batch cũ trong MongoDB
    // ─────────────────────────────────────────────────────────────────────────
    private static void fixMissingItemNames(OrderRequestRepository orderRepo,
                                            MerchandiseRepository  merchRepo) {
        // Lấy thẳng collection để patch Document
        MongoCollection<Document> col =
            com.importorder.config.MongoConfig.getDatabase()
                .getCollection("order_requests");

        int fixed = 0;
        for (Document req : col.find()) {
            List<Document> items = req.getList("items", Document.class);
            if (items == null) continue;

            boolean changed = false;
            List<Document> updatedItems = new ArrayList<>();

            for (Document item : items) {
                String itemName = item.getString("itemName");
                if (itemName == null || itemName.isBlank()) {
                    // Tra cứu tên từ merchandise collection
                    String itemCode = item.getString("itemCode");
                    Merchandise merch = merchRepo.findByCode(itemCode);
                    if (merch != null) {
                        item.put("itemName", merch.getItemName());
                        changed = true;
                        System.out.println("[Seeder] Fixed itemName: "
                            + itemCode + " → " + merch.getItemName());
                    }
                }
                updatedItems.add(item);
            }

            if (changed) {
                col.updateOne(
                    Filters.eq("_id", req.getObjectId("_id")),
                    Updates.set("items", updatedItems)
                );
                fixed++;
            }
        }

        if (fixed == 0) {
            System.out.println("[Seeder] SKIP fixItemNames: tất cả batch đã có itemName.");
        } else {
            System.out.println("[Seeder] fixItemNames: đã patch " + fixed + " batch.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // USERS
    // ─────────────────────────────────────────────────────────────────────────
    private static void seedUsers(UserRepository repo) {
        saveUserIfAbsent(repo, "admin",     "Admin@123", "ADMIN", "System Administrator", null,  true);
        saveUserIfAbsent(repo, "sd_nguyen", "Admin@123", "SD",   "Nguyễn Văn An",         null,  true);
        saveUserIfAbsent(repo, "sd_tran",   "Admin@123", "SD",   "Trần Thị Bảo",          null,  true);
        saveUserIfAbsent(repo, "sd_le",     "Admin@123", "SD",   "Lê Minh Châu",          null,  false);
        saveUserIfAbsent(repo, "ood_pham",  "Admin@123", "OOD",  "Phạm Thị Dung",         null,  true);
        saveUserIfAbsent(repo, "ood_hoang", "Admin@123", "OOD",  "Hoàng Văn Em",          null,  true);
        saveUserIfAbsent(repo, "ood_vu",    "Admin@123", "OOD",  "Vũ Thị Phương",         null,  true);
        saveUserIfAbsent(repo, "wm_do",     "Admin@123", "WM",   "Đỗ Văn Giang",          null,  true);
        saveUserIfAbsent(repo, "wm_bui",    "Admin@123", "WM",   "Bùi Thị Hoa",           null,  true);
        saveUserIfAbsent(repo, "site_s01",  "Admin@123", "SITE", "Tanaka Hiroshi",         "S01", true);
        saveUserIfAbsent(repo, "site_s02",  "Admin@123", "SITE", "Kim Ji-won",             "S02", true);
        saveUserIfAbsent(repo, "site_s03",  "Admin@123", "SITE", "Li Wei",                 "S03", true);
        saveUserIfAbsent(repo, "site_s04",  "Admin@123", "SITE", "Raj Patel",              "S04", true);
        saveUserIfAbsent(repo, "site_s05",  "Admin@123", "SITE", "Hans Mueller",           "S05", true);
        saveUserIfAbsent(repo, "site_s06",  "Admin@123", "SITE", "Maria Santos",           "S06", false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MERCHANDISE
    // ─────────────────────────────────────────────────────────────────────────
    private static void seedMerchandise(MerchandiseRepository repo) {
        saveMerchIfAbsent(repo, "MH001", "Điện thoại Samsung Galaxy S25 Ultra",        "chiếc", "Điện tử");
        saveMerchIfAbsent(repo, "MH002", "Điện thoại iPhone 16 Pro Max",               "chiếc", "Điện tử");
        saveMerchIfAbsent(repo, "MH003", "Laptop Dell XPS 15 2024",                    "chiếc", "Điện tử");
        saveMerchIfAbsent(repo, "MH004", "Laptop Apple MacBook Pro M4",                "chiếc", "Điện tử");
        saveMerchIfAbsent(repo, "MH005", "Màn hình LG UltraWide 34\"",                 "chiếc", "Điện tử");
        saveMerchIfAbsent(repo, "MH006", "Máy tính bảng iPad Pro 13\" M4",             "chiếc", "Điện tử");
        saveMerchIfAbsent(repo, "MH007", "Tai nghe Sony WH-1000XM5",                   "chiếc", "Phụ kiện");
        saveMerchIfAbsent(repo, "MH008", "Tai nghe Apple AirPods Pro 3",               "chiếc", "Phụ kiện");
        saveMerchIfAbsent(repo, "MH009", "Đồng hồ Apple Watch Series 10",              "chiếc", "Phụ kiện");
        saveMerchIfAbsent(repo, "MH010", "Đồng hồ Samsung Galaxy Watch 7",             "chiếc", "Phụ kiện");
        saveMerchIfAbsent(repo, "MH011", "Chuột Logitech MX Master 3S",                "chiếc", "Phụ kiện");
        saveMerchIfAbsent(repo, "MH012", "Bàn phím Keychron Q1 Pro",                   "chiếc", "Phụ kiện");
        saveMerchIfAbsent(repo, "MH013", "Máy ảnh Canon EOS R6 Mark II",               "chiếc", "Máy ảnh");
        saveMerchIfAbsent(repo, "MH014", "Máy ảnh Sony Alpha A7 IV",                   "chiếc", "Máy ảnh");
        saveMerchIfAbsent(repo, "MH015", "Ống kính Sony FE 24-70mm f/2.8 GM II",      "chiếc", "Máy ảnh");
        saveMerchIfAbsent(repo, "MH016", "Máy quay GoPro Hero 13 Black",               "chiếc", "Máy ảnh");
        saveMerchIfAbsent(repo, "MH017", "Robot hút bụi Roborock S8 Pro Ultra",        "chiếc", "Gia dụng thông minh");
        saveMerchIfAbsent(repo, "MH018", "Nồi chiên không khí Philips XXL 7.2L",       "chiếc", "Gia dụng thông minh");
        saveMerchIfAbsent(repo, "MH019", "Máy lọc không khí Dyson Purifier Hot+Cool",  "chiếc", "Gia dụng thông minh");
        saveMerchIfAbsent(repo, "MH020", "Loa thông minh Amazon Echo Show 15",         "chiếc", "Gia dụng thông minh");
        saveMerchIfAbsent(repo, "MH021", "Bộ phát WiFi 6E ASUS ROG Rapture",           "chiếc", "Thiết bị mạng");
        saveMerchIfAbsent(repo, "MH022", "Switch mạng TP-Link TL-SG1024D",             "chiếc", "Thiết bị mạng");
        saveMerchIfAbsent(repo, "MH023", "Ổ cứng SSD Samsung 990 Pro 2TB",             "chiếc", "Linh kiện");
        saveMerchIfAbsent(repo, "MH024", "RAM Corsair Vengeance DDR5 32GB",             "bộ",    "Linh kiện");
        saveMerchIfAbsent(repo, "MH025", "Card đồ họa NVIDIA RTX 5080",                "chiếc", "Linh kiện");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SITES
    // ─────────────────────────────────────────────────────────────────────────
    private static void seedSites(SiteRepository repo) {
        saveSiteIfAbsent(repo, "S01", "Tokyo Import Co.", "Japan",
            "contact@tokyoimport.jp", 14, 3,
            List.of("MH001", "MH003", "MH007", "MH009", "MH013", "MH014", "MH015"));
        saveSiteIfAbsent(repo, "S02", "Seoul Trading Co.", "South Korea",
            "contact@seoultrade.kr", 10, 2,
            List.of("MH001", "MH005", "MH010", "MH017", "MH021", "MH023"));
        saveSiteIfAbsent(repo, "S03", "Shanghai Goods Ltd.", "China",
            "contact@shanghaig.cn", 7, 1,
            List.of("MH003", "MH011", "MH012", "MH018", "MH022", "MH024", "MH025"));
        saveSiteIfAbsent(repo, "S04", "Mumbai Tech Supplies", "India",
            "procurement@mumbaitech.in", 12, 4,
            List.of("MH019", "MH020", "MH021", "MH022", "MH023", "MH024"));
        saveSiteIfAbsent(repo, "S05", "Berlin Quality Imports GmbH", "Germany",
            "orders@berlinqi.de", 21, 5,
            List.of("MH002", "MH004", "MH006", "MH008", "MH013", "MH016", "MH019"));
        saveSiteIfAbsent(repo, "S06", "São Paulo Electronics Ltda.", "Brazil",
            "import@spe.com.br", 30, 7,
            List.of("MH002", "MH004", "MH006", "MH008", "MH009", "MH016", "MH020"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SAMPLE STOCK
    // ─────────────────────────────────────────────────────────────────────────
    private static void seedSampleStock(StockRepository stockRepo,
                                        OrderRequestRepository orderRepo) {
        var processingBatches = orderRepo.findByStatus("PROCESSING");
        if (processingBatches.isEmpty()) {
            System.out.println("[Seeder] SKIP stock: không có batch PROCESSING nào.");
            return;
        }

        SiteRepository siteRepo = new SiteRepository();

        for (var batch : processingBatches) {
            if (batch.getItems() == null) continue;
            for (var item : batch.getItems()) {
                String itemCode = item.getItemCode();
                var sites = siteRepo.findActiveByItemCodes(List.of(itemCode));
                for (var site : sites) {
                    var existing = stockRepo.findByBatchSiteItem(
                        batch.getBatchId(), site.getSiteCode(), itemCode);
                    if (existing != null) {
                        System.out.println("[Seeder] SKIP stock (đã có): "
                            + batch.getBatchId() + "/" + site.getSiteCode() + "/" + itemCode);
                        continue;
                    }
                    int qty = 50 + (int)(Math.random() * 451);
                    StockInfo s = new StockInfo();
                    s.setBatchId(batch.getBatchId());
                    s.setSiteCode(site.getSiteCode());
                    s.setItemCode(itemCode);
                    s.setInStockQty(qty);
                    s.setUnit(item.getUnit());
                    s.setUpdatedBy("seeder");
                    stockRepo.saveOrUpdate(s);
                    System.out.println("[Seeder] Created stock: "
                        + batch.getBatchId() + " / " + site.getSiteCode()
                        + " / " + itemCode + " = " + qty);
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────
    private static void saveUserIfAbsent(UserRepository repo, String username,
            String password, String role, String fullName, String siteCode, boolean active) {
        if (repo.findByUsername(username) != null) {
            System.out.println("[Seeder] SKIP user (đã có): " + username);
            return;
        }
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(AuthService.hashPassword(password));
        u.setRole(role);
        u.setFullName(fullName);
        u.setSiteCode(siteCode);
        u.setActive(active);
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        repo.save(u);
        System.out.println("[Seeder] Created user: " + username
            + " (" + role + ")" + (active ? "" : " [LOCKED]"));
    }

    private static void saveMerchIfAbsent(MerchandiseRepository repo, String code,
            String name, String unit, String category) {
        if (repo.findByCode(code) != null) {
            System.out.println("[Seeder] SKIP merch (đã có): " + code);
            return;
        }
        Merchandise m = new Merchandise();
        m.setItemCode(code);
        m.setItemName(name);
        m.setDefaultUnit(unit);
        m.setCategory(category);
        m.setActive(true);
        m.setCreatedBy("admin");
        m.setCreatedAt(LocalDateTime.now());
        m.setUpdatedAt(LocalDateTime.now());
        repo.save(m);
        System.out.println("[Seeder] Created merch: " + code + " - " + name);
    }

    private static void saveSiteIfAbsent(SiteRepository repo, String code, String name,
            String country, String email, int ship, int air, List<String> catalog) {
        if (repo.findByCode(code) != null) {
            System.out.println("[Seeder] SKIP site (đã có): " + code);
            return;
        }
        SiteInfo s = new SiteInfo();
        s.setSiteCode(code);
        s.setSiteName(name);
        s.setCountry(country);
        s.setContactEmail(email);
        s.setShipDays(ship);
        s.setAirDays(air);
        s.setCatalogItems(catalog);
        s.setStatus("ACTIVE");
        s.setUpdatedAt(LocalDateTime.now());
        repo.save(s);
        System.out.println("[Seeder] Created site: " + code
            + " - " + name + " (" + country + ")");
    }
}