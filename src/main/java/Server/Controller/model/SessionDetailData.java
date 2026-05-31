package Server.Controller.model;

import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Autobid;
import Common.DataBase.entities.Bid;
import Common.DataBase.entities.Item;

import java.util.List;
import java.util.Optional;

/**
 * @param session          Phiên đấu giá đang xem.
 * @param item             Sản phẩm thuộc phiên đấu giá.
 * @param bids             Lịch sử đặt giá của phiên.
 * @param availableBalance Số dư khả dụng của bidder hiện tại; role khác sẽ không dùng giá trị này.
 * @param autobid          Cấu hình auto bid mới nhất của bidder cho item này, nếu có.
 */ /*
 * Gói dữ liệu cho màn chi tiết phiên đấu giá.
 *
 * SessionDetailController load dữ liệu trong background Task để UI không bị đơ.
 * Task trả về object này, sau đó controller lấy từng phần bên trong để render:
 * thông tin phiên, thông tin item, lịch sử bid, số dư và trạng thái auto bid.
 */
public record SessionDetailData(Auction session, Item item, List<Bid> bids, long availableBalance,
                                Optional<Autobid> autobid) {
    public SessionDetailData(
            Auction session,
            Item item,
            List<Bid> bids,
            long availableBalance,
            Optional<Autobid> autobid
    ) {
        this.session = session;
        this.item = item;
        this.bids = bids;
        this.availableBalance = availableBalance;
        this.autobid = autobid == null ? Optional.empty() : autobid;
    }
}
