package Common.Model.item;

import Common.Model.Item;

import java.time.LocalDateTime;

public class Electronics extends Item {
    private static final long serialVersionUID = 1L;

    private final int warrantyMonths; // số tháng bảo hành

    public Electronics(String IID, String sellerId, String itemName, String description, long startPrice, long currentPrice, LocalDateTime startTime, LocalDateTime endTime, long minIncrement, int warrantyMonths) {
        super(IID, sellerId, itemName, description, startPrice, currentPrice, startTime, endTime, minIncrement);
        this.warrantyMonths = warrantyMonths;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    @Override
    public String getItemType() {
        return "ELECTRONICS";
    }
}