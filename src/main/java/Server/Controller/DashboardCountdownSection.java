package Server.Controller;

import Server.Controller.model.DashboardModels.EndedSessionRow;
import Server.Controller.model.DashboardModels.LiveSessionRow;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Nhóm countdown cho các phiên live trong dashboard Bidder.
final class DashboardCountdownSection {
    private final DashBoardController controller;

    DashboardCountdownSection(DashBoardController controller) {
        this.controller = controller;
    }

    // Khởi động timer cập nhật thời gian còn lại của các phiên đang chạy.
    void startLiveCountdown() {
        stopLiveCountdown();
        controller.lastBidderSilentRefreshNanos = System.nanoTime();
        refreshLiveCountdown();

        controller.liveCountdownTimeline = new Timeline(new KeyFrame(
                javafx.util.Duration.seconds(1),
                event -> refreshLiveCountdown()
        ));
        controller.liveCountdownTimeline.setCycleCount(Timeline.INDEFINITE);
        controller.liveCountdownTimeline.play();
    }

    // Cập nhật countdown từng dòng; phiên hết giờ được chuyển sang bảng đã kết thúc.
    void refreshLiveCountdown() {
        if (controller.liveSessionRows == null) {
            return;
        }

        List<LiveSessionRow> expiredRows = new ArrayList<>();
        for (LiveSessionRow row : controller.liveSessionRows) {
            if (isExpired(row.getEndTime())) {
                expiredRows.add(row);
                continue;
            }
            row.setTimeLeft(controller.formatRemaining(row.getEndTime()));
        }

        for (LiveSessionRow row : expiredRows) {
            controller.liveSessionRows.remove(row);
            addEndedSession(row.toEndedSessionRow());
        }
        if (controller.liveSessionTable != null) {
            controller.liveSessionTable.refresh();
        }
        refreshBidderDashboardAfterAutoBidTick();
        updateSessionSummaries();
    }

    // Dashboard chỉ render dữ liệu có sẵn; refresh ngầm định kỳ để thấy bid mới do runner auto bid tạo ra.
    private void refreshBidderDashboardAfterAutoBidTick() {
        if (controller.liveSessionRows.isEmpty()) {
            return;
        }
        long now = System.nanoTime();
        if (now - controller.lastBidderSilentRefreshNanos < 2_000_000_000L) {
            return;
        }
        controller.lastBidderSilentRefreshNanos = now;
        controller.refreshBidderDashboardSilently();
    }

    // Kiểm tra thời gian kết thúc đã qua hay chưa.
    boolean isExpired(LocalDateTime endTime) {
        return endTime == null || !endTime.isAfter(LocalDateTime.now());
    }

    // Thêm một phiên vào bảng đã kết thúc, tránh trùng và giới hạn số dòng hiển thị.
    void addEndedSession(EndedSessionRow endedRow) {
        boolean exists = controller.endedSessionRows.stream()
                .anyMatch(row -> row.getSessionId() == endedRow.getSessionId());
        if (!exists) {
            controller.endedSessionRows.add(0, endedRow);
        }
        while (controller.endedSessionRows.size() > 8) {
            controller.endedSessionRows.remove(controller.endedSessionRows.size() - 1);
        }
    }

    // Cập nhật các label tổng quan số phiên đang mở và phiên gần đây.
    void updateSessionSummaries() {
        controller.setText(controller.liveSessionSummaryLabel, controller.liveSessionRows.size() + " phien dang mo");
        controller.setText(controller.sidebarLiveLabel, String.valueOf(controller.liveSessionRows.size()));
        controller.setText(controller.endedSessionSummaryLabel, controller.endedSessionRows.size() + " phien gan day");
    }

    // Dừng timer countdown khi rời dashboard hoặc tải lại màn.
    void stopLiveCountdown() {
        if (controller.liveCountdownTimeline != null) {
            controller.liveCountdownTimeline.stop();
            controller.liveCountdownTimeline = null;
        }
    }
}
