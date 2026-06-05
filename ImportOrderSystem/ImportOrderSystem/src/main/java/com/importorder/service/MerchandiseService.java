package com.importorder.service;

import com.importorder.model.Merchandise;
import com.importorder.repository.MerchandiseRepository;
import com.importorder.repository.OrderRequestRepository;
import com.importorder.util.AppException;
import com.importorder.util.SessionManager;

import java.time.LocalDateTime;
import java.util.List;

public class MerchandiseService {

    private final MerchandiseRepository merchRepo;
    private final OrderRequestRepository orderRepo;

    // Constructor for Dependency Injection (for testing)
    public MerchandiseService(MerchandiseRepository merchRepo, OrderRequestRepository orderRepo) {
        this.merchRepo = merchRepo;
        this.orderRepo = orderRepo;
    }

    // Default Constructor (for production)
    public MerchandiseService() {
        this.merchRepo = new MerchandiseRepository();
        this.orderRepo = new OrderRequestRepository();
    }

    public Merchandise create(String itemCode, String itemName, String unit,
                               String category, String description) {
        if (itemCode == null || itemCode.isBlank())
            throw new AppException("Mã hàng không được để trống.");
        if (!itemCode.matches("[A-Za-z0-9\\-]+"))
            throw new AppException("Mã hàng chỉ được chứa chữ cái, số và dấu gạch ngang.");
        if (itemName == null || itemName.isBlank())
            throw new AppException("Tên hàng không được để trống.");
        if (unit == null || unit.isBlank())
            throw new AppException("Đơn vị không được để trống.");
        if (merchRepo.existsByCode(itemCode.toUpperCase()))
            throw new AppException("Mã hàng '" + itemCode + "' đã tồn tại trong hệ thống.");

        Merchandise m = new Merchandise();
        m.setItemCode(itemCode.toUpperCase().trim());
        m.setItemName(itemName.trim());
        m.setDefaultUnit(unit.trim());
        m.setCategory(category != null ? category.trim() : "");
        m.setDescription(description != null ? description.trim() : "");
        m.setActive(true);
        m.setCreatedBy(SessionManager.getUsername());
        m.setCreatedAt(LocalDateTime.now());
        m.setUpdatedAt(LocalDateTime.now());

        merchRepo.save(m);
        return m;
    }

    public void update(String itemCode, String itemName, String unit,
                       String category, String description) {
        if (merchRepo.findByCode(itemCode) == null)
            throw new AppException("Không tìm thấy mặt hàng: " + itemCode);
        if (itemName == null || itemName.isBlank())
            throw new AppException("Tên hàng không được để trống.");
        if (unit == null || unit.isBlank())
            throw new AppException("Đơn vị không được để trống.");

        Merchandise m = new Merchandise();
        m.setItemCode(itemCode);
        m.setItemName(itemName.trim());
        m.setDefaultUnit(unit.trim());
        m.setCategory(category != null ? category.trim() : "");
        m.setDescription(description != null ? description.trim() : "");
        m.setUpdatedAt(LocalDateTime.now());

        merchRepo.update(m);
    }

    public void deactivate(String itemCode) {
        if (merchRepo.findByCode(itemCode) == null)
            throw new AppException("Không tìm thấy mặt hàng: " + itemCode);

        // Kiểm tra mặt hàng có đang trong batch PENDING/PROCESSING không
        long activeCount = orderRepo.findAll().stream()
            .filter(r -> "PENDING".equals(r.getStatus()) || "PROCESSING".equals(r.getStatus()))
            .flatMap(r -> r.getItems().stream())
            .filter(i -> itemCode.equals(i.getItemCode()))
            .count();

        if (activeCount > 0) {
            // Vẫn cho phép ẩn nhưng caller phải xác nhận trước (dùng AlertUtils.showConfirm)
            // Service chỉ ghi nhận warning qua exception đặc biệt
            throw new AppException("WARN:Mặt hàng đang có trong " + activeCount
                + " yêu cầu chưa xử lý. Xác nhận vẫn ẩn?");
        }

        merchRepo.setActive(itemCode, false);
    }

    public void deactivateForced(String itemCode) {
        // Gọi khi user đã xác nhận muốn ẩn dù còn batch đang xử lý
        merchRepo.setActive(itemCode, false);
    }

    public void activate(String itemCode) {
        merchRepo.setActive(itemCode, true);
    }

    public List<Merchandise> getAll() {
        return merchRepo.findAll();
    }

    public List<Merchandise> getAllActive() {
        return merchRepo.findAllActive();
    }

    public Merchandise getByCode(String itemCode) {
        return merchRepo.findByCode(itemCode);
    }
}