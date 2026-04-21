package Common.Model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import Exceptions.InvalidBidException;


public abstract class Item extends Entity {
    private static final long serialVersionUID = 1L;

    private final String sellerId;          // UID của Seller sở hữu item
    private String itemName;
    private String description;
    private long startPrice;
    private long currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long minIncrement;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public Item(String IID, String sellerId, String itemName, String description, long startPrice, long currentPrice, LocalDateTime startTime, LocalDateTime endTime, long minIncrement) {
        super(IID);
        this.sellerId = sellerId;
        this.itemName = itemName;
        this.description = description;
        this.startPrice = startPrice;
        // Nếu load item đang bid dở từ DB thì currentPrice != startPrice.
        this.currentPrice = currentPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.minIncrement = minIncrement;
    }

    // ===== Getters =====

    public String getSellerId() {
        return sellerId;
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
        return startTime.format(FORMATTER);
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getFormattedEndTime() {
        return endTime.format(FORMATTER);
    }

    public long getMinIncrement() {
        return minIncrement;
    }

    public boolean timeOut() {
        return LocalDateTime.now().isAfter(endTime);
    }

    // ===== Setters hợp lệ =====

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // ===== Hành vi =====

    /** Gia hạn phiên đấu giá (cho Anti-sniping). */
    public void extendEndTime(long extraSeconds) {
        this.endTime = this.endTime.plusSeconds(extraSeconds);
    }

    /**
     * Cập nhật giá hiện tại. Ném InvalidBidException nếu giá mới
     * thấp hơn currentPrice + minIncrement.
     */
    public void updatePrice(long newPrice) throws InvalidBidException {
        long minAllowed = this.currentPrice + this.minIncrement;
        if (newPrice < minAllowed) {
            throw new InvalidBidException(
                    "Giá đặt (" + newPrice
                            + ") không hợp lệ! Mức giá tối thiểu để đặt lúc này là: "
                            + minAllowed);
        }
        this.currentPrice = newPrice;
    }

    /** Mỗi subclass phải trả về loại cụ thể (ART, VEHICLE, ELECTRONICS). */
    public abstract String getItemType();
}