package Server.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AutoBidBackgroundRunner {
    private static final AtomicBoolean started = new AtomicBoolean(false);
    private static final AtomicBoolean tickRunning = new AtomicBoolean(false);
    private static ScheduledExecutorService executor;

    private AutoBidBackgroundRunner() {
    }

    // Khởi động một runner nền cho toàn app để auto bid không phụ thuộc màn chi tiết phiên.
    public static void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }

        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "auto-bid-background-runner");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(AutoBidBackgroundRunner::runOneTick, 0, 1, TimeUnit.SECONDS);
    }

    // Dừng runner khi app JavaFX đóng hẳn.
    public static void stop() {
        if (!started.compareAndSet(true, false)) {
            return;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        tickRunning.set(false);
    }

    // Mỗi tick chỉ cho một luồng quét DB, tránh chồng query nếu lần trước chạy chậm.
    private static void runOneTick() {
        if (!tickRunning.compareAndSet(false, true)) {
            return;
        }
        try {
            new AutoBidService().resumeAllRunningSessions();
        } catch (RuntimeException ignored) {
            // Runner nền không hiện popup; màn hiện tại sẽ tự refresh và báo lỗi khi user thao tác trực tiếp.
        } finally {
            tickRunning.set(false);
        }
    }
}
