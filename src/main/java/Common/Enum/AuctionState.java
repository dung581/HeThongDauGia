package Common.Enum;

/**
 * State machine của phiên đấu giá:
 *   OPEN      - vừa tạo, chưa bắt đầu
 *   RUNNING   - đang diễn ra, nhận bid
 *   FINISHED  - đã hết thời gian, đợi xử lý payment
 *   PAID      - đã trừ tiền người thắng
 *   CANCELED  - kết thúc nhưng không có người bid / bị hủy
 */
public enum AuctionState {
    OPEN,
    RUNNING,
    FINISHED,
    PAID,
    CANCELED
}