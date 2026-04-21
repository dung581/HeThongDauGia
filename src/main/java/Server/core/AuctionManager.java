package Server.core;

import Common.Enum.AuctionState;
import Server.model.Auction;
import Server.repository.AuctionRepository;
import Server.service.AuctionService;
import java.time.LocalDateTime;
import java.util.concurrent.*;

public class AuctionManager {
    private final AuctionRepository repo = new AuctionRepository();
    private final AuctionService service = new AuctionService();
    private final ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);

    public AuctionManager() {
        startTimer();
    }

    public void startAuction(Auction auction) {
        auction.setState(AuctionState.RUNNING);
        repo.save(auction);
    }

    private void endAuction(Auction auction) {
        synchronized (auction) {
            if (auction.getState() != AuctionState.RUNNING) return;
            auction.setState(AuctionState.FINISHED);
            service.finalizeAuction(auction);
            repo.remove(auction.getAuctionID());
        }
    }

    private void startTimer() {
        timer.scheduleAtFixedRate(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                for (Auction auction : repo.findAll().values()) {
                    if (auction.getState() == AuctionState.RUNNING &&
                            now.isAfter(auction.getEndTime())) {
                        endAuction(auction);
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}