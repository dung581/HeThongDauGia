package Common.Model.item;


import Common.Model.Item;

import java.time.LocalDateTime;

public class Art extends Item {
    private String artistName;
    public Art(String IID, String itemName, String description, long startPrice, long currentPrice, LocalDateTime startTime, LocalDateTime endTime, long minIncrement, String artistName) {
        super(IID, "ART", itemName, description, startPrice, currentPrice, startTime, endTime, minIncrement);
        this.artistName = artistName;
    }

    public String getArtistName() {
        return artistName;
    }

    @Override
    public String getItemType() {
        return "ART";
    }
}