package com.importorder.util;

public class AppException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AppException(String message) {
        super(message);
    }

    public static class InsufficientStockException extends AppException {
        private static final long serialVersionUID = 2L;
        private final String itemCode;
        private final int needed;
        private final int available;

        public InsufficientStockException(String itemCode, int needed, int available) {
            super(String.format(
                "Mặt hàng %s: cần %d, chỉ có %d trong kho", itemCode, needed, available));
            this.itemCode = itemCode;
            this.needed = needed;
            this.available = available;
        }

        public String getItemCode() { return itemCode; }
        public int getNeeded() { return needed; }
        public int getAvailable() { return available; }
    }

    public static class DeadlineUnreachableException extends AppException {
        private static final long serialVersionUID = 3L;
        private final String itemCode;
        private final String earliestDate;

        public DeadlineUnreachableException(String itemCode, String desiredDate, String earliestDate) {
            super(String.format(
                "Mặt hàng %s: có hàng nhưng giao sớm nhất %s, yêu cầu trước %s",
                itemCode, earliestDate, desiredDate));
            this.itemCode = itemCode;
            this.earliestDate = earliestDate;
        }

        public String getItemCode() { return itemCode; }
        public String getEarliestDate() { return earliestDate; }
    }

    public static class OrderNotCancellableException extends AppException {
        private static final long serialVersionUID = 4L;

        public OrderNotCancellableException(String batchId, String status) {
            super(String.format(
                "Batch %s đang ở trạng thái %s, không thể hủy", batchId, status));
        }
    }

    public static class InvalidOrderStateException extends AppException {
        private static final long serialVersionUID = 5L;

        public InvalidOrderStateException(String message) {
            super(message);
        }
    }
}