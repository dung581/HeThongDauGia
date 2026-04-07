package common;

import java.time.LocalDateTime;

public class Art extends Item{
    public Art(String IID, String itemName, String description, long startPrice, long currentPrice, LocalDateTime startTime, LocalDateTime endTime, long minIncrement) {
        super(IID, "ART", itemName, description, startPrice, currentPrice, startTime, endTime, minIncrement);
    }

    @Override
    public String getItemType() {
        return "ART";
    }
}
