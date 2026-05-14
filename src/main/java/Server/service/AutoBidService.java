package Server.service;

import Common.DataBase.entities.Auction;
import Common.DataBase.entities.Autobid;
import Common.DataBase.repository.AutoBidRepository;

import java.time.LocalDateTime;
import java.util.List;

public class AutoBidService {

    private AutoBidRepository repo = new AutoBidRepository();

    public Autobid configure(long userId, long itemId, long maxPrice) {
        Autobid ab = new Autobid();
        ab.setUser_id(userId);
        ab.setItem_id(itemId);
        ab.setMax_price(maxPrice);
        ab.set_active(false);

        repo.saveAutobid(ab);
        return ab;
    }

    public void activate(long id) {
        repo.updateActive(id, true);
    }

    public void deactivate(long id) {
        repo.updateActive(id, false);
    }

    public void trigger(long itemId, long currentPrice) {
        BidService bidService = new BidService();
        List<Autobid> list = repo.getActiveByItemId(itemId);

        for (Autobid ab : list) {

            if (!ab.is_active()) continue;
            if (ab.getMax_price() <= currentPrice) continue;

            long nextPrice = currentPrice + 1;

            if (nextPrice <= ab.getMax_price()) {
                bidService.placeBid(ab.getUser_id(), itemId, nextPrice);
                break;
            }
        }
    }

    public List<Autobid> getByUserId(long userId) {
        return repo.getByUserId(userId);
    }

    public void autoTime(Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = auction.getEndTime();

        // Nếu bid trong vòng 5 phút cuối
        if (now.isAfter(endTime.minusMinutes(5)) && now.isBefore(endTime)) {
            auction.setEndTime(endTime.plusMinutes(5));
            repo.updateEndTime(auction.getId(), auction.getEndTime());
        }
    }
}
