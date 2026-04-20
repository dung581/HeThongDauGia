package Common.Model.item;


import Common.Model.Item;

import java.time.LocalDateTime;

public class Electronics extends Item {
    private int warrantyMonths; // thang bao hanh
    public Electronics(String IID, String itemName, String description, long startPrice, long currentPrice, LocalDateTime startTime, LocalDateTime endTime, long minIncrement, int warrantyMonths) {
        super(IID, "ELECTRONICS", itemName, description, startPrice, currentPrice, startTime, endTime, minIncrement);
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