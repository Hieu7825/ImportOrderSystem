package com.importorder.model;

import java.time.LocalDate;

public class OrderItem {
    private String itemCode;
    private String itemName;       // thêm mới
    private int quantityOrdered;
    private String unit;
    private LocalDate desiredDeliveryDate;

    public OrderItem() {}

    public OrderItem(String itemCode, String itemName, int quantityOrdered,
                     String unit, LocalDate desiredDeliveryDate) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.quantityOrdered = quantityOrdered;
        this.unit = unit;
        this.desiredDeliveryDate = desiredDeliveryDate;
    }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public int getQuantityOrdered() { return quantityOrdered; }
    public void setQuantityOrdered(int quantityOrdered) { this.quantityOrdered = quantityOrdered; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public LocalDate getDesiredDeliveryDate() { return desiredDeliveryDate; }
    public void setDesiredDeliveryDate(LocalDate d) { this.desiredDeliveryDate = d; }
}