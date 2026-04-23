package Common.Model.transaction;

import Common.Model.item.Item;
import Common.Model.user.User;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Giao dịch đặt giá - mỗi lần Bidder đặt giá thành công sẽ tạo 1 BidTransaction.
 *
 * FIX so với bản cũ:
 *   Constructor cũ thiếu tham số transactionID và item, dẫn đến gán null vào null.
 *   Phiên bản mới: tự sinh transactionID bằng UUID, nhận đủ bidder + item + amount + timestamp.
 */
public class BidTransaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String transactionID;
    private final String bidderUID;     // chỉ lưu UID để nhẹ khi serialize qua Socket
    private final String itemIID;       // tương tự
    private final long bidAmount;
    private final LocalDateTime timestamp;

    public BidTransaction(User bidder, Item item, long bidAmount, LocalDateTime timestamp) {
        this.transactionID = UUID.randomUUID().toString();
        this.bidderUID = bidder.getUID();
        this.itemIID = item.getIID();
        this.bidAmount = bidAmount;
        this.timestamp = timestamp;
    }

    public String getTransactionID() {
        return transactionID;
    }

    public String getBidderUID() {
        return bidderUID;
    }

    public String getItemIID() {
        return itemIID;
    }

    public long getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}