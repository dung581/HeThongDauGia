package Common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import Exceptions.InvalidBidException;

public abstract class Item extends Entity{
    private String IID;
    private String type;
    private String itemName;
    private String description;
    private long startPrice;
    private long currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long minIncrement;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public Item(String IID, String type, String itemName, String description, long startPrice, long currentPrice, LocalDateTime startTime, LocalDateTime endTime, long minIncrement) {
        super(IID);
        this.type = type;
        this.itemName = itemName;
        this.description = description;
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.minIncrement = minIncrement;
    }

    public String getIID() {
        return IID;
    }

    public String getType() {
        return type;
    }

    public String getItemName() {
        return itemName;
    }

    public String getDescription() {
        return description;
    }

    public long getStartPrice() {
        return startPrice;
    }

    public long getCurrentPrice() {
        return currentPrice;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public String getFormattedStartTime() {
        return startTime.format(formatter);
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getFormattedEndTime() {
        return endTime.format(formatter);
    }

    public boolean timeOut() {
        return LocalDateTime.now().isAfter(endTime);
    }

    public long getMinIncrement() {
        return minIncrement;
    }
    public void setItemName(String itemName){
        this.itemName = itemName;
    }
    public void setDescription(String description){
        this.description = description;
    }

    // them thoi gian khi co gia moi vao y giay cuoi
    public void extendEndTime(long extraSeconds) {
        this.endTime = this.endTime.plusSeconds(extraSeconds);
    }
    // Thêm throws InvalidBidException vào khai báo hàm
    public void updatePrice(long newPrice) throws InvalidBidException {
        long minAllowed = this.currentPrice + this.minIncrement;
        if (newPrice < minAllowed) {
            // Ném ra lỗi cụ thể kèm thông báo chi tiết
            throw new InvalidBidException("Giá đặt (" + newPrice + ") không hợp lệ! Mức giá tối thiểu để đặt lúc này là: " + minAllowed);
        }
        this.currentPrice = newPrice;
    }

    public abstract String getItemType();

}