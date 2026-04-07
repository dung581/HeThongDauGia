package common;

import java.time.LocalDateTime;

public class Electronics extends Item{
    public Electronics(String IID, String itemName, String description, long startPrice, long currentPrice, LocalDateTime startTime, LocalDateTime endTime, long minIncrement) {
        super(IID, "ELECTRONICS", itemName, description, startPrice, currentPrice, startTime, endTime, minIncrement);
    }

    @Override
    public String getItemType() {
        return "ELECTRONICS";
    }
}
